; MIT License
;
; Copyright (c) 2026 Allan (Slam)
;
; Permission is hereby granted, free of charge, to any person obtaining a copy
; of this software and associated documentation files (the "Software"), to deal
; in the Software without restriction, including without limitation the rights
; to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
; copies of the Software, and to permit persons to whom the Software is
; furnished to do so, subject to the following conditions:
;
; The above copyright notice and this permission notice shall be included in all
; copies or substantial portions of the Software.
;
; THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
; IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
; FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
; AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
; LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
; OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
; SOFTWARE.

; HAL Baremetal Mejorado para JVM (x86 32-bit)
[bits 32]

; SÍMBOLOS GLOBALES EXPORTADOS
; --- Core sistema / interrupciones ---
global sys_hardware_init
global sys_hlt
global sys_exit
global sys_sleep
global sys_get_ticks
global sys_cli
global sys_sti

; --- Memoria ---
global sys_kalloc
global sys_get_free_mem
global sys_get_ram_size
global sys_memcpy
global sys_memset

; --- Serie (debug JVM) ---
global sys_serial_init
global sys_serial_putc
global sys_serial_puts
global sys_serial_print_java

; --- PCI ---
global sys_pci_read_config

; --- Entrada (por IRQ + FIFO) ---
global sys_init_keyboard
global sys_read_keyboard_scancode
global sys_set_keyboard_layout
global sys_init_mouse
global sys_read_mouse

; --- Gráficos VBE ---
global sys_set_color
global sys_draw_pixel
global sys_get_pixel
global sys_fill_rect
global sys_draw_rect
global sys_draw_line
global sys_draw_oval
global sys_fill_oval
global sys_draw_arc
global sys_fill_arc
global sys_draw_polygon
global sys_fill_polygon
global sys_draw_string
global current_color

; --- Disco ATA IDE LBA28 ---
global sys_disk_read_sector
global sys_disk_write_sector

; --- Tiempo CMOS ---
global sys_get_time

; --- Audio PC Speaker ---
global sys_beep
global sys_nosound

; --- Red RTL8139 ---
global sys_rtl8139_init
global sys_rtl8139_send_packet
global sys_net_receive_packet

; --- Puertos I/O ---
global sys_inb
global sys_outb
global sys_inw
global sys_outw
global sys_indw
global sys_outdw
global sys_wait_io

; Externos del Kernel/Framebuffer
extern g_framebuffer
extern g_pitch
extern draw_char_vram

; SECCIÓN BSS (MEMORIA NO INICIALIZADA)
section .bss
align 16

sys_ticks           resd 1

heap_curr_ptr       resd 1
heap_start_ptr      resd 1

kbd_fifo_buf        resb 256
kbd_fifo_head       resd 1
kbd_fifo_tail       resd 1
kbd_layout          resd 1          ; 0=US, 1=LATAM
kbd_shift_state     resb 1

mouse_cycle         resb 1
mouse_byte          resb 3
mouse_x             resd 1
mouse_y             resd 1
mouse_btn           resd 1

current_color       resd 1

disk_sector_buf     resb 512

rtl8139_io_port     resw 1
rx_buffer           resb 8192 + 16

; SECCIÓN TEXT (CÓDIGO EJECUTABLE)
section .text
; INICIALIZACIÓN DE HARDWARE CENTRALIZADA
sys_hardware_init:
    cli

    call sys_serial_init
    call sys_init_pic
    call sys_init_pit
    call sys_setup_idt

    mov dword [kbd_fifo_head], 0
    mov dword [kbd_fifo_tail], 0
    mov byte  [kbd_shift_state], 0
    mov dword [kbd_layout], 1       ; Por defecto LATAM/Español activo
    mov byte  [mouse_cycle], 0
    mov dword [mouse_x], 512
    mov dword [mouse_y], 384
    mov dword [mouse_btn], 0
    mov dword [sys_ticks], 0
    mov dword [heap_curr_ptr], 0
    mov dword [heap_start_ptr], 0x00400000
    mov dword [current_color], 0xFFFFFFFF

    call sys_init_keyboard
    call sys_init_mouse

    sti
    ret

sys_cli:
    cli
    ret
sys_sti:
    sti
    ret


; PUERTO SERIE UART 16550 (COM1 @ 0x3F8)

sys_serial_init:
    mov dx, 0x3F9
    mov al, 0x00
    out dx, al

    mov dx, 0x3FB
    mov al, 0x80
    out dx, al

    mov dx, 0x3F8
    mov al, 0x03                ; Divisor 3 -> 38400 baudios
    out dx, al
    mov dx, 0x3F9
    mov al, 0x00
    out dx, al

    mov dx, 0x3FB
    mov al, 0x03                ; 8N1
    out dx, al

    mov dx, 0x3FA
    mov al, 0xC7
    out dx, al

    mov dx, 0x3FC
    mov al, 0x0B
    out dx, al
    ret

