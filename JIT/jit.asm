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
; Core sistema / interrupciones 
global sys_hardware_init
global sys_hlt
global sys_exit
global sys_sleep
global sys_get_ticks
global sys_cli
global sys_sti

; Memoria
global sys_kalloc
global sys_get_free_mem
global sys_get_ram_size
global sys_memcpy
global sys_memset
global sys_gc_collect
global sys_mark_frame
global sys_reset_frame
%include "boot/sys_thread.asm"
global sys_switch_context

; Serie (debug JVM) 
global sys_serial_init
global sys_serial_putc
global sys_serial_puts
global sys_serial_print_java

; PCI 
global sys_pci_write_config
global sys_pci_read_config

; Entrada (por IRQ + FIFO) 
global sys_init_keyboard
global sys_read_keyboard_scancode
global sys_set_keyboard_layout
global sys_init_mouse
global sys_read_mouse

; Gráficos VBE 
global sys_set_color
global sys_draw_pixel
global sys_get_pixel
global sys_draw_pixel_alpha
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
global sys_scroll_vram

; Disco ATA IDE LBA28 
global sys_disk_read_sector
global sys_disk_write_sector

; Tiempo CMOS 
global sys_get_time

; Audio PC Speaker 
global sys_beep
global sys_nosound

; Red RTL8139 
%include "driver/network/sys_rtl8139.asm"
global sys_rtl8139_init
global sys_rtl8139_send_packet
global sys_net_receive_packet

; PCnet Driver 
%include "driver/network/sys_pcnet.asm"
global sys_pcnet_init
global sys_pcnet_send_packet
global sys_net_receive_packet_pcnet

; Red RTL8111/8168 
%include "driver/network/sys_rtl8168.asm"
global sys_rtl8168_init
global sys_rtl8168_send_packet
global sys_net_receive_packet_rtl8168

; Puertos I/O 
global sys_inb
global sys_outb
global sys_inw
global sys_outw
global sys_indw
global sys_outdw
global sys_wait_io

; Externos del Kernel/Framebuffer/GC
extern g_framebuffer
extern g_pitch
extern draw_char_vram
extern jit_flush_icache
extern java_static_vars

; SECCIÓN BSS (MEMORIA NO INICIALIZADA)
section .bss
alignb 16

sys_ticks           resd 1

heap_curr_ptr       resd 1
heap_start_ptr      resd 1
free_list_head		resd 1	; cabeza de la lisa de bloques reciclados
stack_bottom		resd 1	; saber hasta donde escanear la pila

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

; Sección TEXT (Código ejecutable)
section .text

; Inicialización de HARDWARE
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
    mov dword [heap_curr_ptr], 0x02000000
    mov dword [heap_start_ptr], 0x02000000
    mov dword [current_color], 0xFFFFFFFF
	mov dword [free_list_head], 0       
    mov [stack_bottom], ebp    ; Guardar la base inicial de la pila del kernel

    call sys_init_keyboard
    call sys_init_mouse
    call sys_sti

sys_cli:
    cli
    ret
	
sys_sti:
    sti
    ret

; Puerto serie UART 16550 (COM1 @ 0x3F8)
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

; Imprimir caracter en consola
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

; Imprimir cadena de caracteres en consola
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

; Impresión en consola serial
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


; Controlador de interrupciones PIC 8259A
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

; Temporizador PIT (1000 Hz)
sys_init_pit:
    mov al, 0x36
    out 0x43, al
    mov al, 0xA9
    out 0x40, al
    mov al, 0x04
    out 0x40, al
    ret

; IDT y Manejadores de interrupción
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

; IRQ1: Manejador de teclado PS/2
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

; Temporización y Memoria
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
    call jit_flush_icache
    pop ebx
    pop ebp
    ret

; Guarda el estado actual del Heap 
sys_mark_frame:
	mov eax, [heap_curr_ptr]
	mov [frame_heap_checkpoint], eax
	ret

; Rebobina el Heap al estado guardado (limpiar)
sys_reset_frame:
	mov eax, [frame_heap_checkpoint]	
	test eax, eax
	jz .done_rf
	mov [heap_curr_ptr], eax
	mov dword [free_list_head], 0	; Reset de lista libre
.done_rf:
	ret	

; Asignador de Memoria Kernel preparado para GC (Cabecera de 16 bytes)
sys_kalloc:
    push ebp
    mov ebp, esp
    push ebx
    push esi
    push edi
    push edx

    xor edx, edx                ; Bandera OOM (0 = GC no ejecutado)

    mov ecx, [ebp + 8]          ; Tamaño solicitado
    test ecx, ecx
    jz .fail

    ; Calcular tamaño real (Payload + 16 bytes cabecera alineados a 16)
    add ecx, 31                 
    and ecx, 0xFFFFFFF0         

.try_alloc:
    mov ebx, free_list_head
    mov esi, [ebx]              
.search_free_list:
    test esi, esi
    jz .bump_alloc

    mov eax, [esi]              ; EAX = Tamaño libre
    cmp eax, ecx
    jae .found_free_block

    lea ebx, [esi + 8]          ; Siguiente nodo
    mov esi, [ebx]
    jmp .search_free_list

