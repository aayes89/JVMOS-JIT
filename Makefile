# COMPILADORES Y BANDERAS

ifeq ($(shell uname -s),Darwin)
    # En macOS obligamos a usar el cross-compiler de Homebrew
    CC = i686-elf-gcc
    LD = i686-elf-ld
else ifeq ($(shell uname -s),Linux)
    # En Linux o WSL podemos intentar usar los nativos con multilib
    CC = gcc
    LD = ld
else # windows
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

# Captura de fuentes
C_SOURCES   := $(wildcard *.c) $(wildcard */*.c)
ASM_SOURCES := $(wildcard *.asm) $(wildcard */*.asm) $(wildcard */*/*.asm)
ASM_SOURCES := $(filter-out boot/boot_class.asm, $(ASM_SOURCES))

C_OBJS   := $(patsubst %.c,%.o,$(C_SOURCES))
ASM_OBJS := $(patsubst %.asm,%.o,$(ASM_SOURCES))

BOOT_OBJ  := boot/multiboot.o
REST_OBJS := $(filter-out $(BOOT_OBJ), $(ASM_OBJS) $(C_OBJS))
ALL_OBJS  := $(BOOT_OBJ) $(REST_OBJS)

# Macro recursiva nativa de GNU Make (independiente del OS)
rwildcard=$(wildcard $1$2) $(foreach d,$(wildcard $1*),$(call rwildcard,$d/,$2))

# Busca TODOS los archivos .java dentro de 'kernel', 'java' y 'boot'
JAVA_SOURCES := $(call rwildcard,kernel/,*.java) \
                $(call rwildcard,java/,*.java) \
                $(call rwildcard,boot/,*.java)

# Macro para generar módulos de GRUB automáticamente
define ADD_MODULE
	@echo    module /classes/$(notdir $(1:.java=.class)) $(1:.java=) >> isodir/boot/grub/grub.cfg

endef

# Macro para generar módulos de GRUB automáticamente
define ADD_MODULE
	@echo module /classes/$(notdir $(1:.java=.class)) $(1:.java=) >> isodir/boot/grub/grub.cfg
endef                

# COMANDOS SEGUROS DE COPIA DE CLASES

ifeq ($(OS),Windows_NT)
    CLEAN_CMD = if exist isodir rmdir /s /q isodir & del /s /q *.bin *.iso *.class *.o
    MKDIR     = if not exist isodir\boot\grub mkdir isodir\boot\grub & if not exist isodir\classes mkdir isodir\classes
    COPY_KERN = copy kernel.bin isodir\boot\kernel.bin >nul
    # Windows: Copia carpeta por carpeta garantizando que no se omita ninguna
    COPY_CLS  = copy kernel\*.class isodir\classes\ >nul & copy kernel\vfs\*.class isodir\classes\ >nul & copy java\lang\*.class isodir\classes\ >nul & copy java\awt\*.class isodir\classes\ >nul & copy java\io\*.class isodir\classes\ >nul & copy java\net\*.class isodir\classes\ >nul & copy java\util\*.class isodir\classes\ >nul
else
    CLEAN_CMD = rm -rf isodir *.bin *.iso $(ALL_OBJS) $$(find . -name "*.class")
    MKDIR     = mkdir -p isodir/boot/grub isodir/classes
    COPY_KERN = cp $(KERNEL_BIN) isodir/boot/kernel.bin
    # POSIX: Encuentra y copia cualquier .class
    COPY_CLS  = find kernel java -name "*.class" -exec cp {} isodir/classes/ \;
endif

# REGLAS DE COMPILACIÓN

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

run: $(OS_ISO)
	qemu-system-i386 -cdrom $(OS_ISO) -drive file=disk.img,format=raw -m 128M -serial stdio -rtc base=localtime -machine pcspk-audiodev=snd0 -audiodev pa,id=snd0

run-mac: $(OS_ISO)
	qemu-system-i386 -cdrom $(OS_ISO) -boot d -m 128M -serial stdio -rtc base=localtime

clean:
	@$(CLEAN_CMD)

.PHONY: all run clean