sys_serial_putc:
    push ebp
    mov ebp, esp
    mov dx, 0x3FD
.wait_thre:
    in al, dx
    test al, 0x20
    jz .wait_thre
    mov dx, 0x3F8
    mov al, [ebp + 8]
    out dx, al
    pop ebp
    ret

sys_serial_puts:
    push ebp
    mov ebp, esp
    push esi
    mov esi, [ebp + 8]
    test esi, esi
    jz .done
.loop:
    movzx eax, byte [esi]
    test al, al
    jz .done
    push eax
    call sys_serial_putc
    add esp, 4
    inc esi
    jmp .loop
.done:
    pop esi
    pop ebp
    ret

; IMPRESIÓN EN CONSOLA SERIE
sys_serial_print_java:
    push ebp
    mov ebp, esp
    pusha
    mov esi, [ebp + 8]          ; Objeto String real
    test esi, esi
    jz .done
    
    ; Extraer byte[] value
    mov esi, [esi + 8]
    test esi, esi
    jz .done

    ; Extraer longitud
    mov ecx, [esi]
    test ecx, ecx
    jz .done

    ; Apuntar a caracteres
    add esi, 4
.loop:
    movzx eax, byte [esi]
    push eax
    call sys_serial_putc
    add esp, 4
    inc esi
    dec ecx
    jnz .loop
.done:
    popa
    pop ebp
    ret


; CONTROLADOR DE INTERRUPCIONES PIC 8259A

sys_init_pic:
    mov al, 0x11
    out 0x20, al
    out 0xA0, al

    mov al, 0x20                ; Master -> IRQ 0x20-0x27
    out 0x21, al
    mov al, 0x28                ; Slave -> IRQ 0x28-0x2F
    out 0xA1, al

    mov al, 0x04
    out 0x21, al
    mov al, 0x02
    out 0xA1, al

    mov al, 0x01
    out 0x21, al
    out 0xA1, al

    mov al, 0xF8                ; Habilitar IRQ0, IRQ1, IRQ2
    out 0x21, al
    mov al, 0xEF                ; Habilitar IRQ12 (Mouse)
    out 0xA1, al
    ret


; TEMPORIZADOR PIT (1000 Hz)

sys_init_pit:
    mov al, 0x36
    out 0x43, al
    mov al, 0xA9
    out 0x40, al
    mov al, 0x04
    out 0x40, al
    ret


; IDT Y MANEJADORES DE INTERRUPCIÓN

sys_setup_idt:
    mov dword [idtr_base], idt_entries
    mov word  [idtr_limit], 2047

    mov edi, idt_entries
    mov ecx, 256 * 2
    xor eax, eax
    rep stosd

    mov eax, irq0_timer_handler
    mov ebx, 0x20
    call set_idt_gate

    mov eax, irq1_keyboard_handler
    mov ebx, 0x21
    call set_idt_gate

    mov eax, irq2_cascade_handler
    mov ebx, 0x22
    call set_idt_gate

    mov eax, irq12_mouse_handler
    mov ebx, 0x2C
    call set_idt_gate

    mov ecx, 0
.exc_loop:
    mov eax, exception_stub
    mov ebx, ecx
    push ecx
    call set_idt_gate
    pop ecx
    inc ecx
    cmp ecx, 32
    jl .exc_loop

    lidt [idtr]
    ret

set_idt_gate:
    push ebx
    shl ebx, 3
    add ebx, idt_entries
    mov [ebx], ax
    mov word [ebx + 2], 0x08
    mov byte [ebx + 4], 0x00
    mov byte [ebx + 5], 0x8E
    shr eax, 16
    mov [ebx + 6], ax
    pop ebx
    ret

irq0_timer_handler:
    pusha
    inc dword [sys_ticks]
    mov al, 0x20
    out 0x20, al
    popa
    iret

; IRQ1: MANEJADOR DE TECLADO PS/2 MEJORADO
irq1_keyboard_handler:
    pusha
    in al, 0x60

    ; Evaluar estados de SHIFT (0x2A / 0x36 presionado, 0xAA / 0xB6 liberado)
    cmp al, 0x2A
    je .shift_on
    cmp al, 0x36
    je .shift_on
    cmp al, 0xAA
    je .shift_off
    cmp al, 0xB6
    je .shift_off

    ; Ignorar cualquier evento de liberación de tecla (bit 7)
    test al, 0x80
    jnz .eoi_only

    ; Almacenar el Scancode en el FIFO circular
    mov ebx, [kbd_fifo_tail]
    mov ecx, ebx
    inc ecx
    and ecx, 0xFF
    cmp ecx, [kbd_fifo_head]
    je .eoi_only

    mov [kbd_fifo_buf + ebx], al
    mov [kbd_fifo_tail], ecx
    jmp .eoi_only