.found_free_block:
    sub eax, ecx                ; EAX = Espacio sobrante
    cmp eax, 32                 ; Mínimo 32 bytes para dividir bloque
    jb .no_split

    ; SPLIT (Cortar un pedazo del bloque libre) 
    mov [esi], ecx              
    
    mov edi, esi
    add edi, ecx                ; EDI = Nuevo bloque sobrante
    mov [edi], eax              
    mov dword [edi + 4], 0      
    
    mov eax, [esi + 8]          
    mov [edi + 8], eax          
    mov [ebx], edi              
    jmp .split_done

.no_split:
    ; SIN SPLIT (Usar bloque entero) 
    mov edi, [esi + 8]
    mov [ebx], edi              
    mov ecx, [esi]              ; ECX = Tamaño TOTAL del bloque
    
.split_done:
    mov dword [esi + 4], 1      ; Marcar como asignado
    mov dword [esi + 8], 0xCAFEBABE ; <--- BLINDAJE GC: FIRMA MÁGICA
    mov eax, esi
    add eax, 16                 ; Puntero al payload
    jmp .clear_mem

.bump_alloc:
    mov eax, [heap_curr_ptr]
    mov ebx, eax
    add ebx, ecx                
    
    cmp ebx, 0x08000000         ; Límite 128 MB
    ja .trigger_gc

    mov [heap_curr_ptr], ebx
    mov [eax], ecx              
    mov dword [eax + 4], 1      
    mov dword [eax + 8], 0xCAFEBABE ; <--- BLINDAJE GC: FIRMA MÁGICA

    add eax, 16                 
    jmp .clear_mem

.clear_mem:    
    push edi
    push ecx
    push eax
    
    mov edi, eax                ; Destino = Inicio del Payload
    sub ecx, 16                 ; Restar la cabecera
    xor al, al                  ; Llenar con ceros
    cld                         
    rep stosb                   
    
    pop eax
    pop ecx
    pop edi
    jmp .done

.trigger_gc:
    test edx, edx               
    jnz .force_compaction       ; En lugar de fallar de inmediato, intentar compactar

    mov edx, 1                  ; Marca que el GC ya se ejecutó
    call sys_gc_collect
    jmp .try_alloc

.force_compaction:
    ; Si la lista libre sigue vacía tras el GC, resetear el Heap si no hay objetos vivos
    mov eax, [free_list_head]
    test eax, eax
    jnz .try_alloc              ; Si el GC liberó bloques, reintentar

.fail:
    xor eax, eax
.done:
    pop edx
    pop edi
    pop esi
    pop ebx
    pop ebp
    ret

; ====================================================================
; GC MARK & SWEEP (CON FUSIÓN DE BLOQUES ANTI-FRAGMENTACIÓN)
; ====================================================================
sys_gc_collect:
    pusha            

    ; FASE 1: Marcador (MARK)
    mov esi, java_static_vars
    mov ecx, 4096
.mark_statics:
    mov edi, [esi]              
    call gc_mark_object
    add esi, 4
    dec ecx
    jnz .mark_statics

    mov esi, esp
    mov ecx, [stack_bottom]
.mark_stack:
    cmp esi, ecx
    jae .phase2
    mov edi, [esi]
    call gc_mark_object
    add esi, 4
    jmp .mark_stack

    ; FASE 2: Barrido y Fusión (SWEEP & COALESCE)
.phase2:
    mov esi, [heap_start_ptr]
    mov dword [free_list_head], 0   

.sweep_loop:
    cmp esi, [heap_curr_ptr]
    jae .gc_done                

    mov eax, [esi]              
    mov ebx, [esi + 4]          

    test ebx, 4                 
    jnz .keep_immortal
    test ebx, 2                    
    jnz .keep_object

    ; FUSIONAR BLOQUES MUERTOS
    mov dword [esi + 4], 0      
    mov edi, esi
    add edi, eax                

.coalesce_next:
    cmp edi, [heap_curr_ptr]
    jae .link_free_block        

    mov ecx, [edi + 4]          
    test ecx, 4                 
    jnz .link_free_block        
    test ecx, 2                 
    jnz .link_free_block        

    mov edx, [edi]              
    test edx, edx               ; PROTECCIÓN: Evitar bucle si tamaño es 0
    jz .link_free_block

    add eax, edx                
    mov [esi], eax              
    add edi, edx                
    jmp .coalesce_next

.link_free_block:
    mov edx, [free_list_head]
    mov [esi + 8], edx          ; Nota: Al sobreescribir con la lista libre, el CAFEBABE se destruye (Perfecto)
    mov [free_list_head], esi   
    
    add esi, eax                
    jmp .sweep_loop

.keep_immortal:
    add esi, eax
    jmp .sweep_loop

.keep_object:
    and dword [esi + 4], 0xFFFFFFFD 
    add esi, eax
    jmp .sweep_loop

.gc_done:
    popa
    ret

