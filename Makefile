# =========================================================================
# COMPILADORES Y BANDERAS
# =========================================================================
ifeq ($(shell uname -s),Darwin)
    # MacOS (cross-compiler de Homebrew)
    CC = i686-elf-gcc
    LD = i686-elf-ld
else ifeq ($(shell uname -s),Linux)
    # Linux o WSL
    CC = gcc
    LD = ld
else 
    # Windows
    CC = gcc-12
    LD = ld
endif

AS      = nasm
JAVAC   = javac

CFLAGS  = -m32 -ffreestanding -fno-pic -fno-stack-protector -fno-builtin -nostdlib -O2 -Wall -Wextra -Iinclude
ASFLAGS = -f elf32
LDFLAGS = -m elf_i386 -T linker.ld

# Archivos de salida
KERNEL_BIN = kernel.bin
OS_ISO     = os.iso

# Captura de fuentes C y ASM
C_SOURCES   := $(wildcard *.c) $(wildcard */*.c)
ASM_SOURCES := $(wildcard *.asm) $(wildcard */*.asm) $(wildcard */*/*.asm)
ASM_SOURCES := $(filter-out boot/boot_class.asm, $(ASM_SOURCES))

C_OBJS   := $(patsubst %.c,%.o,$(C_SOURCES))
ASM_OBJS := $(patsubst %.asm,%.o,$(ASM_SOURCES))

BOOT_OBJ  := boot/multiboot.o
REST_OBJS := $(filter-out $(BOOT_OBJ), $(ASM_OBJS) $(C_OBJS))
ALL_OBJS  := $(BOOT_OBJ) $(REST_OBJS)

# =========================================================================
# DEFINICIÓN DE MACROS Y BÚSQUEDA DE CLASES JAVA
# =========================================================================

# 1. Definición indispensable de la función recursiva 'rwildcard'
rwildcard=$(wildcard $1$2) $(foreach d,$(wildcard $1*),$(call rwildcard,$d/,$2))

# 2. Búsqueda automática de todos los .java usando la macro
JAVA_SOURCES := $(call rwildcard,kernel/,*.java) \
                $(call rwildcard,java/,*.java) \
                $(call rwildcard,boot/,*.java)

# 3. Macro para inyectar cada clase en grub.cfg
#define ADD_MODULE
#	@echo    module /classes/$(notdir $(1:.java=.class)) $(1:.java=) >> isodir/boot/grub/grub.cfg
#endef                

define ADD_MODULE
	@echo    module /classes/$(notdir $(1:.java=.class)) $(subst \,/,$(1:.java=)) >> isodir/boot/grub/grub.cfg

endef

# =========================================================================
# COMANDOS SEGUROS DE COPIA DE CLASES
# =========================================================================
ifeq ($(OS),Windows_NT)
    CLEAN_CMD = if exist isodir rmdir /s /q isodir & del /s /q *.bin *.iso *.class *.o
    MKDIR     = if not exist isodir\boot\grub mkdir isodir\boot\grub & if not exist isodir\classes mkdir isodir\classes
    COPY_KERN = copy kernel.bin isodir\boot\kernel.bin >nul
    
    # Windows: Ahora copia RECURSIVAMENTE todos los .class que encuentre en el proyecto, 
    # sin importar en qué subcarpeta profunda (como regex) estén.
    COPY_CLS  = for /R . %%f in (*.class) do copy "%%f" isodir\classes\ >nul
else
    CLEAN_CMD = rm -rf isodir *.bin *.iso $(ALL_OBJS) $$(find . -name "*.class")
    MKDIR     = mkdir -p isodir/boot/grub isodir/classes
    COPY_KERN = cp $(KERNEL_BIN) isodir/boot/kernel.bin
    # POSIX: Encuentra y copia cualquier .class
    COPY_CLS  = find kernel java -name "*.class" -exec cp {} isodir/classes/ \;
endif

# =========================================================================
# REGLAS DE COMPILACIÓN
# =========================================================================
all: $(OS_ISO)

kernel/Boot.class: $(JAVA_SOURCES)
	$(JAVAC) -g:none -Xbootclasspath/p:. -source 8 -target 8 $(JAVA_SOURCES)

%.o: %.asm
	$(AS) $(ASFLAGS) $< -o $@

$(KERNEL_BIN): $(ALL_OBJS)
	$(LD) $(LDFLAGS) -o $@ $(ALL_OBJS)

$(OS_ISO): $(KERNEL_BIN) kernel/Boot.class
	@$(MKDIR)
	@$(COPY_KERN)
	@$(COPY_CLS)
	@echo set timeout=0 > isodir/boot/grub/grub.cfg
	@echo set default=0 >> isodir/boot/grub/grub.cfg
	@echo menuentry "JVM-OS Self-Hosting" { >> isodir/boot/grub/grub.cfg
	@echo    set gfxpayload=1024x768x32 >> isodir/boot/grub/grub.cfg
	@echo    multiboot /boot/kernel.bin >> isodir/boot/grub/grub.cfg
	
	$(foreach src, $(JAVA_SOURCES), $(call ADD_MODULE, $(src)))    

	@echo    boot >> isodir/boot/grub/grub.cfg
	@echo } >> isodir/boot/grub/grub.cfg
ifeq ($(shell uname -s),Darwin)
	i686-elf-grub-mkrescue -o $(OS_ISO) isodir
else
	grub-mkrescue -o $(OS_ISO) isodir
endif    

#run: $(OS_ISO)
#	qemu-system-i386 -cdrom $(OS_ISO) -drive file=disk.img,format=raw -m 128M -serial stdio -netdev user,id=net0 -device rtl8139,netdev=net0 -rtc base=localtime -machine pcspk-audiodev=snd0 -audiodev pa,id=snd0 

# VGA, HDD, RTC, Sound, Network, PacketSniffer, 	
run: $(OS_ISO)
	qemu-system-i386 -cdrom $(OS_ISO) -drive file=disk.img,format=raw -m 128M -serial stdio -netdev user,id=net0,hostfwd=tcp::5555-:80 -device rtl8139,netdev=net0 -object filter-dump,id=f1,netdev=net0,file=trafico.pcap -rtc base=localtime -machine pcspk-audiodev=snd0 -audiodev pa,id=snd0	

run-mac: $(OS_ISO)
	qemu-system-i386 -cdrom $(OS_ISO) -boot d -m 128M -serial stdio -rtc base=localtime
    
clean:
	@$(CLEAN_CMD)

.PHONY: all run clean

# LISTA ESTRICTA DE CLASES JAVA A COMPILAR
#JAVA_SOURCES := kernel/Boot.java kernel/Native.java kernel/vfs/Node.java \
#                java/lang/Object.java java/lang/String.java java/lang/StringBuilder.java \
#                java/lang/System.java java/lang/Thread.java java/lang/Runtime.java \
#                java/awt/Color.java java/awt/Graphics2D.java java/awt/Graphics.java java/awt/Toolkit.java \
#                java/io/PrintStream.java java/io/RandomAccessFile.java \
#                java/net/DatagramPacket.java java/net/RawSocket.java \
#                java/util/Calendar.java java/io/InvalidClassException.java java/io/IOException.java \
#				java/io/Serializable.java java/lang/Boolean.java java/lang/Byte.java \
#				java/lang/Character.java java/lang/Class.java java/lang/CharSequence.java \
#				java/lang/Comparable.java java/lang/Deprecated.java java/lang/Double.java \
#				java/lang/Enum.java java/lang/ClassNotFoundException.java java/lang/FindBugsSuppressWarnings.java \
#				java/lang/HexStringParser.java java/lang/IllegalArgumentException.java \
#				java/lang/ClassLoader.java java/lang/Integer.java java/lang/IntegralToString.java \
#				java/lang/Long.java java/lang/Math.java java/lang/NullPointerException.java \
#				java/lang/Number.java java/lang/NumberFormatException.java java/lang/Override.java \
#				java/lang/RealToString.java java/lang/RuntimeException.java java/lang/StringToReal.java \
#				java/lang/SuppressWarnings.java java/lang/Throwable.java java/lang/Void.java java/lang/Float.java \
#				java/lang/annotation/CallSuper.java java/lang/annotation/Documented.java \
#				java/lang/annotation/ElementType.java java/lang/annotation/Retention.java \
#				java/lang/annotation/RetentionPolicy.java java/lang/annotation/Target.java \
#				java/lang/reflect/Array.java java/math/BigInteger.java java/util/Arrays.java \
#				java/util/Random.java java/util/regex/Matcher.java java/util/regex/MatchResult.java \
#				java/util/regex/MatchResultImpl.java java/util/regex/Pattern.java java/util/regex/Splitter.java