.shift_on:
    mov byte [kbd_shift_state], 1
    jmp .eoi_only
.shift_off:
    mov byte [kbd_shift_state], 0

.eoi_only:
    mov al, 0x20
    out 0x20, al
    popa
    iret

irq2_cascade_handler:
    pusha
    mov al, 0x20
    out 0x20, al
    popa
    iret

irq12_mouse_handler:
    pusha
    in al, 0x60

    movzx ebx, byte [mouse_cycle]
    cmp bl, 0
    je .m_byte0
    cmp bl, 1
    je .m_byte1
    cmp bl, 2
    je .m_byte2
    jmp .m_reset

.m_byte0:
    test al, 0x08
    jz .m_reset
    mov [mouse_byte], al
    mov byte [mouse_cycle], 1
    jmp .m_eoi
.m_byte1:
    mov [mouse_byte + 1], al
    mov byte [mouse_cycle], 2
    jmp .m_eoi
.m_byte2:
    mov [mouse_byte + 2], al
    mov byte [mouse_cycle], 0

    mov al, [mouse_byte]
    and eax, 0x07
    mov [mouse_btn], eax

    mov al, [mouse_byte + 1]
    movsx eax, al
    add [mouse_x], eax

    mov al, [mouse_byte + 2]
    movsx eax, al
    sub [mouse_y], eax

    cmp dword [mouse_x], 0
    jge .cx1
    mov dword [mouse_x], 0
.cx1:
    cmp dword [mouse_x], 1016
    jle .cy1
    mov dword [mouse_x], 1016
.cy1:
    cmp dword [mouse_y], 0
    jge .cy2
    mov dword [mouse_y], 0
.cy2:
    cmp dword [mouse_y], 760
    jle .m_eoi
    mov dword [mouse_y], 760
    jmp .m_eoi

.m_reset:
    mov byte [mouse_cycle], 0

.m_eoi:
    mov al, 0x20
    out 0xA0, al
    out 0x20, al
    popa
    iret

exception_stub:
    pusha
    push exception_msg
    call sys_serial_puts
    add esp, 4
.halt:
    hlt
    jmp .halt

exception_msg:
    db 13, 10, "[HAL Panic] CPU Exception! System Halted.", 13, 10, 0

; TEMPORIZACIÓN Y MEMORIA
; Obtener contador de Ticks (ms desde el arranque)

sys_get_ticks:
    mov eax, [sys_ticks]
    ret


; Suspender ejecución por N milisegundos (Latencia ultra-baja)

sys_sleep:
    push ebp
    mov ebp, esp
    push ebx

    mov eax, [ebp + 8]          ; milisegundos solicitados por Java
    cmp eax, 0
    jle .done                   ; Si es <= 0 ms, retornar de inmediato

    mov ebx, [sys_ticks]
    add ebx, eax                ; ebx = tick_objetivo

.wait:
    cmp dword [sys_ticks], ebx
    jae .done

    sti                         ; Asegurar interrupciones activas para despertar
    hlt                         ; Suspender CPU hasta la siguiente IRQ
    jmp .wait

.done:
    pop ebx
    pop ebp
    ret


; Asignador de Memoria Kernel (Heap Allocator - Alineado a 4 bytes)

sys_kalloc:
    push ebp
    mov ebp, esp
    push ebx

    cmp dword [heap_curr_ptr], 0
    jne .do_alloc

    mov eax, [heap_start_ptr]
    cmp eax, 0
    jne .set_start

    mov eax, 0x00400000

.set_start:
    mov [heap_curr_ptr], eax

.do_alloc:
    mov eax, [heap_curr_ptr]
    mov ecx, [ebp + 8]

    test ecx, ecx
    jz .done_alloc

    add ecx, 3
    jc .fail

    and ecx, ~3

    mov ebx, eax
    add ebx, ecx
    jc .fail

    cmp ebx, 0x08000000
    ja .fail

    mov [heap_curr_ptr], ebx
    jmp .done_alloc

.fail:
    xor eax, eax

.done_alloc:
    pop ebx
    pop ebp
    ret
	
; Obtener Memoria Disponible en el Heap

sys_get_free_mem:
    cmp dword [heap_curr_ptr], 0
    jne .ok
    mov dword [heap_curr_ptr], 0x00400000