; Subrutina: Marca un objeto (Aislamiento Total)
gc_mark_object:    
    pusha            
    test edi, edi
    jz .done
    test edi, 15
    jnz .done
    
    cmp edi, [heap_start_ptr]
    jb .done
    cmp edi, [heap_curr_ptr]
    jae .done

    mov eax, edi
    sub eax, 16
    
    ; --- BLINDAJE ESTRICTO CONTRA FALSOS PUNTEROS DE COORDENADAS 3D ---
    cmp dword [eax + 8], 0xCAFEBABE
    jne .done
    ; ------------------------------------------------------------------

    mov ebx, [eax + 4]
    test ebx, 1
    jz .done
    test ebx, 4        
    jnz .done
    test ebx, 2
    jnz .done

    mov edx, [eax]                
    cmp edx, 16                   
    jl .done
    
    mov ebx, eax
    add ebx, edx                
    cmp ebx, [heap_curr_ptr]     
    ja .done
    
    or dword [eax + 4], 2

    mov ecx, edx            
    sub ecx, 16             
    shr ecx, 2              
    jz .done                

    mov esi, edi            
    
.scan_fields:
    mov edi, [esi]          
    call gc_mark_object         
    add esi, 4              
    dec ecx
    jnz .scan_fields
    
.done:
    popa        
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
    
    ; Cargar el puerto 0x501 en DX primero
    mov dx, 0x0501
    mov al, 0x00
    out dx, al

.hang:
    hlt
    jmp .hang


; Bus PCI
sys_pci_write_config:
    push ebp
    mov ebp, esp
    push ebx
    push edx

    ; EAX = bus (arg_a)
    mov eax, [ebp + 8]    
    and eax, 0xFF
    shl eax, 16

    ; EBX = slot (arg_b)
    mov ebx, [ebp + 12]   
    and ebx, 0xFF
    shl ebx, 11
    or eax, ebx

    ; Función siempre 0 (Ignoramos arg_c como func para ahorrar argumentos)
    
    ; EBX = offset (arg_c)
    mov ebx, [ebp + 16]   
    and ebx, 0xFC
    or eax, ebx

    or eax, 0x80000000    ; Habilitar Bit 31 (Enable)

    ; Apuntar al registro CONFIG_ADDRESS
    mov dx, 0xCF8
    out dx, eax

    ; Escribir el valor en CONFIG_DATA
    mov eax, [ebp + 20]   ; value (arg_d)
    mov dx, 0xCFC
    out dx, eax

    pop edx
    pop ebx
    pop ebp
    ret

sys_pci_read_config:
    push ebp
    mov ebp, esp
    push ebx
    push edx

    mov eax, [ebp + 8]          ; bus
    and eax, 0xFF
    shl eax, 16

    mov ebx, [ebp + 12]         ; slot
    and ebx, 0xFF
    shl ebx, 11
    or eax, ebx

    mov ebx, [ebp + 16]         ; func
    and ebx, 0xFF
    shl ebx, 8
    or eax, ebx

    mov ebx, [ebp + 20]         ; offset
    and ebx, 0xFC
    or eax, ebx

    or eax, 0x80000000          ; Habilitar Bit 31

    mov dx, 0xCF8               ; Escribir dirección en CONFIG_ADDRESS
    out dx, eax

    mov dx, 0xCFC               ; Leer resultado en CONFIG_DATA
    in eax, dx

    pop edx                     ; Restaurar registros
    pop ebx
    pop ebp
    ret


; Driver de Teclado y Mouse
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

; Lectura de FIFO con soporte de Shift
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
    
	cmp byte [kbd_shift_state], 0	; presionó Shift?
	jne .use_shift
	
    mov al, [kbd_ascii_map_normal + eax]
    jmp .finish_map
	
.use_shift:
	mov al, [kbd_ascii_map_shift + eax]	

.finish_map:
	movzx eax, al
	jmp .done

.empty:    
	xor eax, eax
	
.done:
    pop edx
    pop ecx
    pop ebx
    ret

sys_init_mouse:
    push eax
    
    ; Habilitar dispositivo auxiliar en el PS/2
    mov al, 0xA8
    out 0x64, al
    call .wait_write

    ; Habilitar IRQ12 en el Command Configuration Byte (CCB)
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

    ; Habilitar el reporte de datos hacia el mouse
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


; Renderizador y driver gráfico VBE VESA
sys_set_color:
    push ebp
    mov ebp, esp
    mov eax, [ebp + 8]
    ;or eax, 0xFF000000          ; Forzar canal Alpha opaco (24bpp / 32bpp)
    mov [current_color], eax
    pop ebp
    ret

sys_draw_pixel:
    push ebp
    mov ebp, esp
    mov eax, [ebp + 8]          ; x
    mov ecx, [ebp + 12]         ; y
    mov edx, [current_color]	; color
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
    mov eax, [ebp + 8]		; x
    mov ecx, [ebp + 12]		; y
    imul ecx, [g_pitch]
    shl eax, 2
    add ecx, eax
    mov eax, [g_framebuffer] 
    add eax, ecx
    mov eax, [eax]
    pop ebp
    ret

