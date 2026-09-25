# JVMOS-JIT
It is an improved fork of the repository: https://github.com/aayes89/JVMOS.

# Requirements
* QEMU
* C-Compiler: gcc-12
* Linker: ld
* Assembly: nasm
* Java-Compiler from JDK: javac
* GRUB2
* GIT

# HOW to USE
* clone the repository <code>[git clone](https://github.com/aayes89/JVMOS-JIT.git)</code>
* run <code>clear && make clean && make run</code><br>
<b>Note:</b> I shared a 10MB image pre-configured for QEMU so you won't have any issues starting it up, but you can run `kernel.bin` if you'd like to test without a hard drive.

# Architecture
                 HARDWARE
                     │
                     ▼
              BOOTLOADER ASM
                     │
                     ▼
             KERNEL / RUNTIME ASM
                     │
          ┌──────────┴──────────┐
          ▼                     ▼
       HAL ASM               JAVA ABI 
          │                     │
          └──────────┬──────────┘
                     ▼
                 JVMOS-JIT
                 ASM → x86 
                     │
                     ▼
                JAVA BYTECODE 
                     │
                     ▼
          JAVA CLASSES and RUNTIME
                     │
                     ▼
                   JVMOS

# Commands 
- help: show the help menu
- ver: show information about JVMOS-JIT
- date: show date from CMOS (format dd/mm/yyy)
- time: show time from CMOS (format HH:mm:ss)
- startx: start UI (blocked until Filesystem 'BMFS' is finished)
- cube: a wireframe cube as demonstration of Graphics2D class implementation
- cube2d: a colored cube as demonstration of Graphics2D class implementation
- cube3d: a 3D cube rendered as demonstration of g3d libraries
- demo3d: a full demonstration of g3d capabilities
- cls / clear: clean the screen
- reboot: restart the system
- exit: shutdown the system
- Basic Filesystem implementation BMFS
  - run/java <file.class>: executes a .class file (java compiled file with bytecode)
  - cd <.|..|path>: change the actual directory given a path
  - rm <file|directory> : removes a file or directory
  - cp <file_org|directory_org> <file_dest|directory_dest>: copy a file or directory to a new path
  - mv <file_org|directory_org> <file_dest|directory_dest>: move a file or directory to a new path
  - paste: fetch the memory from the RAM and paste into actual directory
  - format: do format to the 1st HDD (caution: clear the whole disk, filling with '0')
- Basic Network support for (RTL8139 and PCnet (VBox))
  - net dhcp: initialize the network interface with data from DHCP server if exists
  - net ifconfig: show eth0 interface info
  - net arp-ping <ip>: do a PING with ARP
  - net ping <ip>: do a PING (ICMP)
  - net ip <ip>: set manually IP to the interface
  - net mask <mask>: set manually mask to the interface
  - net gw <ip_gw>: set manually the GateWay to the interface
  - net nslookup <dest>: do a nslookup command (todo)
  - net wget <url>: do a download for the given url (todo)


# TODO
* Alpha Blending
* Test useful apps (Notepad, Paint, Calculator)
* Sound support
* Full Network support (Parcial done)
  - DHCP working good in VBox and VMware 
* Improve actual filesystem
* (FAT/FAT32, NTFS, etc.) support (Parcial done)
  - BMFS: is a self implementation of FAT/FAT32

# Screenshots

### JIT Tests PASSED!
<img width="1016" height="389" alt="imagen" src="https://github.com/user-attachments/assets/ca85eb75-cab0-4432-a411-461381ccc0fc" />

### I/O
<img width="622" height="361" alt="imagen" src="https://github.com/user-attachments/assets/3e062dd3-c122-4653-8b60-7a0be0d3ddeb" />

### RTL8139 
## Rx
<img width="593" height="371" alt="imagen" src="https://github.com/user-attachments/assets/3d75dc57-c6b9-47b4-9b7a-e38deb8b0215" />



### Test UI
<img width="1024" height="834" alt="imagen" src="https://github.com/user-attachments/assets/431dbd24-4842-4d5b-98a3-fe33ddc94120" />

<img width="1021" height="826" alt="imagen" src="https://github.com/user-attachments/assets/4bd80ea3-48e1-4743-b3a5-f7174f54dbbe" />

## 3D motor

<img width="913" height="624" alt="imagen" src="https://github.com/user-attachments/assets/c6b57487-223f-4d84-9695-eed51c03e02e" />