.ok:
    ; Memoria libre = RAM total (128MB) - Puntero actual del Heap
    mov eax, 0x08000000
    sub eax, [heap_curr_ptr]
    ret


; Obtener tamaño total de la memoria RAM (128 MB)

sys_get_ram_size:
    mov eax, 0x08000000         ; 128 MB en bytes
    ret


; Copia de bloques de memoria byte a byte segura

sys_memcpy:
    push ebp
    mov ebp, esp
    push edi
    push esi

    mov edi, [ebp + 8]          ; destino
    mov esi, [ebp + 12]         ; origen
    mov ecx, [ebp + 16]         ; tamaño

    test ecx, ecx
    jz .done_memcpy

    cld                         ; Limpiar Direction Flag (copiar hacia adelante)
    rep movsb

.done_memcpy:
    mov eax, [ebp + 8]          ; Retornar puntero destino
    pop esi
    pop edi
    pop ebp
    ret


; Relleno de bloques de memoria

sys_memset:
    push ebp
    mov ebp, esp
    push edi

    mov edi, [ebp + 8]          ; destino
    mov al, [ebp + 12]          ; valor (byte)
    mov ecx, [ebp + 16]         ; tamaño

    test ecx, ecx
    jz .done_memset

    cld                         ; Limpiar Direction Flag
    rep stosb

.done_memset:
    mov eax, [ebp + 8]          ; Retornar puntero destino
    pop edi
    pop ebp
    ret


; Detención temporal de la CPU (HLT)

sys_hlt:
    sti
    hlt
    ret


; Apagado / Salida del Sistema Operativo (QEMU / Bochs / ACPI)

sys_exit:
    cli
    ; QEMU / Bochs Poweroff via I/O Ports
    mov ax, 0x2000
    mov dx, 0x604
    out dx, ax
    mov dx, 0xB004
    out dx, ax
    mov al, 0x00
    out 0x501, al
.hang:
    hlt
    jmp .hang


; BUS PCI

sys_pci_read_config:
    push ebp
    mov ebp, esp
	push ebx
    mov eax, [ebp + 8]          ; bus
    shl eax, 16
    mov ebx, [ebp + 12]         ; slot
    shl ebx, 11
    or eax, ebx
    mov ebx, [ebp + 16]         ; func
    shl ebx, 8
    or eax, ebx
    mov ebx, [ebp + 20]         ; offset
    and ebx, 0xFC
    or eax, ebx
    or eax, 0x80000000
    mov dx, 0xCF8
    out dx, eax
    mov dx, 0xCFC
    in eax, dx
	pop ebx
    pop ebp
    ret


; DRIVERS DE TECLADO Y RATÓN

sys_init_keyboard:	
    ; Habilitar puerto PS/2 primario
    mov al, 0xAE
    out 0x64, al
	mov al, 0x20
    out 0x64, al
    call .wait_read
    in al, 0x60
    or al, 0x01
    push eax
    mov al, 0x60
    out 0x64, al
    call .wait_write
    pop eax
    out 0x60, al
    call .wait_write
    mov al, 0xF4
    out 0x60, al
.flush_kbd:
    in al, 0x64
    test al, 0x01
    jz .done
    in al, 0x60
    jmp .flush_kbd
.done:
    ret
.wait_read:
    in al, 0x64
    test al, 0x01
    jz .wait_read
    ret
.wait_write:
    in al, 0x64
    test al, 0x02
    jnz .wait_write
    ret

sys_set_keyboard_layout:
    push ebp
    mov ebp, esp
    mov eax, [ebp + 8]
    mov [kbd_layout], eax
    pop ebp
    ret

; Lectura de FIFO sin Polling invasivo
sys_read_keyboard_scancode:
    push ebx
    push ecx
    push edx
    
    xor eax, eax                ; Asegurar EAX en 0 desde el principio
    
    mov ebx, [kbd_fifo_head]
    cmp ebx, [kbd_fifo_tail]
    je .done                    ; Saltar directamente si está vacío
    
    mov al, byte [kbd_fifo_buf + ebx]
    inc ebx
    and ebx, 0xFF
    mov [kbd_fifo_head], ebx
    
    cmp eax, 128
    jge .empty                  ; Si es un scancode de liberación (>128), retornar 0
    
    mov al, [kbd_ascii_map + eax]
    movzx eax, al
    
    cmp al, 'A'
    jl .done
    cmp al, 'Z'
    jg .done
    add al, 32                  ; Convertir a minúscula
    jmp .done

.empty:
    xor eax, eax
.done:
    pop edx
    pop ecx
    pop ebx
    ret