sys_draw_pixel_alpha:
    push ebp
    mov ebp, esp
    pusha

    mov esi, [current_color]    ; ESI = 0xAARRGGBB
    mov edx, esi
    shr edx, 24                 ; EDX = Alpha (0 - 255)

    cmp edx, 255
    je .draw_solid              ; Opaco: Escritura rápida
    test edx, edx
    jz .done                    ; Transparente: No hacer nada

    ; Calcular offset en el framebuffer (y * pitch + x * 4)
    mov eax, [ebp + 8]          ; X
    mov ecx, [ebp + 12]         ; Y
    imul ecx, [g_pitch]
    shl eax, 2
    add ecx, eax
	
    mov edi, [g_framebuffer]
    add edi, ecx                ; EDI = Dirección del píxel destino

    mov ebx, [edi]              ; EBX = Color de fondo (0x00RRGGBB)
    
    mov eax, 255
    sub eax, edx                ; EAX = InvAlpha (255 - Alpha)

    ; Procesar Rojo y Azul simultáneamente
    push eax                    ; Guardar InvAlpha para el canal Verde    
    mov ecx, ebx
    and ecx, 0x00FF00FF         ; ECX = Destino R_B
    imul ecx, eax               ; Destino R_B * InvAlpha

    mov eax, esi
    and eax, 0x00FF00FF         ; EAX = Origen R_B
    imul eax, edx               ; Origen R_B * Alpha

    add ecx, eax                ; Sumar origen y destino
    shr ecx, 8                  ; Dividir entre 256 (aproximación rápida a 255)
    and ecx, 0x00FF00FF         ; Limpiar basura, ECX = Final R_B

    ; Procesar Verde
    pop eax                     ; EAX = InvAlpha    
    push ecx                    ; Guardar R_B procesado
    
    mov ecx, ebx
    and ecx, 0x0000FF00         ; ECX = Destino G
    imul ecx, eax               ; Destino G * InvAlpha

    mov eax, esi
    and eax, 0x0000FF00         ; EAX = Origen G
    imul eax, edx               ; Origen G * Alpha

    add ecx, eax
    shr ecx, 8                  ; Dividir entre 256
    and ecx, 0x0000FF00         ; ECX = Final G

    pop eax                     ; Recuperar R_B
    or eax, ecx                 ; Combinar Canales (Final RGB)
    mov [edi], eax              ; Escribir píxel mezclado
    jmp .done

.draw_solid:
    ; Ruta rápida para alpha 255
    mov eax, [ebp + 8]
    mov ecx, [ebp + 12]
    imul ecx, [g_pitch]
    shl eax, 2
    add ecx, eax
	
	mov edi, [g_framebuffer]
    add edi, ecx
    mov [edi], esi

.done:
    popa
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
    mov esi, [current_color]	; color
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
sys_draw_arc:
    jmp sys_draw_oval
    
sys_fill_arc:
    jmp sys_fill_oval

sys_draw_oval:
    push ebp
    mov ebp, esp
    sub esp, 28             ; Espacio local
    pusha
    
    ; Similar a fill_oval, pero evaluamos un anillo entre 0.85 y 1.0
    mov eax, [ebp+16]
    shr eax, 1
    mov [ebp-4], eax
    
    mov eax, [ebp+20]
    shr eax, 1
    mov [ebp-8], eax
    
    cmp dword [ebp-4], 0
    je .draw_o_done
    cmp dword [ebp-8], 0
    je .draw_o_done

    mov eax, [ebp+8]
    add eax, [ebp-4]
    mov [ebp-12], eax
    
    mov eax, [ebp+12]
    add eax, [ebp-8]
    mov [ebp-16], eax
    
    mov ecx, [ebp+12]       
.y_loop:
    mov eax, [ebp+12]
    add eax, [ebp+20]
    cmp ecx, eax            
    jge .draw_o_done
    
    mov ebx, [ebp+8]        
.x_loop:
    mov eax, [ebp+8]
    add eax, [ebp+16]
    cmp ebx, eax            
    jge .x_end
    
    mov eax, ebx
    sub eax, [ebp-12]
    mov [ebp-20], eax
    
    mov eax, ecx
    sub eax, [ebp-16]
    mov [ebp-24], eax
    
    fild dword [ebp-20]
    fild dword [ebp-4]
    fdivp st1, st0
    fmul st0, st0
    
    fild dword [ebp-24]
    fild dword [ebp-8]
    fdivp st1, st0
    fmul st0, st0
    
    faddp st1, st0          ; Suma actual
    
    ; Umbral inferior (aprox 0.85). Valor en float (32-bit IEEE) = 0x3F59999A
    mov dword [ebp-28], 0x3F59999A
    fld dword [ebp-28]      ; st0 = 0.85, st1 = Suma
    fcomp st1               ; Comparar 0.85 y Suma
    fnstsw ax
    sahf
    ja .cleanup_st0         ; Si Suma < 0.85, limpiar st0 y saltar

    fld1                    ; st0 = 1.0, st1 = Suma
    fcomp st1               ; Comparar 1.0 y Suma
    fnstsw ax
    sahf
    jb .cleanup_st0         ; Si Suma > 1.0, limpiar st0 y saltar
    
    ; Si está en el anillo, limpiar suma y dibujar
    fstp st0
    
    ; BUCLE
    push ecx                ; RESPALDAR ECX (Contador Y)
    push ecx                ; Empujar Y como argumento para sys_draw_pixel
    push ebx                ; Empujar X como argumento para sys_draw_pixel
    call sys_draw_pixel
    add esp, 8              ; Limpiar argumentos
    pop ecx                 ; RESTAURAR ECX (Contador Y) intacto
    
    
    jmp .skip_pixel
    
.cleanup_st0:
    fstp st0                ; Limpiar la suma descartada
.skip_pixel:
    inc ebx
    jmp .x_loop
.x_end:
    inc ecx
    jmp .y_loop
    
.draw_o_done:
    popa
    mov esp, ebp
    pop ebp
    ret    

sys_fill_oval:
    push ebp
    mov ebp, esp
    sub esp, 28             ; Espacio local para cálculos FPU
    pusha
    
    ; Variables locales: [ebp-4]=rx, [ebp-8]=ry, [ebp-12]=xc, [ebp-16]=yc, [ebp-20]=dx, [ebp-24]=dy
    mov eax, [ebp+16]       ; w
    shr eax, 1              ; rx = w/2
    mov [ebp-4], eax
    
    mov eax, [ebp+20]       ; h
    shr eax, 1              ; ry = h/2
    mov [ebp-8], eax
    
    ; Protección división por cero
    cmp dword [ebp-4], 0
    je .fill_o_done
    cmp dword [ebp-8], 0
    je .fill_o_done

    mov eax, [ebp+8]
    add eax, [ebp-4]        ; xc = x + rx
    mov [ebp-12], eax
    
    mov eax, [ebp+12]
    add eax, [ebp-8]        ; yc = y + ry
    mov [ebp-16], eax
    
    ; Y Loop
    mov ecx, [ebp+12]       ; py = y
.y_loop:
    mov eax, [ebp+12]
    add eax, [ebp+20]
    cmp ecx, eax            ; py < y+h ?
    jge .fill_o_done
    
    ; X Loop
    mov ebx, [ebp+8]        ; px = x
.x_loop:
    mov eax, [ebp+8]
    add eax, [ebp+16]
    cmp ebx, eax            ; px < x+w ?
    jge .x_end
    
    ; Evaluar ((px-xc)/rx)^2 + ((py-yc)/ry)^2 <= 1.0 mediante FPU
    mov eax, ebx
    sub eax, [ebp-12]
    mov [ebp-20], eax       ; dx
    
    mov eax, ecx
    sub eax, [ebp-16]
    mov [ebp-24], eax       ; dy
    
    fild dword [ebp-20]     ; Cargar dx
    fild dword [ebp-4]      ; Cargar rx
    fdivp st1, st0          ; (dx/rx)
    fmul st0, st0           ; (dx/rx)^2
    
    fild dword [ebp-24]     ; Cargar dy
    fild dword [ebp-8]      ; Cargar ry
    fdivp st1, st0          ; (dy/ry)
    fmul st0, st0           ; (dy/ry)^2
    
    faddp st1, st0          ; Suma
    
    fld1                    ; Cargar 1.0
    fcompp                  ; Comparar 1.0 con la Suma (Saca ambos de la pila)
    fnstsw ax               ; Extraer status FPU
    sahf
    jb .skip_pixel          ; Si 1.0 < suma, está fuera del óvalo
    
    ; BUCLE
    push ecx                ; RESPALDAR ECX (Contador Y)
    push ecx                ; Empujar Y como argumento para sys_draw_pixel
    push ebx                ; Empujar X como argumento para sys_draw_pixel
    call sys_draw_pixel
    add esp, 8              ; Limpiar argumentos
    pop ecx                 ; RESTAURAR ECX (Contador Y) intacto
    
    
.skip_pixel:
    inc ebx
    jmp .x_loop
.x_end:
    inc ecx
    jmp .y_loop
    
.fill_o_done:
    popa
    mov esp, ebp
    pop ebp
    ret

sys_draw_polygon:
    push ebp
    mov ebp, esp
    pusha
    
    mov esi, [ebp + 8]      ; xPoints (Puntero al array Java)
    mov edi, [ebp + 12]     ; yPoints (Puntero al array Java)
    mov ecx, [ebp + 16]     ; nPoints
    
    cmp ecx, 2
    jl .poly_done           ; Mínimo 2 puntos
    
    add esi, 4              ; Saltar cabecera 'length' de Java
    add edi, 4				; Apuntar a yPoints[0]
    
    xor ebx, ebx            ; Índice i = 0
.poly_loop:
    mov eax, ebx
    inc eax                 ; j = i + 1
    cmp eax, ecx
    jne .no_wrap
    xor eax, eax            ; j = 0 (Cerrar el polígono)
.no_wrap:
    push ecx                ; Preservar nPoints
    
    ; Empujar argumentos para sys_draw_line (y2, x2, y1, x1)
    push dword [edi + eax*4]
    push dword [esi + eax*4]
    push dword [edi + ebx*4]
    push dword [esi + ebx*4]
    call sys_draw_line
    add esp, 16
    
    pop ecx
    inc ebx
    cmp ebx, ecx
    jl .poly_loop
    