; =====================================================================
; CORRECCIÓN EN: sys_api.asm
; =====================================================================
sys_init_mouse:
    push eax
    
    ; 1. Habilitar dispositivo auxiliar en el PS/2
    mov al, 0xA8
    out 0x64, al
    call .wait_write

    ; 2. Habilitar IRQ12 en el Command Configuration Byte (CCB)
    mov al, 0x20
    out 0x64, al
    call .wait_read
    in al, 0x60
    or al, 0x02         ; Bit 1 activa la interrupción del mouse
    push eax
    mov al, 0x60
    out 0x64, al
    call .wait_write
    pop eax
    out 0x60, al
    call .wait_write

    ; 3. Habilitar el reporte de datos hacia el mouse
    mov al, 0xD4
    out 0x64, al
    call .wait_write
    mov al, 0xF4
    out 0x60, al
    call .wait_read
    in al, 0x60         ; Leer ACK (0xFA)

.flush_mouse:
    in al, 0x64
    test al, 0x01
    jz .done_flush_m
    in al, 0x60
    jmp .flush_mouse
    
.done_flush_m:
    mov byte [mouse_cycle], 0
    pop eax
    ret

.wait_read:
    in al, 0x64
    test al, 0x01
    jz .wait_read
    ret

.wait_write:
    in al, 0x64
    test al, 0x02
    jnz .wait_write
    ret

sys_read_mouse:
    push ebp
    mov ebp, esp
    mov ecx, [ebp + 8]
    cmp ecx, 0
    je .rx
    cmp ecx, 1
    je .ry
    mov eax, [mouse_btn]
    pop ebp
    ret
.rx:
    mov eax, [mouse_x]
    pop ebp
    ret
.ry:
    mov eax, [mouse_y]
    pop ebp
    ret


; RENDERIZADOR Y DRIVER GRÁFICO VBE VESA

sys_set_color:
    push ebp
    mov ebp, esp
    mov eax, [ebp + 8]
    or eax, 0xFF000000          ; Forzar canal Alpha opaco (24bpp / 32bpp)
    mov [current_color], eax
    pop ebp
    ret

sys_draw_pixel:
    push ebp
    mov ebp, esp
    mov eax, [ebp + 8]          ; x
    mov ecx, [ebp + 12]         ; y
    mov edx, [current_color]
    imul ecx, [g_pitch]
    shl eax, 2
    add ecx, eax
    mov eax, [g_framebuffer]
    add eax, ecx
    mov [eax], edx
    pop ebp
    ret

sys_get_pixel:
    push ebp
    mov ebp, esp
    mov eax, [ebp + 8]
    mov ecx, [ebp + 12]
    imul ecx, [g_pitch]
    shl eax, 2
    add ecx, eax
    mov eax, [g_framebuffer]
    add eax, ecx
    mov eax, [eax]
    pop ebp
    ret

sys_fill_rect:
    push ebp
    mov ebp, esp
    push edi
    push ebx
    push esi
    mov ebx, [ebp + 16]         ; w
    mov edx, [ebp + 20]         ; h
    mov esi, [current_color]
    test ebx, ebx
    jle .done
    test edx, edx
    jle .done
.row:
    push edx
    mov ecx, [ebp + 12]         ; y
    imul ecx, [g_pitch]
    mov eax, [ebp + 8]          ; x
    shl eax, 2
    add ecx, eax
    mov edi, [g_framebuffer]
    add edi, ecx
    mov ecx, ebx
    mov eax, esi
    rep stosd
    pop edx
    inc dword [ebp + 12]
    dec edx
    jnz .row
.done:
    pop esi
    pop ebx
    pop edi
    pop ebp
    ret

sys_draw_rect:
    push ebp
    mov ebp, esp
    push 1
    push dword [ebp + 16]
    push dword [ebp + 12]
    push dword [ebp + 8]
    call sys_fill_rect
    add esp, 16

    mov eax, [ebp + 12]
    add eax, [ebp + 20]
    dec eax
    push 1
    push dword [ebp + 16]
    push eax
    push dword [ebp + 8]
    call sys_fill_rect
    add esp, 16

    push dword [ebp + 20]
    push 1
    push dword [ebp + 12]
    push dword [ebp + 8]
    call sys_fill_rect
    add esp, 16

    mov eax, [ebp + 8]
    add eax, [ebp + 16]
    dec eax
    push dword [ebp + 20]
    push 1
    push dword [ebp + 12]
    push eax
    call sys_fill_rect
    add esp, 16
    pop ebp
    ret

sys_draw_line:
    push ebp
    mov ebp, esp
    push ebx
    push esi
    push edi
    sub esp, 24

    mov eax, [ebp + 16]
    sub eax, [ebp + 8]
    jns .absdx
    neg eax
.absdx:
    mov [ebp - 4], eax          ; dx

    mov eax, [ebp + 20]
    sub eax, [ebp + 12]
    jns .absdy
    neg eax
.absdy:
    neg eax
    mov [ebp - 8], eax          ; -dy

    mov eax, [ebp + 8]
    cmp eax, [ebp + 16]
    jl .sxpos
    mov dword [ebp - 12], -1
    jmp .sy
.sxpos:
    mov dword [ebp - 12], 1
.sy:
    mov eax, [ebp + 12]
    cmp eax, [ebp + 20]
    jl .sypos
    mov dword [ebp - 16], -1
    jmp .err
.sypos:
    mov dword [ebp - 16], 1
.err:
    mov eax, [ebp - 4]
    add eax, [ebp - 8]
    mov [ebp - 20], eax

.loop:
    push dword [ebp + 12]
    push dword [ebp + 8]
    call sys_draw_pixel
    add esp, 8

    mov eax, [ebp + 8]
    cmp eax, [ebp + 16]
    jne .cont
    mov eax, [ebp + 12]
    cmp eax, [ebp + 20]
    je .done
.cont:
    mov eax, [ebp - 20]
    shl eax, 1
    cmp eax, [ebp - 8]
    jl .check2
    mov ecx, [ebp - 8]
    add [ebp - 20], ecx
    mov ecx, [ebp - 12]
    add [ebp + 8], ecx
.check2:
    cmp eax, [ebp - 4]
    jg .loop
    mov ecx, [ebp - 4]
    add [ebp - 20], ecx
    mov ecx, [ebp - 16]
    add [ebp + 12], ecx
    jmp .loop
.done:
    add esp, 24
    pop edi
    pop esi
    pop ebx
    pop ebp
    ret

sys_draw_oval:
sys_fill_oval:
sys_draw_arc:
sys_fill_arc:
    push ebp
    mov ebp, esp
    push dword [ebp + 20]
    push dword [ebp + 16]
    push dword [ebp + 12]
    push dword [ebp + 8]
    call sys_draw_rect
    add esp, 16
    pop ebp
    ret

sys_draw_polygon:
sys_fill_polygon:
    ret

; IMPRESIÓN DE CADENAS DE TEXTO
sys_draw_string:
    push ebp
    mov ebp, esp
    pusha
    mov ebx, [ebp + 8]          ; x
    mov edx, [ebp + 12]         ; y
    mov esi, [ebp + 16]         ; puntero al Objeto String real
    
    test esi, esi
    jz .done

    ; Extraer byte[] value del String (alojado en el Offset 8)
    mov esi, [esi + 8]
    test esi, esi
    jz .done

    ; Extraer longitud del byte[] (alojada en el Offset 0)
    mov ecx, [esi]
    test ecx, ecx
    jz .done

    ; Apuntar a los caracteres reales (Offset 4)
    add esi, 4
    
    mov edi, [current_color]
    or edi, 0xFF000000          

.char:
    mov al, [esi]
    cmp al, 13
    je .skip_char
    cmp al, 10
    je .skip_char
    cmp al, 32
    jl .skip_char
    
    pusha
    push edi
    push edx
    push ebx
    movzx eax, al
    push eax
    call draw_char_vram
    add esp, 16
    popa
    
.skip_char:
    add ebx, 10
    inc esi
    dec ecx
    jnz .char

.done:
    popa
    pop ebp
    ret


; CMOS RELOJ REAL (RTC)

sys_get_time:
    push ebp
    mov ebp, esp
    push ebx

    mov eax, [ebp + 8]
    cmp eax, 0
    je .sec
    cmp eax, 1
    je .min
    cmp eax, 2
    je .hour
    cmp eax, 3
    je .day
    cmp eax, 4
    je .month
    cmp eax, 5
    je .year
    
    xor eax, eax
    pop ebx
    pop ebp
    ret

.sec:   mov al, 0x00
        jmp .read
.min:   mov al, 0x02
        jmp .read
.hour:  mov al, 0x04
        jmp .read
.day:   mov al, 0x07
        jmp .read
.month: mov al, 0x08
        jmp .read
.year:  mov al, 0x09
.read:
    out 0x70, al
    out 0x80, al
    in al, 0x71
    movzx ebx, al
    mov eax, ebx
    and eax, 0x0F
    shr ebx, 4
    and ebx, 0x0F
    imul ebx, 10
    add eax, ebx

    pop ebx
    pop ebp
    ret


; PARLANTE PC SPEAKER
sys_beep:
    push ebp
    mov ebp, esp
    push ebx

    mov ecx, [ebp + 8]
    test ecx, ecx
    jz .off

    mov eax, 1193180
    xor edx, edx
    div ecx

    mov ebx, eax

    mov al, 0xB6
    out 0x43, al

    mov al, bl
    out 0x42, al

    mov al, bh
    out 0x42, al

    in al, 0x61
    or al, 0x03
    out 0x61, al

    jmp .done