.poly_done:
    popa
    pop ebp
    ret

sys_fill_polygon:
    push ebp
    mov ebp, esp
    pusha
    
    mov esi, [ebp + 8]      ; xPoints
    mov edi, [ebp + 12]     ; yPoints
    mov ecx, [ebp + 16]     ; nPoints
    
    cmp ecx, 3
    jl .fill_poly_done
    je .is_triangle
    
    ; Convex-Fan dividido en triángulos puros para nPoints > 3
    add esi, 4              ; Saltar la cabecera/length
    add edi, 4              ; Apuntar al índice 0
    mov ebx, 1              
.fan_loop:
    mov eax, ecx
    dec eax
    cmp ebx, eax
    jge .fill_poly_done

    push dword [edi + ebx*4 + 4] ; y2
    push dword [esi + ebx*4 + 4] ; x2
    push dword [edi + ebx*4]     ; y1
    push dword [esi + ebx*4]     ; x1
    push dword [edi]             ; y0
    push dword [esi]             ; x0
    call internal_fill_triangle
    add esp, 24

    inc ebx
    jmp .fan_loop

.is_triangle:
    add esi, 4
    add edi, 4
    push dword [edi + 8]    ; y2
    push dword [esi + 8]    ; x2
    push dword [edi + 4]    ; y1
    push dword [esi + 4]    ; x1
    push dword [edi]        ; y0
    push dword [esi]        ; x0
    call internal_fill_triangle
    add esp, 24

.fill_poly_done:
    popa
    pop ebp
    ret

; Subrutina interna de rasterización (Scanline Triangle Fill)
internal_fill_triangle:
    push ebp
    mov ebp, esp
    sub esp, 24             ; Espacio para x0, y0, x1, y1, x2, y2
    pusha

    ; Variables locales
    mov eax, [ebp+8]
    mov [ebp-4], eax        ; x0
    mov eax, [ebp+12]
    mov [ebp-8], eax        ; y0
    mov eax, [ebp+16]
    mov [ebp-12], eax       ; x1
    mov eax, [ebp+20]
    mov [ebp-16], eax       ; y1
    mov eax, [ebp+24]
    mov [ebp-20], eax       ; x2
    mov eax, [ebp+28]
    mov [ebp-24], eax       ; y2

    ; Ordenar vértices por Y (y0 <= y1 <= y2)
    mov eax, [ebp-8]
    cmp eax, [ebp-16]
    jle .sort1
    mov ebx, [ebp-4]
    mov ecx, [ebp-12]
    mov [ebp-4], ecx
    mov [ebp-12], ebx
    mov ebx, [ebp-8]
    mov ecx, [ebp-16]
    mov [ebp-8], ecx
    mov [ebp-16], ebx
.sort1:
    mov eax, [ebp-8]
    cmp eax, [ebp-24]
    jle .sort2
    mov ebx, [ebp-4]
    mov ecx, [ebp-20]
    mov [ebp-4], ecx
    mov [ebp-20], ebx
    mov ebx, [ebp-8]
    mov ecx, [ebp-24]
    mov [ebp-8], ecx
    mov [ebp-24], ebx
.sort2:
    mov eax, [ebp-16]
    cmp eax, [ebp-24]
    jle .sort3
    mov ebx, [ebp-12]
    mov ecx, [ebp-20]
    mov [ebp-12], ecx
    mov [ebp-20], ebx
    mov ebx, [ebp-16]
    mov ecx, [ebp-24]
    mov [ebp-16], ecx
    mov [ebp-24], ebx
.sort3:
    mov eax, [ebp-8]
    cmp eax, [ebp-24]
    je .done_tri            ; Triángulo sin área (y0 == y2)

    ; Rasterizar mitad superior (y0 a y1)
    mov ebx, [ebp-8]        ; y = y0
.top_loop:
    cmp ebx, [ebp-16]
    jge .bot_half

    ; xA = x0 + (x1 - x0) * (y - y0) / (y1 - y0)
    mov eax, [ebp-12]
    sub eax, [ebp-4]
    mov ecx, ebx
    sub ecx, [ebp-8]
    imul ecx
    mov ecx, [ebp-16]
    sub ecx, [ebp-8]
    cdq
    idiv ecx
    add eax, [ebp-4]
    mov esi, eax            ; xA

    ; xB = x0 + (x2 - x0) * (y - y0) / (y2 - y0)
    mov eax, [ebp-20]
    sub eax, [ebp-4]
    mov ecx, ebx
    sub ecx, [ebp-8]
    imul ecx
    mov ecx, [ebp-24]
    sub ecx, [ebp-8]
    cdq
    idiv ecx
    add eax, [ebp-4]
    mov edi, eax            ; xB

    cmp esi, edi
    jle .top_draw
    xchg esi, edi
.top_draw:
    mov ecx, edi
    sub ecx, esi
    inc ecx                 ; width

    push 1
    push ecx
    push ebx
    push esi
    call sys_fill_rect
    add esp, 16

    inc ebx
    jmp .top_loop