.off:
    call sys_nosound

.done:
    pop ebx
    pop ebp
    ret

sys_nosound:
    in al, 0x61
    and al, 0xFC
    out 0x61, al
    ret


; RED RTL8139
sys_rtl8139_init:
    push ebp
    mov ebp, esp
    mov ax, [ebp + 8]
    mov [rtl8139_io_port], ax

    mov dx, ax
    add dx, 0x37
    mov al, 0x10
    out dx, al
.wait_rst:
    in al, dx
    test al, 0x10
    jnz .wait_rst

    ; Configurar inicio del buffer de recepción (RBSTART)
    mov dx, [rtl8139_io_port]
    add dx, 0x30
    mov eax, rx_buffer
    out dx, eax

    ; Configurar RCR para aceptar Broadcast y Physical Match (0x8F)
    mov dx, [rtl8139_io_port]
    add dx, 0x44
    mov eax, 0x8F
    out dx, eax

    ; Habilitar Rx y Tx
    mov dx, [rtl8139_io_port]
    add dx, 0x37
    mov al, 0x0C
    out dx, al

	mov eax, 1	; retornar 1 (éxito)
    pop ebp
    ret

sys_rtl8139_send_packet:
    push ebp
    mov ebp, esp
    push esi

    mov esi, [ebp + 8]
    mov ecx, [ebp + 12]

    mov dx, [rtl8139_io_port]
    add dx, 0x20
    mov eax, esi
    out dx, eax

    mov dx, [rtl8139_io_port]
    add dx, 0x10
    mov eax, ecx
    out dx, eax

    pop esi
    pop ebp
    ret

sys_net_receive_packet:
    push ebp
    mov ebp, esp
    push ebx
    push esi
    push edi

    ; Chequear Command Register (0x37) bit 0 (BUFE - Buffer Empty)
    mov dx, [rtl8139_io_port]
    add dx, 0x37
    in al, dx
    test al, 0x01
    jnz .no_packet

    ; Hay paquete? Obtener puntero actual en el anillo
    mov ebx, [rtl8139_rx_ptr]
    mov esi, rx_buffer
    add esi, ebx

    ; Leer longitud del paquete desde la cabecera hardware
    movzx ecx, word [esi + 2]

    ; Copiar payload al buffer en Java (Arg C está en ebp+8 vía dispatch)
    mov edi, [ebp + 8]
    push ecx
    add esi, 4          ; Saltar cabecera de 4 bytes de RTL8139
    sub ecx, 4          ; Copiar el payload puro
    rep movsb
    pop ecx

    ; Actualizar puntero Rx circular alineado a 4 bytes
    add ebx, ecx
    add ebx, 4
    add ebx, 3
    and ebx, ~3
    cmp ebx, 8192
    jl .no_wrap
    sub ebx, 8192
.no_wrap:
    mov [rtl8139_rx_ptr], ebx

    ;Notificar a la tarjeta actualizando el CAPR (0x38)
    mov dx, [rtl8139_io_port]
    add dx, 0x38
    mov eax, ebx
    sub eax, 16
    out dx, ax

    ; Limpiar bit Rx OK en el ISR (0x3E) para recibir más
    mov dx, [rtl8139_io_port]
    add dx, 0x3E
    mov ax, 0x01
    out dx, ax

    ; Retornar tamaño del paquete a Java
    mov eax, ecx
    sub eax, 4
    jmp .done

.no_packet:
    xor eax, eax

.done:
    pop edi
    pop esi
    pop ebx
    pop ebp
    ret

; DISCO ATA IDE LBA28

sys_disk_read_sector:
    push ebp
    mov ebp, esp
    push ebx
    push edi

	mov edi, [ebp + 12]         ; Obtener puntero del argumento
    cmp edi, 0                  ; Si es 0 (null)...
    jne .skip_default_r
    mov edi, disk_sector_buf    ; ...usar el buffer global de 512 bytes
.skip_default_r:    
    mov dx, 0x1F7
.wait_bsy:
    in al, dx
    test al, 0x80
    jnz .wait_bsy

    mov eax, [ebp + 8]          ; LBA
    mov edi, [ebp + 12]         ; buffer

    mov dx, 0x1F6
    shr eax, 24
    or al, 0xE0
    out dx, al

    mov dx, 0x1F2
    mov al, 1
    out dx, al

    mov eax, [ebp + 8]
    mov dx, 0x1F3
    out dx, al
    shr eax, 8
    mov dx, 0x1F4
    out dx, al
    shr eax, 8
    mov dx, 0x1F5
    out dx, al

    mov dx, 0x1F7
    mov al, 0x20
    out dx, al

.wait_drq:
    in al, dx
    test al, 0x08
    jz .wait_drq

    mov ecx, 256
    mov dx, 0x1F0
.read:
    in ax, dx
    mov [edi], ax
    add edi, 2
    loop .read

    mov eax, 1
    pop edi
    pop ebx
    pop ebp
    ret

sys_disk_write_sector:
    push ebp
    mov ebp, esp
    push ebx
    push esi
	
	mov esi, [ebp + 12]         ; Obtener puntero del argumento
    cmp esi, 0                  ; Si es 0 (null)...
    jne .skip_default_w
    mov esi, disk_sector_buf    ; ...usar el buffer global de 512 bytes
.skip_default_w:
    mov dx, 0x1F7
.wait_bsy_w:
    in al, dx
    test al, 0x80
    jnz .wait_bsy_w

    mov eax, [ebp + 8]
    mov esi, [ebp + 12]

    mov dx, 0x1F6
    shr eax, 24
    or al, 0xE0
    out dx, al

    mov dx, 0x1F2
    mov al, 1
    out dx, al

    mov eax, [ebp + 8]
    mov dx, 0x1F3
    out dx, al
    shr eax, 8
    mov dx, 0x1F4
    out dx, al
    shr eax, 8
    mov dx, 0x1F5
    out dx, al

    mov dx, 0x1F7
    mov al, 0x30
    out dx, al

.wait_drq_w:
    in al, dx
    test al, 0x08
    jz .wait_drq_w

    mov ecx, 256
    mov dx, 0x1F0
.write:
    mov ax, [esi]
    out dx, ax
    add esi, 2
    loop .write
	
	mov dx, 0x1F7
	mov al, 0xE7	; ATA CACHE FLUSH
	out dx, al

.wait_flush:	
	in al, dx
	test al, 0x80	; Esperar a BSY (Bit 7) sea 0
	jnz .wait_flush

    mov eax, 1
    pop esi
    pop ebx
    pop ebp
    ret


; PUERTOS DEDICADOS I/O

sys_inb:
    push ebp
    mov ebp, esp
    mov dx, [ebp + 8]
    in al, dx
    pop ebp
    ret
sys_outb:
    push ebp
    mov ebp, esp
    mov dx, [ebp + 8]
    mov al, [ebp + 12]
    out dx, al
    pop ebp
    ret
sys_inw:
    push ebp
    mov ebp, esp
    mov dx, [ebp + 8]
    in ax, dx
    pop ebp
    ret
sys_outw:
    push ebp
    mov ebp, esp
    mov dx, [ebp + 8]
    mov ax, [ebp + 12]
    out dx, ax
    pop ebp
    ret
sys_indw:
    push ebp
    mov ebp, esp
    mov dx, [ebp + 8]
    in eax, dx
    pop ebp
    ret
sys_outdw:
    push ebp
    mov ebp, esp
    mov dx, [ebp + 8]
    mov eax, [ebp + 12]
    out dx, eax
    pop ebp
    ret
sys_wait_io:
    out 0x80, al
    ret

; SECCIÓN DATA Y RODATA
section .data
align 16

rtl8139_rx_ptr		dd 0
idtr:
    idtr_limit      dw 2047
    idtr_base       dd idt_entries
align 16
idt_entries:        times 256 * 8 db 0

section .rodata
align 4

kbd_ascii_map:
    ; 0x00 - 0x0F
    db 0, 27, '1', '2', '3', '4', '5', '6', '7', '8', '9', '0', '=', '+', 8, 9
    ; 0x10 - 0x1F
    db 'Q', 'W', 'E', 'R', 'T', 'Y', 'U', 'I', 'O', 'P', '[', ']', 13, 0, 'A', 'S'
    ; 0x20 - 0x2F
    db 'D', 'F', 'G', 'H', 'J', 'K', 'L', ';', '{', '|', 0, '}', 'Z', 'X', 'C', 'V'    
    ; 0x30 - 0x3F (0x39 es el ESPACIO ASCII 32)
    db 'B', 'N', 'M', ',', '.', '-', 0, '*', 0, 32, 0, 0, 0, 0, 0, 0
    ; 0x40 - 0x4F (Teclado numérico)
    db 0, 0, 0, 0, 0, 0, 0, '7', '8', '9', '-', '4', '5', '6', '+', '1'
    db '2', '3', '0', '.', 0, 0, '<', 0, 0, 0, 0, 0, 0, 0, 0, 0
    times 32 db 0

section .note.GNU-stack noalloc noexec nowrite progbits