.bot_half:
    ; Rasterizar mitad inferior (y1 a y2)
    mov ebx, [ebp-16]       ; y = y1
.bot_loop:
    cmp ebx, [ebp-24]
    jg .done_tri

    ; xA = x1 + (x2 - x1) * (y - y1) / (y2 - y1)
    mov ecx, [ebp-24]
    sub ecx, [ebp-16]
    je .calc_xB_bot         ; Evitar / 0

    mov eax, [ebp-20]
    sub eax, [ebp-12]
    mov edx, ebx
    sub edx, [ebp-16]
    imul edx
    cdq
    idiv ecx
    add eax, [ebp-12]
    mov esi, eax            ; xA
    jmp .do_xB_bot

.calc_xB_bot:
    mov esi, [ebp-12]

.do_xB_bot:
    ; xB = x0 + (x2 - x0) * (y - y0) / (y2 - y0)
    mov ecx, [ebp-24]
    sub ecx, [ebp-8]
    je .calc_xB_bot_zero

    mov eax, [ebp-20]
    sub eax, [ebp-4]
    mov edx, ebx
    sub edx, [ebp-8]
    imul edx
    cdq
    idiv ecx
    add eax, [ebp-4]
    mov edi, eax            ; xB
    jmp .order_bot

.calc_xB_bot_zero:
    mov edi, [ebp-4]

.order_bot:
    cmp esi, edi
    jle .bot_draw
    xchg esi, edi
.bot_draw:
    mov ecx, edi
    sub ecx, esi
    inc ecx                 ; width

    push 1
    push ecx
    push ebx
    push esi
    call sys_fill_rect
    add esp, 16

    inc ebx
    jmp .bot_loop

.done_tri:
    popa
    mov esp, ebp
    pop ebp
    ret

; Impresión de cadenas de texto
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
	; Decodificador UTF-8
	cmp al, 0xC3
	je .utf8_c3
	cmp al, 0xC2
	je .utf8_c2
	
.process_char:	
    cmp al, 13
    je .skip_char
    cmp al, 10
    je .skip_char
    cmp al, 32
    jb .skip_char		; Compara de 0 a 255 (sin signo)
    
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
	jmp .done

.utf8_c3:	
	inc esi						; avanzar al segundo byte del UTF-8
	dec ecx						; restar longitud total
	jz .done					; evitar cuelgue si la cadena se corta
	mov al, [esi]				; Leer el segundo byte (0xA1 para 'á' por ej.)
	add al, 64					; 0xA1 + 64 = 0xE1 (índice en font.asm)
	jmp .process_char

.utf8_c2:
    inc esi
    dec ecx
    jz .done
    mov al, [esi]               ; El segundo byte ya es el ASCII correcto
    jmp .process_char	

.done:
    popa
    pop ebp
    ret

; Desplazamiento de pantalla (SCROLL)
sys_scroll_vram:
    push ebp
    mov ebp, esp
    pusha

    ; [ebp + 8] = Cantidad de píxeles a desplazar hacia arriba (ej. 25)
    mov eax, [ebp + 8]
    imul eax, [g_pitch]         ; eax = offset en bytes a desplazar

    mov edi, [g_framebuffer]    ; Destino: Inicio de la pantalla
    mov esi, [g_framebuffer]
    add esi, eax                ; Origen: Pantalla desplazada

    ; Calcular cuántos dwords (4 bytes) mover: ((768 * pitch) - offset) / 4
    mov ecx, 768
    imul ecx, [g_pitch]
    sub ecx, eax
    shr ecx, 2                  ; Dividir entre 4 para 'rep movsd'

    cld                         ; Dirección de copia hacia adelante
    rep movsd                   ; Copiar la memoria de video hacia arriba

    ; Limpiar la franja inferior con color negro
    ; EDI ya quedó apuntando a la zona libre al finalizar el rep movsd
    mov ecx, eax
    shr ecx, 2                  ; Convertir offset a dwords
    xor eax, eax                ; Color Negro (0x00000000)
    rep stosd                   ; Rellenar

    popa
    pop ebp
    ret

; CMOS Reloj real (RTC)
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


; Bocinas de PC y Buzzer
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

; Disco ATA IDE LBA28 (TIMEOUT Y RETARDO 400ns)
; Subrutina: Esperar a que el disco se libere (BSY = 0)
ata_wait_bsy:
    push ecx
    push edx
    mov dx, 0x3F6
    in al, dx
    in al, dx
    in al, dx
    in al, dx
    mov ecx, 100000         ; Timeout de seguridad
    mov dx, 0x1F7
.poll_bsy:
    in al, dx
    test al, 0x80           
    jz .ready               
    dec ecx
    jnz .poll_bsy
    stc                     ; Activa CF por timeout
    jmp .done
.ready:
    clc                     
.done:
    pop edx
    pop ecx
    ret

; Subrutina: Esperar a que el disco pida datos (DRQ = 1)
ata_wait_drq:
    push ecx
    push edx
    mov dx, 0x3F6
    in al, dx
    in al, dx
    in al, dx
    in al, dx
    mov ecx, 100000         
    mov dx, 0x1F7
.poll_drq:
    in al, dx
    test al, 0x80           
    jnz .retry
    test al, 0x08           
    jnz .ready
    test al, 0x01           
    jnz .error
.retry:
    dec ecx
    jnz .poll_drq
.error:
    stc                     
    jmp .done
.ready:
    clc
.done:
    pop edx
    pop ecx
    ret

; Leer sector
sys_disk_read_sector:
    push ebp
    mov ebp, esp
    push ebx
    push edi

    mov edi, [ebp + 12]         
    cmp edi, 0                  
    jne .skip_default_r
    mov edi, disk_sector_buf    
	jmp .read_ready
	
.skip_default_r:  
    add edi, 4					; protección GC
	
.read_ready:	
    call ata_wait_bsy           
    jc .disk_error

    mov eax, [ebp + 8]          ; LBA (Nota: No sobrescribir EDI aquí)

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

    call ata_wait_drq           
    jc .disk_error

    mov ecx, 256
    mov dx, 0x1F0
.read:
    in ax, dx
    mov [edi], ax
    add edi, 2
    loop .read

    mov eax, 1                  
    jmp .done

.disk_error:
    xor eax, eax                

.done:
    pop edi
    pop ebx
    pop ebp
    ret

; Escribir sector
sys_disk_write_sector:
    push ebp
    mov ebp, esp
    push ebx
    push esi
    
    mov esi, [ebp + 12]         
    cmp esi, 0                  
    jne .skip_default_w
    mov esi, disk_sector_buf
	jmp .write_ready    

.skip_default_w:
	add esi, 4					; protección GC
	
.write_ready:
    call ata_wait_bsy           
    jc .disk_error

    mov eax, [ebp + 8]          ; LBA (Nota: No sobrescribir ESI aquí)

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

    call ata_wait_drq           
    jc .disk_error

    mov ecx, 256
    mov dx, 0x1F0
.write:
    mov ax, [esi]
    out dx, ax
    add esi, 2
    loop .write
    
    mov dx, 0x1F7
    mov al, 0xE7                
    out dx, al

    call ata_wait_bsy           
    jc .disk_error

    mov eax, 1                  
    jmp .done

.disk_error:
    xor eax, eax                

.done:
    pop esi
    pop ebx
    pop ebp
    ret


; Puertos dedicados I/O
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

; Sección DATA y RODATA
section .data
align 16

frame_heap_checkpoint dd 0

idtr:
    idtr_limit      dw 2047
    idtr_base       dd idt_entries
align 16
idt_entries:        times 256 * 8 db 0

section .rodata
align 4

; Mapa de teclado LATAM Normal (Minúsculas y números)
kbd_ascii_map_normal:
    ; 0x00 - 0x0F (191 = ¿)
    db 0, 27, '1', '2', '3', '4', '5', '6', '7', '8', '9', '0', 39, 191, 8, 9
    ; 0x10 - 0x1F (180 = ´)
    db 'q', 'w', 'e', 'r', 't', 'y', 'u', 'i', 'o', 'p', 180, '+', 13, 0, 'a', 's'
    ; 0x20 - 0x2F (241 = ñ)
    db 'd', 'f', 'g', 'h', 'j', 'k', 'l', 241, '{', '|', 0, '}', 'z', 'x', 'c', 'v'    
    ; 0x30 - 0x3F 
    db 'b', 'n', 'm', ',', '.', '-', 0, '*', 0, 32, 0, 0, 0, 0, 0, 0
    ; 0x40 - 0x5F (Scancode 0x56 = <)
    db 0, 0, 0, 0, 0, 0, 0, '7', '8', '9', '-', '4', '5', '6', '+', '1'
    db '2', '3', '0', '.', 0, 0, '<', 0, 0, 0, 0, 0, 0, 0, 0, 0
    times 32 db 0

; Mapa de teclado LATAM Shifted (Mayúsculas y Símbolos)
kbd_ascii_map_shift:
    ; 0x00 - 0x0F (161 = ¡)
    db 0, 27, '!', '"', '#', '$', '%', '&', '/', '(', ')', '=', '?', 161, 8, 9
    ; 0x10 - 0x1F (168 = ¨)
    db 'Q', 'W', 'E', 'R', 'T', 'Y', 'U', 'I', 'O', 'P', 168, '*', 13, 0, 'A', 'S'
    ; 0x20 - 0x2F (209 = Ñ, 176 = °)
    db 'D', 'F', 'G', 'H', 'J', 'K', 'L', 209, '[', 176, 0, ']', 'Z', 'X', 'C', 'V'    
    ; 0x30 - 0x3F 
    db 'B', 'N', 'M', ';', ':', '_', 0, '*', 0, 32, 0, 0, 0, 0, 0, 0
    ; 0x40 - 0x5F (Scancode 0x56 = >)
    db 0, 0, 0, 0, 0, 0, 0, '7', '8', '9', '-', '4', '5', '6', '+', '1'
    db '2', '3', '0', '.', 0, 0, '>', 0, 0, 0, 0, 0, 0, 0, 0, 0
    times 32 db 0

kbd_ascii_map_old:
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
