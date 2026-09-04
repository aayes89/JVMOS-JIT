;MIT License
;
;Copyright (c) 2026 Allan (Slam)
;
;Permission is hereby granted, free of charge, to any person obtaining a copy
;of this software and associated documentation files (the "Software"), to deal
;in the Software without restriction, including without limitation the rights
;to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
;copies of the Software, and to permit persons to whom the Software is
;furnished to do so, subject to the following conditions:
;
;The above copyright notice and this permission notice shall be included in all
;copies or substantial portions of the Software.
;
;THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
;IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
;FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
;AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
;LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
;OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
;SOFTWARE.

; ============================================================================
; JVMOS - JavaMonolitic JIT Engine (Pure Translation Unit)
; Formato: NASM x86 32-bit (Modo Protegido Bare-Metal)
; ============================================================================

[bits 32]

section .bss

align 16
    jit_buffer_base: resd 1    	; base física de memoria JIT (0x00200000)
    jit_buffer_ptr:  resd 1    	; cursor actual
    jit_buffer_end:  resd 1    	; límite de memoria
	
	align 4
	pc_map:			 resd 65536	; hasta 64kb de bytecode por método
	fixup_addr:		 resd 1024	; donde sobreescribir métodos nativos
	fixup_target: 	 resd 1024  ; a cual PC del bytecode apuntaba
    fixup_count:  	 resd 1     ; contador de saltos hacia adelante
	method_cache: 	 resd 2048	; mapea [CP_Index] -> [Dirección Nativa]
	java_static_vars: resd 4096 ; para manejar variables estáticas

section .data
    jit_bytecode_base: dd 0
    loop_start_addr:   dd 0
	heap_ptr:		   dd 0x01000000 ; 16MB de RAM física
	
	; Bytecodes de prueba para fases JIT 2 a 6
    test_bytecode_p2: db 0x10, 0x42, 0xAC
    test_bytecode_p3: db 0x10, 0x0A, 0x3B, 0x10, 0x14, 0x3C, 0x1B, 0xAC
    test_bytecode_p4: db 0x10, 0x0A, 0x3B, 0x10, 0x14, 0x3C, 0x1A, 0x1B, 0x60, 0xAC
    test_bytecode_p5: db 0x1A, 0x1B, 0x60, 0xAC
    test_bytecode_p6: db 0x03, 0x3B, 0x1A, 0x10, 0x05, 0x9F, 0x00, 0x0A, 0x1A, 0x04, 0x60, 0x3B, 0xA7, 0xFF, 0xF6, 0x1A, 0xAC

section .text
    global jit_init
    global jit_compile_method
    global jit_execute_method
	global jit_runtime_trampoline
	global jit_emit_byte
	global jit_emit_dword
	global jit_buffer_ptr

	global sys_native_dispatch
	extern resolve_and_compile_java_method
	extern sys_kalloc
	extern cp_base_ptr
	extern cp_offsets
	extern current_param_count
	extern current_class_ptr

	extern sys_arg_id, sys_arg_a, sys_arg_b, sys_arg_c, sys_arg_d			
	extern draw_char_vram, sys_draw_string, sys_serial_puts, sys_serial_putc, sys_serial_print_java
	extern current_color
	extern sys_read_keyboard_scancode, sys_set_keyboard_layout, sys_read_mouse
	extern sys_draw_rect, sys_fill_rect, sys_draw_line, sys_get_pixel, sys_draw_pixel
	extern sys_beep, sys_nosound, sys_get_free_mem, sys_get_ram_size
	extern sys_pci_read_config, sys_disk_read_sector, sys_disk_write_sector
	extern sys_rtl8139_init, sys_rtl8139_send_packet, sys_net_receive_packet
	extern sys_inb, sys_outb, sys_inw, sys_outw, sys_indw, sys_outdw, sys_get_ticks
	extern sys_get_time, sys_sleep, sys_exit

jit_init:
    push eax
    push ebx
    mov [jit_buffer_base], eax
    mov [jit_buffer_ptr], eax
    add eax, ebx
    mov [jit_buffer_end], eax
    pop ebx
    pop eax
    ret

jit_emit_byte:
    push edi
    mov edi, [jit_buffer_ptr]
    cmp edi, [jit_buffer_end]
    jae .overflow
    mov [edi], al
    inc edi
    mov [jit_buffer_ptr], edi
    pop edi
    ret
.overflow:
    pop edi
    cli
    hlt

jit_emit_dword:
    push edi
    push ecx
    mov edi, [jit_buffer_ptr]
    lea ecx, [edi + 4]
    cmp ecx, [jit_buffer_end]
    jae .overflow
    mov [edi], eax
    mov [jit_buffer_ptr], ecx
    pop ecx
    pop edi
    ret
.overflow:
    pop ecx
    pop edi
    cli
    hlt

; Preserva registros, invoca a resolve_and_compile_java_method en bootjvm y salta.
jit_runtime_trampoline:    
    pusha
    call resolve_and_compile_java_method
    test eax, eax
    jz .resolve_failed
    mov [esp + 28], eax         ; Poner dirección nativa en el EAX guardado de pusha

    ; Parche del call site
    ; [esp + 32] contiene la dirección de retorno (la instrucción justo después del CALL)
    mov ebx, [esp + 32]         
    mov ecx, eax                ; ECX = Dirección del método nativo recién compilado
    sub ecx, ebx                ; ECX = Destino - Retorno (Offset relativo rel32)
    mov [ebx - 4], ecx          ; Sobrescribir el offset rel32 del CALL original en la memoria JIT
    popa
    jmp eax                     ; Salta a la dirección nativa que acaba de devolver EAX

.resolve_failed:
    popa
    cli
    hlt

jit_emit_prologue:
    mov al, 0x55                    ; push ebp
    call jit_emit_byte
    mov al, 0x89                    ; mov ebp, esp
    call jit_emit_byte
    mov al, 0xE5
    call jit_emit_byte
    mov al, 0x53                    ; push ebx
    call jit_emit_byte
    mov al, 0x56                    ; push esi
    call jit_emit_byte
    mov al, 0x57                    ; push edi
    call jit_emit_byte

    ; Ampliar marco de pila a 1024 bytes
    mov al, 0x81                    ; sub esp, 1024
    call jit_emit_byte
    mov al, 0xEC
    call jit_emit_byte
    mov eax, 1024
    call jit_emit_dword

    mov ecx, [current_param_count]
    test ecx, ecx
    jz .no_params
    cmp ecx, 256
    ja .no_params

    mov al, 0xB9                    ; mov ecx, imm32
    call jit_emit_byte
    mov eax, ecx
    call jit_emit_dword

    mov al, 0x8D                    ; lea esi,[ebp+8]
    call jit_emit_byte
    mov al, 0x75
    call jit_emit_byte
    mov al, 0x08
    call jit_emit_byte
    
    mov eax, ecx
    dec eax
    shl eax, 2
    mov al, 0x81                    ; add esi, imm32
    call jit_emit_byte
    mov al, 0xC6
    call jit_emit_byte

    mov eax, ecx
    dec eax
    shl eax, 2
    call jit_emit_dword

    mov al, 0x8D                    ; lea edi,[ebp-16]
    call jit_emit_byte
    mov al, 0x7D
    call jit_emit_byte
    mov al, 0xF0
    call jit_emit_byte

.copy_loop:
    mov al, 0x8B
    call jit_emit_byte
    mov al, 0x06
    call jit_emit_byte
    mov al, 0x89
    call jit_emit_byte
    mov al, 0x07
    call jit_emit_byte
    mov al, 0x83
    call jit_emit_byte
    mov al, 0xEE
    call jit_emit_byte
    mov al, 0x04
    call jit_emit_byte
    mov al, 0x83
    call jit_emit_byte
    mov al, 0xEF
    call jit_emit_byte
    mov al, 0x04
    call jit_emit_byte
    mov al, 0x49
    call jit_emit_byte
    mov al, 0x75
    call jit_emit_byte
    mov al, 0xF3
    call jit_emit_byte
.no_params:
    ret

jit_emit_epilogue:
    mov al, 0x8D                ; lea esp, [ebp - 12]
    call jit_emit_byte
    mov al, 0x65
    call jit_emit_byte
    mov al, 0xF4
    call jit_emit_byte

    mov al, 0x5F                ; pop edi
    call jit_emit_byte
    mov al, 0x5E                ; pop esi
    call jit_emit_byte
    mov al, 0x5B                ; pop ebx
    call jit_emit_byte
    mov al, 0x5D                ; pop ebp
    call jit_emit_byte

    ; Emitir retorno con limpieza de argumentos (stdcall)
    mov eax, [current_param_count]
    test eax, eax
    jz .ret_normal
    
    mov al, 0xC2                ; ret imm16
    call jit_emit_byte
    
    mov eax, [current_param_count]
    shl eax, 2                  ; * 4 bytes por parámetro
    
    push eax
    call jit_emit_byte          ; byte bajo
    pop eax
    shr eax, 8
    call jit_emit_byte          ; byte alto
    ret
    
.ret_normal:
    mov al, 0xC3                ; ret
    call jit_emit_byte
    ret

sys_native_dispatch:
    push ebp
    mov ebp, esp
    push ebx
    push ecx
    push edx

    mov eax, [sys_arg_id]

    cmp eax, 0
    je .sys_kalloc
    cmp eax, 1
    je .sys_set_color
    cmp eax, 2
    je .sys_fill_rect
    cmp eax, 3
    je .sys_draw_rect
    cmp eax, 4
    je .sys_draw_line
    cmp eax, 5
    je .sys_draw_string
    cmp eax, 6
    je .sys_read_keyboard
    cmp eax, 7
    je .sys_read_mouse
	cmp eax, 8
	je .sys_disk_read
	cmp eax, 9
	je .sys_disk_write   	
	cmp eax, 10
	je .sys_inb
	cmp eax, 11
    je .sys_outb
    cmp eax, 12
    je .sys_sleep
    cmp eax, 13
    je .sys_get_time
	cmp eax, 14
    je .sys_get_pixel
    cmp eax, 15
    je .sys_draw_char
    cmp eax, 16
    je .sys_set_kbd_layout
    cmp eax, 17
    je .sys_exit
    cmp eax, 18
    je .sys_get_ticks
	cmp eax, 19
	je .sys_serial_putc
	cmp eax, 20
	je .sys_serial_puts
	cmp eax, 21
    je .sys_pci_read
    cmp eax, 22
    je .sys_beep
	cmp eax, 23
    je .sys_rtl8139_init
    cmp eax, 24
    je .sys_rtl8139_send_packet
    cmp eax, 25
    je .sys_net_receive_packet
	cmp eax, 26
    je .sys_mem_write_byte
    cmp eax, 27
    je .sys_mem_read_byte

    xor eax, eax
    jmp .done

.sys_kalloc:
    push dword [sys_arg_a]
    call sys_kalloc
    add esp, 4
    jmp .done

.sys_set_color:
    mov eax, [sys_arg_a]
    or eax, 0xFF000000
    mov [current_color], eax
    xor eax, eax
    jmp .done

.sys_fill_rect:
    push dword [sys_arg_d]
    push dword [sys_arg_c]
    push dword [sys_arg_b]
    push dword [sys_arg_a]
    call sys_fill_rect
    add esp, 16
    xor eax, eax
    jmp .done

.sys_draw_rect:
    push dword [sys_arg_d]
    push dword [sys_arg_c]
    push dword [sys_arg_b]
    push dword [sys_arg_a]
    call sys_draw_rect
    add esp, 16
    xor eax, eax
    jmp .done

.sys_draw_line:
    push dword [sys_arg_d]
    push dword [sys_arg_c]
    push dword [sys_arg_b]
    push dword [sys_arg_a]
    call sys_draw_line
    add esp, 16
    xor eax, eax
    jmp .done

.sys_draw_string:
    push dword [sys_arg_c]
    push dword [sys_arg_b]
    push dword [sys_arg_a]
    call sys_draw_string
    add esp, 12
    xor eax, eax
    jmp .done

.sys_read_keyboard:
    call sys_read_keyboard_scancode
    jmp .done

.sys_read_mouse:
    push dword [sys_arg_a]
    call sys_read_mouse
    add esp, 4
    jmp .done

.sys_disk_read:
    push dword [sys_arg_c]  ; Puntero al Buffer
    push dword [sys_arg_a]  ; LBA
    call sys_disk_read_sector
    add esp, 8
    jmp .done

.sys_disk_write:
    push dword [sys_arg_c]  ; Puntero al Buffer
    push dword [sys_arg_a]  ; LBA
    call sys_disk_write_sector
    add esp, 8
    jmp .done
	
.sys_inb:
    push dword [sys_arg_a]
    call sys_inb
    add esp, 4
    jmp .done

.sys_outb:
    push dword [sys_arg_b]
    push dword [sys_arg_a]
    call sys_outb
    add esp, 8
    jmp .done

.sys_sleep:
    push dword [sys_arg_a]
    call sys_sleep
    add esp, 4
    xor eax, eax
    jmp .done
	
.sys_get_time:
    push dword [sys_arg_a]
    call sys_get_time
    add esp, 4
    jmp .done
	
.sys_get_pixel:
    push dword [sys_arg_b]
    push dword [sys_arg_a]
    call sys_get_pixel
    add esp, 8
    jmp .done	

.sys_draw_char:
    push dword [current_color]
    push dword [sys_arg_b]
    push dword [sys_arg_a]
    push dword [sys_arg_c]
    call draw_char_vram
    add esp, 16
    xor eax, eax
    jmp .done

.sys_set_kbd_layout:
    push dword [sys_arg_a]
    call sys_set_keyboard_layout
    add esp, 4
    mov eax, 1
    jmp .done

.sys_exit:
    call sys_exit
    xor eax, eax
    jmp .done

.sys_get_ticks:
    call sys_get_ticks
    jmp .done
	
.sys_serial_putc:
    push dword [sys_arg_a]
    call sys_serial_putc
    add esp, 4
    xor eax, eax
    jmp .done

.sys_serial_puts:
    push dword [sys_arg_c]      ; las cadenas de Java vienen en arg_c
    call sys_serial_print_java  ; llama al handler exclusivo de Java
    add esp, 4
    xor eax, eax
    jmp .done	

.sys_pci_read:
    push dword [sys_arg_d]
    push dword [sys_arg_c]
    push dword [sys_arg_b]
    push dword [sys_arg_a]
    call sys_pci_read_config
    add esp, 16
    jmp .done

.sys_beep:
    push dword [sys_arg_a]
    call sys_beep
    add esp, 4
    jmp .done

.sys_rtl8139_init:
    push dword [sys_arg_a]
    call sys_rtl8139_init
    add esp, 4
    jmp .done

.sys_rtl8139_send_packet:
    push dword [sys_arg_b]     ; Longitud
    push dword [sys_arg_c]     ; Puntero al Buffer
    call sys_rtl8139_send_packet
    add esp, 8
    xor eax, eax
    jmp .done

.sys_net_receive_packet:
    push dword [sys_arg_b]
    push dword [sys_arg_c]
    call sys_net_receive_packet
    add esp, 8
    jmp .done	
	
.sys_mem_write_byte:
    ; sys_arg_a = Dirección física (Puntero)
    ; sys_arg_b = Valor del byte a escribir (0-255)
    mov eax, [sys_arg_a]
    mov ebx, [sys_arg_b]
    mov byte [eax], bl          ; Escribir el byte en la memoria RAM
    xor eax, eax                ; Retornar 0
    jmp .done

.sys_mem_read_byte:
    ; sys_arg_a = Dirección física (Puntero)
    mov eax, [sys_arg_a]
    xor ebx, ebx
    mov bl, byte [eax]          ; Leer el byte de la memoria RAM
    mov eax, ebx                ; Retornar el byte leído
    jmp .done

.done:
    pop edx
    pop ecx
    pop ebx
    mov esp, ebp
    pop ebp
    ret

; COMPILADOR JIT

jit_compile_method:
    push ebp
    mov ebp, esp
    push ebx
    push esi
    push edi
    
    mov eax, [jit_buffer_ptr]
    push eax 
    
    mov dword [loop_start_addr], 0
    mov dword [fixup_count], 0
    mov [jit_bytecode_base], esi

    push esi                    
    push ecx                    
    call jit_emit_prologue
    pop ecx                     

    mov edi, esi
    add edi, ecx                

.compile_loop:
    cmp esi, edi
    jae .resolve_fixups         

    mov ecx, esi
    sub ecx, [jit_bytecode_base] 
    mov edx, [jit_buffer_ptr]    
    mov [pc_map + ecx * 4], edx  

	; debug
    movzx eax, byte [esi]
	pusha
	mov ebx, eax
	shr ebx, 4
	call .nibble_to_hex
	mov [hex_byte_str], bl
	mov ebx, eax
	and ebx, 0x0F
	call .nibble_to_hex
	mov [hex_byte_str + 1], bl
	push hex_byte_str
	call sys_serial_puts
	add esp, 4
	popa
	; fin del debug
	movzx eax, byte [esi]
    inc esi
    mov ebx, [jit_opcode_table + eax * 4]
    call ebx

    jmp .compile_loop

.resolve_fixups:
    mov ecx, [fixup_count]
    test ecx, ecx
    jz .compile_done
    xor ebx, ebx

.fixup_loop:
    mov eax, [fixup_target + ebx * 4]
    mov edx, [pc_map + eax * 4]
    mov edi, [fixup_addr + ebx * 4]
    
    mov eax, edx
    sub eax, edi
    sub eax, 4                        
    mov [edi], eax

    inc ebx
    cmp ebx, ecx
    jb .fixup_loop

.compile_done:
    pop edx                     ; Descartar el 'push esi' sobrante
    pop eax                     ; Recuperar el puntero JIT original
    
    pop edi                     
    pop esi                     
    pop ebx
    mov esp, ebp
    pop ebp
    ret

.nibble_to_hex:
    cmp bl, 9
    jbe .is_digit
    add bl, 7
.is_digit:
    add bl, '0'
    ret

; OPCODES BÁSICOS
jit_op_aconst_null:
    mov al, 0x6A
    call jit_emit_byte
    mov al, 0x00
    call jit_emit_byte
    ret

jit_op_iconst_m1:
    mov al, 0x6A
    call jit_emit_byte
    mov al, 0xFF
    call jit_emit_byte
    ret
    
jit_op_nop:
    mov al, 0x90
    call jit_emit_byte
    ret

jit_op_iconst_0:
    mov al, 0x6A
    call jit_emit_byte
    mov al, 0x00
    call jit_emit_byte
    ret

jit_op_iconst_1:
    mov al, 0x6A
    call jit_emit_byte
    mov al, 0x01
    call jit_emit_byte
    ret

jit_op_iconst_2:
    mov al, 0x6A
    call jit_emit_byte
    mov al, 0x02
    call jit_emit_byte
    ret

jit_op_iconst_3:
    mov al, 0x6A
    call jit_emit_byte
    mov al, 0x03
    call jit_emit_byte
    ret

jit_op_iconst_4:
    mov al, 0x6A
    call jit_emit_byte
    mov al, 0x04
    call jit_emit_byte
    ret

jit_op_iconst_5:
    mov al, 0x6A
    call jit_emit_byte
    mov al, 0x05
    call jit_emit_byte
    ret

jit_op_lconst_0:
    mov al, 0x6A
    call jit_emit_byte
    mov al, 0x00
    call jit_emit_byte
    mov al, 0x6A
    call jit_emit_byte
    mov al, 0x00
    call jit_emit_byte
    ret

jit_op_lconst_1:
    mov al, 0x6A
    call jit_emit_byte
    mov al, 0x00
    call jit_emit_byte
    mov al, 0x6A
    call jit_emit_byte
    mov al, 0x01
    call jit_emit_byte
    ret

jit_op_bipush:
    movsx eax, byte [esi]
    inc esi
    push eax
    mov al, 0x68
    call jit_emit_byte
    pop eax
    call jit_emit_dword
    ret

jit_op_sipush:
    movzx eax, byte [esi]
    inc esi
    movzx ebx, byte [esi]
    inc esi
    shl eax, 8
    or eax, ebx
    movsx eax, ax

    push eax
    mov al, 0x68
    call jit_emit_byte
    pop eax
    call jit_emit_dword
    ret

jit_op_ldc:
    movzx eax, byte [esi]
    inc esi

    mov ebx, [cp_base_ptr]
    test ebx, ebx
    jz .fallback_zero

    mov eax, [ebx + eax * 4]
    test eax, eax
    jz .fallback_zero

    cmp byte [eax], 8
    je .is_string

    mov eax, [eax + 1]
    bswap eax
    jmp .emit_val

.is_string:
    mov ax, [eax + 1]
    xchg al, ah
    movzx eax, ax
    mov ebx, [cp_base_ptr]
    mov eax, [ebx + eax * 4]
    add eax, 3
	
	; Crear Objeto en el Heap durante Compilación 
    pusha
    mov esi, eax                ; ESI apunta a los caracteres en el CP
    movzx ecx, byte [esi - 2]
    shl ecx, 8
    mov cl, byte [esi - 1]      ; ECX = Longitud real de la cadena

    mov edx, ecx
    add edx, 4                  ; Tamaño = Longitud + 4 bytes de header
    push edx
    call sys_kalloc
    add esp, 4
    ; EAX = Puntero al nuevo arreglo byte[] en el Heap

    ; Recuperar EAX original de pusha porque sys_kalloc altera registros
    mov esi, [esp + 28]         
    movzx ecx, byte [esi - 2]
    shl ecx, 8
    mov cl, byte [esi - 1]

    mov [eax], ecx              ; Escribir longitud en el header del arreglo
    mov edi, eax
    add edi, 4                  ; Apuntar a los datos del arreglo
    rep movsb                   ; Copiar los caracteres

    mov [esp + 16], eax         ; Guardar puntero del byte[] en la posición EBX de pusha

    ; Crear el objeto String
    push 4096
    call sys_kalloc
    add esp, 4
    ; EAX = Puntero al nuevo objeto String

    mov ebx, [esp + 16]         ; Recuperar puntero del byte[]
	; --- prueba de saturación ---
	; Esto garantiza que text.getBytes() siempre funcione.
    mov edi, eax
    add edi, 8                  
    mov ecx, 256                
.fill_fields:
    mov [edi], ebx
    add edi, 4
    dec ecx
    jnz .fill_fields
	; --- fin de prueba ---
    ;mov [eax + 8], ebx          ; Asignar arreglo al campo 'value' (Offset 8 del String)

    mov [esp + 28], eax         ; Sobrescribir el EAX original con el objeto String
    popa
    jmp .emit_val

.fallback_zero:
    xor eax, eax

.emit_val:
    push eax
    mov al, 0x68
    call jit_emit_byte
    pop eax
    call jit_emit_dword
    ret

jit_op_ldc_w:
    movzx eax, byte [esi]
    inc esi
    movzx ebx, byte [esi]
    inc esi
    shl eax, 8
    or eax, ebx

    mov ebx, [cp_offsets + eax * 4]
    test ebx, ebx
    jz .fallback_zero

    cmp byte [ebx], 8
    je .is_string_w

    mov eax, [ebx + 1]
    bswap eax
    jmp .emit_val_w

.is_string_w:
    mov ax, [ebx + 1]
    xchg al, ah
    movzx eax, ax
    mov ebx, [cp_offsets + eax * 4]
    add ebx, 3
    mov eax, ebx
	; Crear objeto en el Heap durante compilación
	pusha
    mov esi, eax                
    movzx ecx, byte [esi - 2]
    shl ecx, 8
    mov cl, byte [esi - 1]      

    mov edx, ecx
    add edx, 4                  
    push edx
    call sys_kalloc
    add esp, 4
    
    mov esi, [esp + 28]         
    movzx ecx, byte [esi - 2]
    shl ecx, 8
    mov cl, byte [esi - 1]

    mov [eax], ecx              
    mov edi, eax
    add edi, 4                  
    rep movsb                   

    mov [esp + 16], eax         

    push 4096
    call sys_kalloc
    add esp, 4
    
    mov ebx, [esp + 16]         
    ;mov [eax + 8], ebx   
	; --- prueba de saturación---
	mov edi, eax
	add edi, 8
	mov ecx, 256
.fill_fields_w:
	mov [edi], ebx
	add edi, 4
	dec ecx
	jnz .fill_fields_w
	; --- fin d prueba ---

    mov [esp + 28], eax         
    popa
    jmp .emit_val_w

.fallback_zero:
    xor eax, eax

.emit_val_w:
    push eax
    mov al, 0x68
    call jit_emit_byte
    pop eax
    call jit_emit_dword
    ret

jit_op_ldc2_w:
    movzx eax, byte [esi]
    inc esi
    movzx ebx, byte [esi]
    inc esi
    shl eax, 8
    or eax, ebx

    mov ebx, [cp_offsets + eax * 4]
    test ebx, ebx
    jz .fallback_zero_64

    mov eax, [ebx + 1]
    mov edx, [ebx + 5]
    bswap eax
    bswap edx
    jmp .emit_val_64

.fallback_zero_64:
    xor eax, eax
    xor edx, edx

.emit_val_64:
    push edx
    push eax
    
    mov al, 0x68
    call jit_emit_byte
    pop eax
    call jit_emit_dword
    
    mov al, 0x68
    call jit_emit_byte
    pop eax
    call jit_emit_dword
    ret

jit_op_new:
    add esi, 2

    mov al, 0x68                ; push imm32
    call jit_emit_byte
    mov eax, 4096               ; Ampliar Allocation a 4KB por objeto 
    call jit_emit_dword

    mov al, 0xE8                ; call sys_kalloc
    call jit_emit_byte
    mov eax, sys_kalloc
    mov ebx, [jit_buffer_ptr]
    add ebx, 4
    sub eax, ebx
    call jit_emit_dword

    mov al, 0x83                ; add esp, 4
    call jit_emit_byte
    mov al, 0xC4
    call jit_emit_byte
    mov al, 0x04
    call jit_emit_byte

    mov al, 0x50                ; push eax
    call jit_emit_byte
    ret

jit_op_getstatic:
    movzx eax, byte [esi]
    inc esi
    movzx ebx, byte [esi]
    inc esi
    shl eax, 8
    or eax, ebx

    ; Limitar el CP Index a 1023 max variables estáticas para no desbordar
    and eax, 0x03FF             
    mov ebx, eax
    shl ebx, 2
    add ebx, java_static_vars

    mov al, 0xA1                ; mov eax, [addr]
    call jit_emit_byte
    mov eax, ebx
    call jit_emit_dword
    
    mov al, 0x50                ; push eax
    call jit_emit_byte
    ret

jit_op_putstatic:
    movzx eax, byte [esi]
    inc esi
    movzx ebx, byte [esi]
    inc esi
    shl eax, 8
    or eax, ebx

    ; Limitar el CP Index a 1023 max variables estáticas para no desbordar
    and eax, 0x03FF
    mov ebx, eax
    shl ebx, 2
    add ebx, java_static_vars

    mov al, 0x58                ; pop eax
    call jit_emit_byte
    mov al, 0xA3                ; mov [addr], eax
    call jit_emit_byte
    mov eax, ebx
    call jit_emit_dword
    ret

jit_op_getfield:
    movzx eax, byte [esi]
    inc esi
    movzx ebx, byte [esi]
    inc esi
    shl eax, 8
    or eax, ebx

    mov ecx, eax
    shl ecx, 2
    add ecx, 8

    mov al, 0x58                ; pop eax (object reference)
    call jit_emit_byte
    
    mov al, 0x8B                ; mov eax, [eax + imm32]
    call jit_emit_byte
    mov al, 0x80
    call jit_emit_byte
    mov eax, ecx
    call jit_emit_dword
    
    mov al, 0x50                ; push eax
    call jit_emit_byte
    ret

jit_op_putfield:
    movzx eax, byte [esi]
    inc esi
    movzx ebx, byte [esi]
    inc esi
    shl eax, 8
    or eax, ebx

    mov ecx, eax
    shl ecx, 2
    add ecx, 8

    mov al, 0x58                ; pop eax (valor a asignar)
    call jit_emit_byte
    mov al, 0x5B                ; pop ebx (referencia del objeto)
    call jit_emit_byte
    
    mov al, 0x89                ; mov [ebx + imm32], eax
    call jit_emit_byte
    mov al, 0x83
    call jit_emit_byte
    mov eax, ecx
    call jit_emit_dword
    ret

jit_op_invokeinterface:
    add esi, 2
    jmp jit_op_invokevirtual

jit_op_invokedynamic:
    add esi, 4
    ret

jit_op_checkcast:
    add esi, 2
    ret

jit_op_instanceof:
    add esi, 2
    mov al, 0x58
    call jit_emit_byte
    mov al, 0x85
    call jit_emit_byte
    mov al, 0xC0
    call jit_emit_byte
    mov al, 0x0F
    call jit_emit_byte
    mov al, 0x95
    call jit_emit_byte
    mov al, 0xC0
    call jit_emit_byte
    mov al, 0x0F
    call jit_emit_byte
    mov al, 0xB6
    call jit_emit_byte
    mov al, 0xC0
    call jit_emit_byte
    mov al, 0x50
    call jit_emit_byte
    ret

jit_op_monitorenter:
    mov al, 0x58
    call jit_emit_byte
    ret

jit_op_monitorexit:
    mov al, 0x58
    call jit_emit_byte
    ret

jit_op_wide:
    ; ESI -> opcode modificado
    movzx eax, byte [esi]
    inc esi
    ; índice u16
    movzx ebx, byte [esi]
    inc esi
    shl ebx, 8
    movzx ecx, byte [esi]
    inc esi
    or ebx, ecx    
    ; offset local = -(16 + index*4)
    mov ecx, ebx
    shl ecx, 2
    add ecx, 16
    neg ecx
    ; iload / fload / aload = 0x15 / 0x17 / 0x19
    cmp al, 0x15
    je .load
    cmp al, 0x17
    je .load
    cmp al, 0x19
    je .load    
    ; istore / fstore / astore = 0x36 / 0x38 / 0x3A
    cmp al, 0x36
    je .store
    cmp al, 0x38
    je .store
    cmp al, 0x3A
    je .store
    ; iinc
    cmp al, 0x84
    je .iinc
    ; ret
    cmp al, 0xA9
    je .ret
    ret
.load:
    ; mov eax,[ebp+disp32]
    mov al, 0x8B
    call jit_emit_byte
    mov al, 0x85
    call jit_emit_byte
    mov eax, ecx
    call jit_emit_dword
    ; push eax
    mov al, 0x50
    call jit_emit_byte
    ret

.store:
    ; pop eax
    mov al, 0x58
    call jit_emit_byte
    ; mov [ebp+disp32],eax
    mov al, 0x89
    call jit_emit_byte
    mov al, 0x85
    call jit_emit_byte
    mov eax, ecx
    call jit_emit_dword
    ret

.iinc:
    ; ESI apunta al high byte del const
    movsx edx, byte [esi]
    shl edx, 8
    inc esi
    movzx ebx, byte [esi]
    inc esi
    or edx, ebx
    movsx edx, dx
    ; add dword [ebp+disp32],imm32
    mov al, 0x81
    call jit_emit_byte
    mov al, 0x85
    call jit_emit_byte
    mov eax, ecx
    call jit_emit_dword
    mov eax, edx
    call jit_emit_dword
    ret
.ret:
    ; mov eax,[ebp+disp32]
    mov al, 0x8B
    call jit_emit_byte
    mov al, 0x85
    call jit_emit_byte
    mov eax, ecx
    call jit_emit_dword
    ; jmp eax
    mov al, 0xFF
    call jit_emit_byte
    mov al, 0xE0
    call jit_emit_byte
    ret

.wide_load:
    mov al, 0x8B
    call jit_emit_byte
    mov al, 0x85
    call jit_emit_byte
    mov eax, ebx
    call jit_emit_dword
    mov al, 0x50
    call jit_emit_byte
    ret

.wide_store:
    mov al, 0x58
    call jit_emit_byte
    mov al, 0x89
    call jit_emit_byte
    mov al, 0x85
    call jit_emit_byte
    mov eax, ebx
    call jit_emit_dword
    ret

jit_op_multianewarray:
    add esi, 2
    movzx eax, byte [esi]
    inc esi
    jmp jit_op_newarray

; Emisión de invocaciones
jit_op_invokevirtual:
jit_op_invokespecial:
jit_op_invokestatic:
    ; 1. Extraer el índice de 16 bits de forma segura
    movzx ebx, byte [esi]       ; Byte alto
    shl ebx, 8
    movzx eax, byte [esi+1]     ; Byte bajo
    or ebx, eax                 ; EBX = CP_Index (16-bit completo)
    add esi, 2                  ; Avanzar el puntero del bytecode

    ; 2. Emitir x86: mov eax, <CP_Index>
    mov al, 0xB8
    call jit_emit_byte
    mov eax, ebx                ; Ahora sí inyectamos el índice completo
    call jit_emit_dword

    ; 3. Emitir x86: mov edx, <current_class_ptr>
    mov al, 0xBA
    call jit_emit_byte
    mov eax, [current_class_ptr]
    call jit_emit_dword

    ; 4. Emitir x86: call rel32 jit_runtime_trampoline
    mov al, 0xE8
    call jit_emit_byte
    mov eax, jit_runtime_trampoline
    push ebx                    ; Proteger nuestro CP_Index intacto
    mov ebx, [jit_buffer_ptr]
    add ebx, 4
    sub eax, ebx
    call jit_emit_dword
    pop ebx                     ; Recuperar CP_Index

    ; Evaluar el tipo de retorno y apilar
    mov ecx, [cp_offsets + ebx * 4]     ; CONSTANT_Methodref
    movzx edx, word [ecx + 3]
    xchg dl, dh
    mov ecx, [cp_offsets + edx * 4]     ; CONSTANT_NameAndType
    movzx edx, word [ecx + 3]
    xchg dl, dh
    mov ecx, [cp_offsets + edx * 4]     ; CONSTANT_Utf8 (Descriptor)
    add ecx, 3                          ; Saltar Tag y Length

.scan_desc:
    mov al, [ecx]
    inc ecx
    cmp al, ')'
    jne .scan_desc
    
    mov al, [ecx]               ; Leer la letra del tipo de retorno
    cmp al, 'V'
    je .is_void
    
    mov al, 0x50                ; push eax (Si no es 'V', empuja el resultado)
    call jit_emit_byte
    
.is_void:
    ret

jit_op_fconst_0:
    mov al, 0x68
    call jit_emit_byte
    mov eax, 0x00000000
    call jit_emit_dword
    ret

jit_op_fconst_1:
    mov al, 0x68
    call jit_emit_byte
    mov eax, 0x3F800000
    call jit_emit_dword
    ret

jit_op_fconst_2:
    mov al, 0x68
    call jit_emit_byte
    mov eax, 0x40000000
    call jit_emit_dword
    ret

jit_op_dconst_0:
    mov al, 0x68
    call jit_emit_byte
    mov eax, 0x00000000
    call jit_emit_dword
    mov al, 0x68
    call jit_emit_byte
    mov eax, 0x00000000
    call jit_emit_dword
    ret

jit_op_dconst_1:
    mov al, 0x68
    call jit_emit_byte
    mov eax, 0x3FF00000
    call jit_emit_dword
    mov al, 0x68
    call jit_emit_byte
    mov eax, 0x00000000
    call jit_emit_dword
    ret

jit_op_iload:
jit_op_fload:
jit_op_aload:
    movzx ebx, byte [esi]
    inc esi
    shl ebx, 2
    add ebx, 16
    neg ebx                 ; ebx = offset negativo de 32-bits

    mov al, 0x8B            ; mov eax, [ebp + disp32]
    call jit_emit_byte
    mov al, 0x85            ; ModR/M 0x85
    call jit_emit_byte
    mov eax, ebx
    call jit_emit_dword
    
    mov al, 0x50            ; push eax
    call jit_emit_byte
    ret

jit_op_iload_0:
jit_op_aload_0:
jit_op_fload_0:
    mov al, 0x8B
    call jit_emit_byte
    mov al, 0x45
    call jit_emit_byte
    mov al, 0xF0
    call jit_emit_byte
    mov al, 0x50
    call jit_emit_byte
    ret

jit_op_iload_1:
jit_op_aload_1:
jit_op_fload_1:
    mov al, 0x8B
    call jit_emit_byte
    mov al, 0x45
    call jit_emit_byte
    mov al, 0xEC
    call jit_emit_byte
    mov al, 0x50
    call jit_emit_byte
    ret

jit_op_iload_2:
jit_op_aload_2:
jit_op_fload_2:
    mov al, 0x8B
    call jit_emit_byte
    mov al, 0x45
    call jit_emit_byte
    mov al, 0xE8
    call jit_emit_byte
    mov al, 0x50
    call jit_emit_byte
    ret

jit_op_iload_3:
jit_op_aload_3:
jit_op_fload_3:
    mov al, 0x8B
    call jit_emit_byte
    mov al, 0x45
    call jit_emit_byte
    mov al, 0xE4
    call jit_emit_byte
    mov al, 0x50
    call jit_emit_byte
    ret

jit_op_istore:
jit_op_astore:
jit_op_fstore:
    movzx ebx, byte [esi]
    inc esi
    shl ebx, 2
    add ebx, 16
    neg ebx                 ; ebx = offset negativo de 32-bits

    mov al, 0x58            ; pop eax
    call jit_emit_byte
    
    mov al, 0x89            ; mov [ebp + disp32], eax
    call jit_emit_byte
    mov al, 0x85            ; ModR/M 0x85
    call jit_emit_byte
    mov eax, ebx
    call jit_emit_dword
    ret

jit_op_iload_4:
jit_op_aload_4:
jit_op_fload_4:
    mov al, 0x8B
    call jit_emit_byte
    mov al, 0x45
    call jit_emit_byte
    mov al, 0xE0
    call jit_emit_byte
    mov al, 0x50
    call jit_emit_byte
    ret

jit_op_iload_5:
jit_op_aload_5:
jit_op_fload_5:
    mov al, 0x8B
    call jit_emit_byte
    mov al, 0x45
    call jit_emit_byte
    mov al, 0xDC
    call jit_emit_byte
    mov al, 0x50
    call jit_emit_byte
    ret

jit_op_lload_0:
jit_op_dload_0:
    mov al, 0x8B
    call jit_emit_byte
    mov al, 0x45
    call jit_emit_byte
    mov al, 0xF0
    call jit_emit_byte
    mov al, 0x50
    call jit_emit_byte
    mov al, 0x8B
    call jit_emit_byte
    mov al, 0x45
    call jit_emit_byte
    mov al, 0xEC
    call jit_emit_byte
    mov al, 0x50
    call jit_emit_byte
    ret

jit_op_lload_1:
jit_op_dload_1:
    mov al, 0x8B
    call jit_emit_byte
    mov al, 0x45
    call jit_emit_byte
    mov al, 0xE8
    call jit_emit_byte
    mov al, 0x50
    call jit_emit_byte
    mov al, 0x8B
    call jit_emit_byte
    mov al, 0x45
    call jit_emit_byte
    mov al, 0xE4
    call jit_emit_byte
    mov al, 0x50
    call jit_emit_byte
    ret

jit_op_lstore_1:
    mov al, 0x58
    call jit_emit_byte
    mov al, 0x89
    call jit_emit_byte
    mov al, 0x45
    call jit_emit_byte
    mov al, 0xE4
    call jit_emit_byte
    mov al, 0x58
    call jit_emit_byte
    mov al, 0x89
    call jit_emit_byte
    mov al, 0x45
    call jit_emit_byte
    mov al, 0xE8
    call jit_emit_byte
    ret

jit_op_lstore_0:
    ; low word -> local slot 1
    mov al, 0x58
    call jit_emit_byte
    mov al, 0x89
    call jit_emit_byte
    mov al, 0x45
    call jit_emit_byte
    mov al, 0xEC                ; [ebp-20]
    call jit_emit_byte
    ; high word -> local slot 0
    mov al, 0x58
    call jit_emit_byte
    mov al, 0x89
    call jit_emit_byte
    mov al, 0x45
    call jit_emit_byte
    mov al, 0xF0                ; [ebp-16]
    call jit_emit_byte
    ret

jit_op_istore_0:
jit_op_astore_0:
    mov al, 0x58
    call jit_emit_byte
    mov al, 0x89
    call jit_emit_byte
    mov al, 0x45
    call jit_emit_byte
    mov al, 0xF0
    call jit_emit_byte
    ret

jit_op_istore_1:
jit_op_astore_1:
    mov al, 0x58
    call jit_emit_byte
    mov al, 0x89
    call jit_emit_byte
    mov al, 0x45
    call jit_emit_byte
    mov al, 0xEC
    call jit_emit_byte
    ret

jit_op_istore_2:
jit_op_astore_2:
    mov al, 0x58
    call jit_emit_byte
    mov al, 0x89
    call jit_emit_byte
    mov al, 0x45
    call jit_emit_byte
    mov al, 0xE8
    call jit_emit_byte
    ret

jit_op_istore_3:
jit_op_astore_3:
    mov al, 0x58
    call jit_emit_byte
    mov al, 0x89
    call jit_emit_byte
    mov al, 0x45
    call jit_emit_byte
    mov al, 0xE4
    call jit_emit_byte
    ret

jit_op_istore_4:
jit_op_astore_4:
    mov al, 0x58
    call jit_emit_byte
    mov al, 0x89
    call jit_emit_byte
    mov al, 0x45
    call jit_emit_byte
    mov al, 0xE0
    call jit_emit_byte
    ret

jit_op_istore_5:
jit_op_astore_5:
    mov al, 0x58
    call jit_emit_byte
    mov al, 0x89
    call jit_emit_byte
    mov al, 0x45
    call jit_emit_byte
    mov al, 0xDC
    call jit_emit_byte
    ret

jit_op_iload_param_0:
    mov al, 0x8B
    call jit_emit_byte
    mov al, 0x45
    call jit_emit_byte
    mov al, 0x08
    call jit_emit_byte
    mov al, 0x50
    call jit_emit_byte
    ret

jit_op_iload_param_1:
    mov al, 0x8B
    call jit_emit_byte
    mov al, 0x45
    call jit_emit_byte
    mov al, 0x0C
    call jit_emit_byte
    mov al, 0x50
    call jit_emit_byte
    ret

jit_op_iadd:
    mov al, 0x5B
    call jit_emit_byte
    mov al, 0x58
    call jit_emit_byte
    mov al, 0x01
    call jit_emit_byte
    mov al, 0xD8
    call jit_emit_byte
    mov al, 0x50
    call jit_emit_byte
    ret

jit_op_isub:
    mov al, 0x5B
    call jit_emit_byte
    mov al, 0x58
    call jit_emit_byte
    mov al, 0x29
    call jit_emit_byte
    mov al, 0xD8
    call jit_emit_byte
    mov al, 0x50
    call jit_emit_byte
    ret

jit_op_imul:
    mov al, 0x5B
    call jit_emit_byte
    mov al, 0x58
    call jit_emit_byte
    mov al, 0x0F
    call jit_emit_byte
    mov al, 0xAF
    call jit_emit_byte
    mov al, 0xC3
    call jit_emit_byte
    mov al, 0x50
    call jit_emit_byte
    ret

jit_op_idiv:
    mov al, 0x5B
    call jit_emit_byte
    mov al, 0x58
    call jit_emit_byte
    mov al, 0x99
    call jit_emit_byte
    mov al, 0xF7
    call jit_emit_byte
    mov al, 0xFB
    call jit_emit_byte
    mov al, 0x50
    call jit_emit_byte
    ret

jit_op_irem:
    mov al, 0x5B
    call jit_emit_byte
    mov al, 0x58
    call jit_emit_byte
    mov al, 0x99
    call jit_emit_byte
    mov al, 0xF7
    call jit_emit_byte
    mov al, 0xFB
    call jit_emit_byte
    mov al, 0x52
    call jit_emit_byte
    ret

jit_op_ineg:
    mov al, 0x58
    call jit_emit_byte
    mov al, 0xF7
    call jit_emit_byte
    mov al, 0xD8
    call jit_emit_byte
    mov al, 0x50
    call jit_emit_byte
    ret

jit_op_ishl:
    mov al, 0x59
    call jit_emit_byte
    mov al, 0x58
    call jit_emit_byte
    mov al, 0xD3
    call jit_emit_byte
    mov al, 0xE0
    call jit_emit_byte
    mov al, 0x50
    call jit_emit_byte
    ret

jit_op_ishr:
    mov al, 0x59
    call jit_emit_byte
    mov al, 0x58
    call jit_emit_byte
    mov al, 0xD3
    call jit_emit_byte
    mov al, 0xF8
    call jit_emit_byte
    mov al, 0x50
    call jit_emit_byte
    ret

jit_op_iushr:
    mov al, 0x59
    call jit_emit_byte
    mov al, 0x58
    call jit_emit_byte
    mov al, 0xD3
    call jit_emit_byte
    mov al, 0xE8
    call jit_emit_byte
    mov al, 0x50
    call jit_emit_byte
    ret

jit_op_iand:
    mov al, 0x5B
    call jit_emit_byte
    mov al, 0x58
    call jit_emit_byte
    mov al, 0x21
    call jit_emit_byte
    mov al, 0xD8
    call jit_emit_byte
    mov al, 0x50
    call jit_emit_byte
    ret

jit_op_ior:
    mov al, 0x5B
    call jit_emit_byte
    mov al, 0x58
    call jit_emit_byte
    mov al, 0x09
    call jit_emit_byte
    mov al, 0xD8
    call jit_emit_byte
    mov al, 0x50
    call jit_emit_byte
    ret

jit_op_ixor:
    mov al, 0x5B
    call jit_emit_byte
    mov al, 0x58
    call jit_emit_byte
    mov al, 0x31
    call jit_emit_byte
    mov al, 0xD8
    call jit_emit_byte
    mov al, 0x50
    call jit_emit_byte
    ret

jit_op_iinc:
    movzx ebx, byte [esi]
    inc esi
    movzx ecx, byte [esi]
    inc esi
    
    shl ebx, 2
    add ebx, 16
    neg ebx
    
    mov al, 0x83            ; add dword [ebp+disp32], imm8
    call jit_emit_byte
    mov al, 0x85
    call jit_emit_byte
    mov eax, ebx
    call jit_emit_dword
    mov al, cl              ; emitir el valor a incrementar (1 byte)
    call jit_emit_byte
    ret

jit_op_i2l:
    mov al, 0x58
    call jit_emit_byte
    mov al, 0x99
    call jit_emit_byte
    mov al, 0x52
    call jit_emit_byte
    mov al, 0x50
    call jit_emit_byte
    ret

jit_op_l2i:
    mov al, 0x58
    call jit_emit_byte
    mov al, 0x5B
    call jit_emit_byte
    mov al, 0x50
    call jit_emit_byte
    ret

jit_op_i2b:
    mov al, 0x58
    call jit_emit_byte
    mov al, 0x0F
    call jit_emit_byte
    mov al, 0xBE
    call jit_emit_byte
    mov al, 0xC0
    call jit_emit_byte
    mov al, 0x50
    call jit_emit_byte
    ret

jit_op_i2c:
    mov al, 0x58
    call jit_emit_byte
    mov al, 0x0F
    call jit_emit_byte
    mov al, 0xB7
    call jit_emit_byte
    mov al, 0xC0
    call jit_emit_byte
    mov al, 0x50
    call jit_emit_byte
    ret

jit_op_ifeq:
    mov al, 0x58
    call jit_emit_byte
    mov al, 0x83
    call jit_emit_byte
    mov al, 0xF8
    call jit_emit_byte
    mov al, 0x00
    call jit_emit_byte
    mov al, 0x0F
    call jit_emit_byte
    mov al, 0x84
    call jit_emit_byte
    call jit_emit_branch_target
    ret

jit_op_ifne:
    mov al, 0x58
    call jit_emit_byte
    mov al, 0x83
    call jit_emit_byte
    mov al, 0xF8
    call jit_emit_byte
    mov al, 0x00
    call jit_emit_byte
    mov al, 0x0F
    call jit_emit_byte
    mov al, 0x85
    call jit_emit_byte
    call jit_emit_branch_target
    ret

jit_op_ifle:
    mov al, 0x58
    call jit_emit_byte
    mov al, 0x83
    call jit_emit_byte
    mov al, 0xF8
    call jit_emit_byte
    mov al, 0x00
    call jit_emit_byte
    mov al, 0x0F
    call jit_emit_byte
    mov al, 0x8E
    call jit_emit_byte
    call jit_emit_branch_target
    ret

jit_op_ifge:
    mov al, 0x58
    call jit_emit_byte
    mov al, 0x83
    call jit_emit_byte
    mov al, 0xF8
    call jit_emit_byte
    mov al, 0x00
    call jit_emit_byte
    mov al, 0x0F
    call jit_emit_byte
    mov al, 0x8D
    call jit_emit_byte
    call jit_emit_branch_target
    ret

jit_emit_icmp_branch:
    push ecx
    mov al, 0x5B
    call jit_emit_byte
    mov al, 0x58
    call jit_emit_byte
    mov al, 0x39
    call jit_emit_byte
    mov al, 0xD8
    call jit_emit_byte

    mov al, 0x0F
    call jit_emit_byte
    pop ecx
    mov al, cl
    call jit_emit_byte

    call jit_emit_branch_target
    ret

jit_op_if_icmpne:
    mov ecx, 0x85
    jmp jit_emit_icmp_branch

jit_op_if_icmplt:
    mov ecx, 0x8C
    jmp jit_emit_icmp_branch

jit_op_if_icmpge:
    mov ecx, 0x8D
    jmp jit_emit_icmp_branch

jit_op_if_icmpgt:
    mov ecx, 0x8F
    jmp jit_emit_icmp_branch

jit_op_if_icmple:
    mov ecx, 0x8E
    jmp jit_emit_icmp_branch

jit_op_iaload:
    ; 1. Desapilar el Índice (EBX) y la Base del Array (EAX)
    mov al, 0x5B                ; pop ebx (Índice i)
    call jit_emit_byte
    mov al, 0x58                ; pop eax (Puntero al Array)
    call jit_emit_byte

    ; 2. Calcular la dirección física: EAX = Base + 4 + (EBX * 4)
    ; Opcode x86: lea eax, [eax + ebx*4 + 4]
    mov al, 0x8D                ; LEA
    call jit_emit_byte
    mov al, 0x44                ; ModR/M: disp8 con SIB
    call jit_emit_byte
    mov al, 0x98                ; SIB: [eax + ebx*4]
    call jit_emit_byte
    mov al, 0x04                ; Displamiento constante de +4 bytes (Header de longitud Java)
    call jit_emit_byte

    ; 3. Cargar el valor entero de la celda: mov eax, [eax]
    mov al, 0x8B
    call jit_emit_byte
    mov al, 0x00
    call jit_emit_byte

    ; 4. Apilar el resultado en la pila de evaluación
    mov al, 0x50                ; push eax
    call jit_emit_byte
    ret

jit_op_aaload:
    mov al, 0x59
    call jit_emit_byte
    mov al, 0x58
    call jit_emit_byte
    mov al, 0x8B
    call jit_emit_byte
    mov al, 0x04
    call jit_emit_byte
    mov al, 0x88
    call jit_emit_byte
    mov al, 0x50
    call jit_emit_byte
    ret

jit_op_iastore:
    mov al, 0x5A                ; pop edx (Valor a guardar)
    call jit_emit_byte
    mov al, 0x5B                ; pop ebx (Índice i)
    call jit_emit_byte
    mov al, 0x58                ; pop eax (Puntero Base Array)
    call jit_emit_byte

    ; mov [eax + ebx*4 + 4], edx
    mov al, 0x89
    call jit_emit_byte
    mov al, 0x54
    call jit_emit_byte
    mov al, 0x98
    call jit_emit_byte
    mov al, 0x04
    call jit_emit_byte
    ret

jit_op_saload:
    mov al, 0x59
    call jit_emit_byte
    mov al, 0x5B
    call jit_emit_byte
    mov al, 0x0F
    call jit_emit_byte
    mov al, 0xBF
    call jit_emit_byte
    mov al, 0x4C
    call jit_emit_byte
    mov al, 0x4B
    call jit_emit_byte
    mov al, 0x08
    call jit_emit_byte
    mov al, 0x50
    call jit_emit_byte
    ret

jit_op_aastore:
    mov al, 0x58
    call jit_emit_byte
    mov al, 0x59
    call jit_emit_byte
    mov al, 0x5B
    call jit_emit_byte
    mov al, 0x89
    call jit_emit_byte
    mov al, 0x04
    call jit_emit_byte
    mov al, 0x8B
    call jit_emit_byte
    ret

jit_op_bastore:
    mov al, 0x58                ; pop eax (value)
    call jit_emit_byte
    mov al, 0x59                ; pop ecx (index)
    call jit_emit_byte
    mov al, 0x5B                ; pop ebx (arrayref)
    call jit_emit_byte
    mov al, 0x88                ; mov [ebx + ecx + 4], al
    call jit_emit_byte
    mov al, 0x44                ; ModRM: mod=01 (Permite desplazamiento disp8)
    call jit_emit_byte
    mov al, 0x0B                ; SIB: [ebx + ecx]
    call jit_emit_byte
    mov al, 0x04                ; disp8: +4 (Saltar cabecera)
    call jit_emit_byte
    ret

jit_op_castore:
jit_op_sastore:
    mov al, 0x58                ; pop eax
    call jit_emit_byte
    mov al, 0x59                ; pop ecx
    call jit_emit_byte
    mov al, 0x5B                ; pop ebx
    call jit_emit_byte
    mov al, 0x66
    call jit_emit_byte
    mov al, 0x89                ; mov [ebx + ecx*2 + 4], ax
    call jit_emit_byte
    mov al, 0x44                ; ModRM: mod=01
    call jit_emit_byte
    mov al, 0x4B                ; SIB: [ebx + ecx*2]
    call jit_emit_byte
    mov al, 0x04                ; disp8: +4
    call jit_emit_byte
    ret

jit_op_arraylength:
    mov al, 0x58                ; pop eax
    call jit_emit_byte
    mov al, 0x8B                ; mov eax, [eax] (Leer el header de longitud)
    call jit_emit_byte
    mov al, 0x00                ; ModRM: mod=00, reg=000, rm=000
    call jit_emit_byte
    mov al, 0x50                ; push eax
    call jit_emit_byte
    ret

jit_op_swap:
    mov al, 0x58
    call jit_emit_byte
    mov al, 0x5B
    call jit_emit_byte
    mov al, 0x50
    call jit_emit_byte
    mov al, 0x53
    call jit_emit_byte
    ret	

jit_op_dup:
    mov al, 0x58
    call jit_emit_byte
    mov al, 0x50
    call jit_emit_byte
    mov al, 0x50
    call jit_emit_byte
    ret

jit_op_pop:
    mov al, 0x58
    call jit_emit_byte
    ret

jit_op_dup_x1:
    mov al, 0x58
    call jit_emit_byte
    mov al, 0x5B
    call jit_emit_byte
    mov al, 0x50
    call jit_emit_byte
    mov al, 0x53
    call jit_emit_byte
    mov al, 0x50
    call jit_emit_byte
    ret

jit_op_dup_x2:
    mov al, 0x58
    call jit_emit_byte
    mov al, 0x5B
    call jit_emit_byte
    mov al, 0x59
    call jit_emit_byte
    mov al, 0x50
    call jit_emit_byte
    mov al, 0x51
    call jit_emit_byte
    mov al, 0x53
    call jit_emit_byte
    mov al, 0x50
    call jit_emit_byte
    ret

jit_op_dup2:
    ; top = B
    mov al, 0x58                ; pop eax
    call jit_emit_byte
    ; next = A
    mov al, 0x5B                ; pop ebx
    call jit_emit_byte
    ; A
    mov al, 0x53                ; push ebx
    call jit_emit_byte
    ; B
    mov al, 0x50                ; push eax
    call jit_emit_byte
    ; A
    mov al, 0x53
    call jit_emit_byte
    ; B
    mov al, 0x50
    call jit_emit_byte
    ret

jit_op_dup2_x1:
jit_op_dup2_x2:
    jmp jit_op_dup2

jit_op_if_icmpeq:
    mov ecx, 0x84
    jmp jit_emit_icmp_branch

jit_op_goto:
    mov al, 0xE9
    call jit_emit_byte
    call jit_emit_branch_target
    ret

jit_op_ireturn:
    mov al, 0x58
    call jit_emit_byte
    call jit_emit_epilogue
    ret

jit_op_return:
    call jit_emit_epilogue
    ret

jit_op_newarray:
    inc esi                     ; Saltar el byte <atype> (ej. 10 para enteros)
    
    mov al, 0x5B                ; pop ebx (Extraer la cantidad de elementos)
    call jit_emit_byte
    
    ; Calcular bytes requeridos: eax = (ebx * 4) + 4 bytes de header
    mov al, 0x89                ; mov eax, ebx
    call jit_emit_byte
    mov al, 0xD8
    call jit_emit_byte
    
    mov al, 0xC1                ; shl eax, 2 (Multiplicar por 4)
    call jit_emit_byte
    mov al, 0xE0
    call jit_emit_byte
    mov al, 0x02
    call jit_emit_byte
    
    mov al, 0x83                ; add eax, 4 (Espacio para la longitud)
    call jit_emit_byte
    mov al, 0xC0
    call jit_emit_byte
    mov al, 0x04
    call jit_emit_byte
    
    ; Obtener memoria del Heap
    mov al, 0x8B                ; mov ecx, [heap_ptr]
    call jit_emit_byte
    mov al, 0x0D
    call jit_emit_byte
    mov eax, heap_ptr
    call jit_emit_dword
    
    ; Escribir la longitud en el Header del array
    mov al, 0x89                ; mov [ecx], ebx
    call jit_emit_byte
    mov al, 0x19
    call jit_emit_byte
    
    ; Avanzar el puntero del Heap global para la próxima asignación
    mov al, 0x01                ; add [heap_ptr], eax
    call jit_emit_byte
    mov al, 0x05
    call jit_emit_byte
    mov eax, heap_ptr
    call jit_emit_dword
    
    ; Devolver la dirección base del nuevo array a Java
    mov al, 0x51                ; push ecx
    call jit_emit_byte
    ret

jit_op_lload:
jit_op_dload:
    movzx ebx, byte [esi]
    inc esi
    
    shl ebx, 2
    add ebx, 16
    neg ebx
    
    mov al, 0x8B
    call jit_emit_byte
    mov al, 0x45
    call jit_emit_byte
    mov al, bl
    call jit_emit_byte
    mov al, 0x50
    call jit_emit_byte
    
    mov al, 0x8B
    call jit_emit_byte
    mov al, 0x45
    call jit_emit_byte
    mov eax, ebx
    sub eax, 4
    call jit_emit_byte
    mov al, 0x50
    call jit_emit_byte
    ret

jit_op_lstore:
jit_op_dstore:
    movzx ebx, byte [esi]
    inc esi
    
    shl ebx, 2
    add ebx, 16
    neg ebx
    
    mov al, 0x58
    call jit_emit_byte
    mov al, 0x89
    call jit_emit_byte
    mov al, 0x45
    call jit_emit_byte
    mov eax, ebx
    sub eax, 4
    call jit_emit_byte
    
    mov al, 0x58
    call jit_emit_byte
    mov al, 0x89
    call jit_emit_byte
    mov al, 0x45
    call jit_emit_byte
    mov al, bl
    call jit_emit_byte
    ret

jit_op_dload_2:
    mov al, 0x8B
    call jit_emit_byte
    mov al, 0x45
    call jit_emit_byte
    mov al, 0xE8                ; [ebp-24]
    call jit_emit_byte
    mov al, 0x50
    call jit_emit_byte
    mov al, 0x8B
    call jit_emit_byte
    mov al, 0x45
    call jit_emit_byte
    mov al, 0xE4                ; [ebp-28]
    call jit_emit_byte
    mov al, 0x50
    call jit_emit_byte
    ret

jit_op_dload_3:
    mov al, 0x8B
    call jit_emit_byte
    mov al, 0x45
    call jit_emit_byte
    mov al, 0xE4                ; [ebp-28]
    call jit_emit_byte
    mov al, 0x50
    call jit_emit_byte
    mov al, 0x8B
    call jit_emit_byte
    mov al, 0x45
    call jit_emit_byte
    mov al, 0xE0                ; [ebp-32]
    call jit_emit_byte
    mov al, 0x50
    call jit_emit_byte
    ret

jit_op_laload:
    jmp jit_op_iaload

jit_op_baload:
    mov al, 0x59                ; pop ecx (index)
    call jit_emit_byte
    mov al, 0x5B                ; pop ebx (arrayref)
    call jit_emit_byte
    mov al, 0x0F
    call jit_emit_byte
    mov al, 0xBE                ; movsx eax, byte [ebx + ecx + 4]
    call jit_emit_byte
    mov al, 0x44                ; ModRM: mod=01 (Permite desplazamiento disp8)
    call jit_emit_byte
    mov al, 0x0B                ; SIB: [ebx + ecx]
    call jit_emit_byte
    mov al, 0x04                ; disp8: +4 (Saltar cabecera)
    call jit_emit_byte
    mov al, 0x50                ; push eax
    call jit_emit_byte
    ret

jit_op_caload:
    mov al, 0x59                ; pop ecx
    call jit_emit_byte
    mov al, 0x5B                ; pop ebx
    call jit_emit_byte
    mov al, 0x0F
    call jit_emit_byte
    mov al, 0xB7                ; movzx eax, word [ebx + ecx*2 + 4]
    call jit_emit_byte
    mov al, 0x44                ; ModRM: mod=01
    call jit_emit_byte
    mov al, 0x4B                ; SIB: [ebx + ecx*2]
    call jit_emit_byte
    mov al, 0x04                ; disp8: +4
    call jit_emit_byte
    mov al, 0x50
    call jit_emit_byte
    ret

jit_op_iflt:
    mov al, 0x58
    call jit_emit_byte
    mov al, 0x83
    call jit_emit_byte
    mov al, 0xF8
    call jit_emit_byte
    mov al, 0x00
    call jit_emit_byte
    mov al, 0x0F
    call jit_emit_byte
    mov al, 0x8C
    call jit_emit_byte
    call jit_emit_branch_target
    ret

jit_op_ifgt:
    mov al, 0x58
    call jit_emit_byte
    mov al, 0x83
    call jit_emit_byte
    mov al, 0xF8
    call jit_emit_byte
    mov al, 0x00
    call jit_emit_byte
    mov al, 0x0F
    call jit_emit_byte
    mov al, 0x8F
    call jit_emit_byte
    call jit_emit_branch_target
    ret

jit_op_anewarray:
    add esi, 2
    jmp jit_op_newarray

jit_op_lneg:
jit_op_fneg:
jit_op_dneg:
    jmp jit_op_ineg

jit_op_i2f:
    mov al, 0xDB
    call jit_emit_byte
    mov al, 0x04
    call jit_emit_byte
    mov al, 0x24
    call jit_emit_byte

    mov al, 0xD9
    call jit_emit_byte
    mov al, 0x1C
    call jit_emit_byte
    mov al, 0x24
    call jit_emit_byte
    ret

jit_op_f2i:
    mov al, 0xD9
    call jit_emit_byte
    mov al, 0x04
    call jit_emit_byte
    mov al, 0x24
    call jit_emit_byte

    mov al, 0xDB
    call jit_emit_byte
    mov al, 0x1C
    call jit_emit_byte
    mov al, 0x24
    call jit_emit_byte
    ret

jit_op_i2d:
jit_op_l2f:
jit_op_l2d:
jit_op_f2l:
jit_op_f2d:
jit_op_d2i:
jit_op_d2l:
jit_op_d2f:
    ret

jit_op_i2s:
    mov al, 0x58
    call jit_emit_byte
    mov al, 0x0F
    call jit_emit_byte
    mov al, 0xBF
    call jit_emit_byte
    mov al, 0xC0
    call jit_emit_byte
    mov al, 0x50
    call jit_emit_byte
    ret

jit_op_lcmp:
    mov al, 0x5B
    call jit_emit_byte
    mov al, 0x5A
    call jit_emit_byte
    mov al, 0x59
    call jit_emit_byte
    mov al, 0x58
    call jit_emit_byte

    mov al, 0x39
    call jit_emit_byte
    mov al, 0xD0
    call jit_emit_byte

    mov al, 0x75
    call jit_emit_byte
    mov al, 0x06
    call jit_emit_byte

    mov al, 0x39
    call jit_emit_byte
    mov al, 0xD9
    call jit_emit_byte

.cmp_done:
    mov al, 0x0F
    call jit_emit_byte
    mov al, 0x9F
    call jit_emit_byte
    mov al, 0xC1
    call jit_emit_byte

    mov al, 0x0F
    call jit_emit_byte
    mov al, 0x9C
    call jit_emit_byte
    mov al, 0xC2
    call jit_emit_byte

    mov al, 0x0F
    call jit_emit_byte
    mov al, 0xB6
    call jit_emit_byte
    mov al, 0xC1
    call jit_emit_byte

    mov al, 0x0F
    call jit_emit_byte
    mov al, 0xB6
    call jit_emit_byte
    mov al, 0xD2
    call jit_emit_byte

    mov al, 0x29
    call jit_emit_byte
    mov al, 0xD0
    call jit_emit_byte

    mov al, 0x50
    call jit_emit_byte
    ret

jit_emit_fcom_routine:
    push eax

    mov al, 0xD9
    call jit_emit_byte
    mov al, 0x44
    call jit_emit_byte
    mov al, 0x24
    call jit_emit_byte
    mov al, 0x04
    call jit_emit_byte

    mov al, 0xD9
    call jit_emit_byte
    mov al, 0x04
    call jit_emit_byte
    mov al, 0x24
    call jit_emit_byte

    mov al, 0xD9
    call jit_emit_byte
    mov al, 0xD9
    call jit_emit_byte

    mov al, 0xDF
    call jit_emit_byte
    mov al, 0xE0
    call jit_emit_byte

    mov al, 0x9E
    call jit_emit_byte

    mov al, 0x83
    call jit_emit_byte
    mov al, 0xC4
    call jit_emit_byte
    mov al, 0x08
    call jit_emit_byte

    pop eax
    mov cl, al

    mov al, 0x7A
    call jit_emit_byte
    mov al, 0x0A
    call jit_emit_byte

    mov al, 0x0F
    call jit_emit_byte
    mov al, 0x97
    call jit_emit_byte
    mov al, 0xC0
    call jit_emit_byte

    mov al, 0x0F
    call jit_emit_byte
    mov al, 0x92
    call jit_emit_byte
    mov al, 0xD2
    call jit_emit_byte

    mov al, 0x0F
    call jit_emit_byte
    mov al, 0xB6
    call jit_emit_byte
    mov al, 0xC0
    call jit_emit_byte

    mov al, 0x0F
    call jit_emit_byte
    mov al, 0xB6
    call jit_emit_byte
    mov al, 0xD2
    call jit_emit_byte

    mov al, 0x29
    call jit_emit_byte
    mov al, 0xD0
    call jit_emit_byte

    mov al, 0xEB
    call jit_emit_byte
    mov al, 0x02
    call jit_emit_byte

    mov al, 0x0F
    call jit_emit_byte
    mov al, 0xBE
    call jit_emit_byte
    mov al, 0xC1
    call jit_emit_byte

    mov al, 0x50
    call jit_emit_byte
    ret

jit_op_fcmpl:
    mov al, -1
    call jit_emit_fcom_routine
    ret

jit_op_fcmpg:
    mov al, 1
    call jit_emit_fcom_routine
    ret

jit_op_dcmpl:
    mov al, 0x83
    call jit_emit_byte
    mov al, 0xC4
    call jit_emit_byte
    mov al, 0x08
    call jit_emit_byte
    jmp jit_op_fcmpl

jit_op_dcmpg:
    mov al, 0x83
    call jit_emit_byte
    mov al, 0xC4
    call jit_emit_byte
    mov al, 0x08
    call jit_emit_byte
    jmp jit_op_fcmpg

jit_op_if_acmpeq:
    jmp jit_op_if_icmpeq

jit_op_if_acmpne:
    jmp jit_op_if_icmpne

jit_op_ifnull:
    jmp jit_op_ifeq

jit_op_ifnonnull:
    jmp jit_op_ifne

jit_op_goto_w:
    mov eax, [esi]
    bswap eax
    add esi, 4
    ; eax = signed int32 offset
    mov ecx, esi
    sub ecx, [jit_bytecode_base]
    sub ecx, 5
    add ecx, eax
    ; E9 rel32
    mov al, 0xE9
    call jit_emit_byte
    ; Guardar fixup
    mov edx, [fixup_count]
    mov ebx, [jit_buffer_ptr]
    mov [fixup_addr + edx * 4], ebx
    mov [fixup_target + edx * 4], ecx
    inc edx
    mov [fixup_count], edx
    xor eax, eax
    call jit_emit_dword
    ret

jit_op_jsr:
    movzx eax, byte [esi]
    inc esi
    movzx ebx, byte [esi]
    inc esi
    shl eax, 8
    or eax, ebx
    movsx eax, ax
    ; return_pc = ESI - jit_bytecode_base
    mov ecx, esi
    sub ecx, [jit_bytecode_base]
    ; push native_address_of(return_pc)
    mov al, 0x68
    call jit_emit_byte
    mov edx, [fixup_count]
    mov ebx, [jit_buffer_ptr]
    mov [fixup_addr + edx * 4], ebx
    mov [fixup_target + edx * 4], ecx
    inc edx
    mov [fixup_count], edx
    xor eax, eax
    call jit_emit_dword
    ; target = opcode_pc + offset
    ; opcode_pc = ESI - base - 3
    mov ecx, esi
    sub ecx, [jit_bytecode_base]
    sub ecx, 3
    add ecx, eax
    ; E9 rel32
    mov al, 0xE9
    call jit_emit_byte
    mov edx, [fixup_count]
    mov ebx, [jit_buffer_ptr]
    mov [fixup_addr + edx * 4], ebx
    mov [fixup_target + edx * 4], ecx
    inc edx
    mov [fixup_count], edx
    xor eax, eax
    call jit_emit_dword
    ret

jit_op_jsr_w:
    mov eax, esi
    sub eax, [jit_bytecode_base]
    add eax, 4

    push eax
    mov al, 0x68
    call jit_emit_byte
    pop eax
    call jit_emit_dword

    mov al, 0xE9
    call jit_emit_byte
    call jit_emit_branch_target
    ret

jit_op_ret:
    movzx ebx, byte [esi]
    inc esi
    shl ebx, 2
    add ebx, 16
    neg ebx
    
    mov al, 0x8B            ; mov eax, [ebp + disp32]
    call jit_emit_byte
    mov al, 0x85
    call jit_emit_byte
    mov eax, ebx
    call jit_emit_dword
    
    mov al, 0xFF            ; jmp eax
    call jit_emit_byte
    mov al, 0xE0
    call jit_emit_byte
    ret

jit_op_tableswitch:
    mov eax, esi
    sub eax, [jit_bytecode_base]
    and eax, 3
    jz .ts_aligned
    neg eax
    add eax, 4
    add esi, eax

.ts_aligned:
    mov eax, [esi]
    bswap eax
    add esi, 4
    mov ebx, [esi]
    bswap ebx
    add esi, 4
    mov ecx, [esi]
    bswap ecx
    add esi, 4

    mov al, 0x58
    call jit_emit_byte

    mov al, 0x3D
    call jit_emit_byte
    mov eax, ebx
    call jit_emit_dword

    mov al, 0x0F
    call jit_emit_byte
    mov al, 0x8C
    call jit_emit_byte
    push eax
    imul eax, eax, 5
    call jit_emit_dword
    pop eax

    mov al, 0x3D
    call jit_emit_byte
    mov eax, ecx
    call jit_emit_dword

    mov al, 0x0F
    call jit_emit_byte
    mov al, 0x8F
    call jit_emit_byte
    push eax
    imul eax, eax, 5
    call jit_emit_dword
    pop eax

    sub ecx, ebx
    inc ecx
    shl ecx, 2
    add esi, ecx
    ret

jit_op_lookupswitch:
    mov eax, esi
    sub eax, [jit_bytecode_base]
    and eax, 3
    jz .ls_aligned
    neg eax
    add eax, 4
    add esi, eax

.ls_aligned:
    mov eax, [esi]
    bswap eax
    add esi, 4
    mov ecx, [esi]
    bswap ecx
    add esi, 4

    mov al, 0x58
    call jit_emit_byte

.ls_loop:
    test ecx, ecx
    jz .ls_done

    mov ebx, [esi]
    bswap ebx
    add esi, 4
    mov edx, [esi]
    bswap edx
    add esi, 4

    mov al, 0x3D
    call jit_emit_byte
    mov eax, ebx
    call jit_emit_dword

    mov al, 0x0F
    call jit_emit_byte
    mov al, 0x84
    call jit_emit_byte
    push eax
    mov eax, edx
    imul eax, eax, 5
    call jit_emit_dword
    pop eax

    dec ecx
    jmp .ls_loop

.ls_done:
    ret	

jit_op_lreturn:
jit_op_freturn:
jit_op_dreturn:
jit_op_areturn:
    jmp jit_op_ireturn

jit_op_athrow:
    ; Extraer el objeto Throwable
    mov al, 0x58                ; pop eax
    call jit_emit_byte

    ; Emitir CLI (Apagar interrupciones)
    mov al, 0xFA
    call jit_emit_byte

    ; Emitir HLT (Congelar CPU)
    mov al, 0xF4
    call jit_emit_byte
    ret

jit_op_unsupported:
    movzx eax, byte [esi - 1]

    mov ebx, eax
    shr ebx, 4
    call .nibble_to_hex
    mov [hex_byte_str], bl

    mov ebx, eax
    and ebx, 0x0F
    call .nibble_to_hex
    mov [hex_byte_str + 1], bl

    push msg_panic_head
    call sys_serial_puts
    add esp, 4

    push hex_byte_str
    call sys_serial_puts
    add esp, 4

    cli
    hlt

.nibble_to_hex:
    cmp bl, 9
    jbe .is_digit
    add bl, 7
.is_digit:
    add bl, '0'
    ret

jit_emit_branch_target:
    movzx eax, byte [esi]       
    inc esi
    movzx ebx, byte [esi]       
    inc esi
    shl eax, 8
    or eax, ebx
    movsx eax, ax               ; EAX = Offset relativo en bytecode (int16 signed)

    mov ecx, esi
    sub ecx, 3                  ; Retrocero al inicio de la instrucción de salto
    sub ecx, [jit_bytecode_base]
    add ecx, eax                ; ECX = PC destino en Bytecode

    cmp eax, 0
    jl .backwards

.forward:
    mov edx, [fixup_count]
    mov ebx, [jit_buffer_ptr]
    mov [fixup_addr + edx * 4], ebx
    mov [fixup_target + edx * 4], ecx
    
    inc edx
    mov [fixup_count], edx
    
    xor eax, eax
    call jit_emit_dword
    ret

.backwards:
    mov eax, [pc_map + ecx * 4]
    mov ebx, [jit_buffer_ptr]
    add ebx, 4
    sub eax, ebx
    call jit_emit_dword
    ret

jit_test_phase1:
    mov eax, 0x00200000
    mov ebx, 65536
    call jit_init
    mov al, 0x90
    call jit_emit_byte
    mov al, 0xB8
    call jit_emit_byte
    mov eax, 0x12345678
    call jit_emit_dword
    mov al, 0xC3
    call jit_emit_byte

    mov eax, [jit_buffer_base]
    call eax
    ret

jit_test_phase2:
    mov esi, test_bytecode_p2
    mov ecx, 3
    call jit_compile_method
    call eax
    ret

jit_test_phase3:
    mov esi, test_bytecode_p3
    mov ecx, 8
    call jit_compile_method
    call eax
    ret

jit_test_phase4:
    mov esi, test_bytecode_p4
    mov ecx, 10
    call jit_compile_method
    call eax
    ret

jit_test_phase5:
    mov dword [jit_opcode_table + 0x1A * 4], jit_op_iload_param_0
    mov dword [jit_opcode_table + 0x1B * 4], jit_op_iload_param_1

    mov esi, test_bytecode_p5
    mov ecx, 4
    call jit_compile_method
    push dword 60
    push dword 40
    call eax
    add esp, 8

    mov dword [jit_opcode_table + 0x1A * 4], jit_op_iload_0
    mov dword [jit_opcode_table + 0x1B * 4], jit_op_iload_1
    ret

jit_test_phase6:    
    mov esi, test_bytecode_p6
    mov ecx, 17
    call jit_compile_method
    call eax
    ret

jit_execute_method:
    push ebp
    mov ebp, esp
    push ebx
    push ecx
    push edx

    test ecx, ecx
    jnz .has_len
    mov ecx, 4096
.has_len:

    call jit_compile_method
    call eax

    pop edx
    pop ecx
    pop ebx
    mov esp, ebp
    pop ebp
    ret

section .rodata
align 4

msg_err_unsupported_op: db 13, 10, "[JIT Panic] Unsupported Opcode encountered!", 13, 10, 0
msg_panic_head:         db 13, 10, "[JIT Panic] Unsupported Opcode: 0x", 0
hex_byte_str:           db "00!", 13, 10, 0

section .rodata
align 4
jit_opcode_table:
    dd jit_op_nop                   ; 0x00
    dd jit_op_aconst_null           ; 0x01
    dd jit_op_iconst_m1             ; 0x02
    dd jit_op_iconst_0              ; 0x03
    dd jit_op_iconst_1              ; 0x04
    dd jit_op_iconst_2              ; 0x05
    dd jit_op_iconst_3              ; 0x06
    dd jit_op_iconst_4              ; 0x07
    dd jit_op_iconst_5              ; 0x08
    dd jit_op_lconst_0              ; 0x09
    dd jit_op_lconst_1              ; 0x0A
    dd jit_op_fconst_0              ; 0x0B
    dd jit_op_fconst_1              ; 0x0C
    dd jit_op_fconst_2              ; 0x0D
    dd jit_op_dconst_0              ; 0x0E
    dd jit_op_dconst_1              ; 0x0F
    dd jit_op_bipush                ; 0x10
    dd jit_op_sipush                ; 0x11
    dd jit_op_ldc                   ; 0x12
    dd jit_op_ldc_w                 ; 0x13
    dd jit_op_ldc2_w                ; 0x14
    dd jit_op_iload                 ; 0x15
    dd jit_op_lload                 ; 0x16
    dd jit_op_fload                 ; 0x17
    dd jit_op_dload                 ; 0x18
    dd jit_op_aload                 ; 0x19
    dd jit_op_iload_0               ; 0x1A
    dd jit_op_iload_1               ; 0x1B
    dd jit_op_iload_2               ; 0x1C
    dd jit_op_iload_3               ; 0x1D
    dd jit_op_lload_0               ; 0x1E
    dd jit_op_lload_1               ; 0x1F
    dd jit_op_unsupported           ; 0x20
    dd jit_op_unsupported           ; 0x21
    dd jit_op_fload_0               ; 0x22
    dd jit_op_fload_1               ; 0x23
    dd jit_op_fload_2               ; 0x24
    dd jit_op_fload_3               ; 0x25
    dd jit_op_dload_0               ; 0x26
    dd jit_op_dload_1               ; 0x27
    dd jit_op_dload_2               ; 0x28
    dd jit_op_dload_3               ; 0x29
    dd jit_op_aload_0               ; 0x2A
    dd jit_op_aload_1               ; 0x2B
    dd jit_op_aload_2               ; 0x2C
    dd jit_op_aload_3               ; 0x2D
    dd jit_op_iaload                ; 0x2E
    dd jit_op_laload                ; 0x2F
    dd jit_op_iaload                ; 0x30 (faload alias)
    dd jit_op_laload                ; 0x31 (daload alias)
    dd jit_op_aaload                ; 0x32
    dd jit_op_baload                ; 0x33
    dd jit_op_caload                ; 0x34
    dd jit_op_saload                ; 0x35
    dd jit_op_istore                ; 0x36
    dd jit_op_lstore                ; 0x37
    dd jit_op_fstore                ; 0x38
    dd jit_op_dstore                ; 0x39
    dd jit_op_astore                ; 0x3A
    dd jit_op_istore_0              ; 0x3B
    dd jit_op_istore_1              ; 0x3C
    dd jit_op_istore_2              ; 0x3D
    dd jit_op_istore_3              ; 0x3E
    dd jit_op_lstore_0              ; 0x3F
    dd jit_op_lstore_1              ; 0x40
    dd jit_op_unsupported           ; 0x41
    dd jit_op_unsupported           ; 0x42
    dd jit_op_istore_0              ; 0x43 (fstore_0 alias)
    dd jit_op_istore_1              ; 0x44
    dd jit_op_istore_2              ; 0x45
    dd jit_op_istore_3              ; 0x46
    dd jit_op_lstore_0              ; 0x47 (dstore_0 alias)
    dd jit_op_lstore_1              ; 0x48
    dd jit_op_unsupported           ; 0x49
    dd jit_op_unsupported           ; 0x4A
    dd jit_op_astore_0              ; 0x4B
    dd jit_op_astore_1              ; 0x4C
    dd jit_op_astore_2              ; 0x4D
    dd jit_op_astore_3              ; 0x4E
    dd jit_op_iastore               ; 0x4F
    dd jit_op_iastore               ; 0x50 (lastore alias)
    dd jit_op_iastore               ; 0x51 (fastore alias)
    dd jit_op_iastore               ; 0x52 (dastore alias)
    dd jit_op_aastore               ; 0x53
    dd jit_op_bastore               ; 0x54
    dd jit_op_castore               ; 0x55
    dd jit_op_sastore               ; 0x56
    dd jit_op_pop                   ; 0x57
    dd jit_op_pop                   ; 0x58
    dd jit_op_dup                   ; 0x59
    dd jit_op_dup_x1                ; 0x5A
    dd jit_op_dup_x2                ; 0x5B
    dd jit_op_dup2                  ; 0x5C
    dd jit_op_dup2_x1               ; 0x5D
    dd jit_op_dup2_x2               ; 0x5E
    dd jit_op_swap                  ; 0x5F
    dd jit_op_iadd                  ; 0x60
    dd jit_op_iadd                  ; 0x61
    dd jit_op_iadd                  ; 0x62
    dd jit_op_iadd                  ; 0x63
    dd jit_op_isub                  ; 0x64
    dd jit_op_isub                  ; 0x65
    dd jit_op_isub                  ; 0x66
    dd jit_op_isub                  ; 0x67
    dd jit_op_imul                  ; 0x68
    dd jit_op_imul                  ; 0x69
    dd jit_op_imul                  ; 0x6A
    dd jit_op_imul                  ; 0x6B
    dd jit_op_idiv                  ; 0x6C
    dd jit_op_idiv                  ; 0x6D
    dd jit_op_idiv                  ; 0x6E
    dd jit_op_idiv                  ; 0x6F
    dd jit_op_irem                  ; 0x70
    dd jit_op_irem                  ; 0x71
    dd jit_op_irem                  ; 0x72
    dd jit_op_irem                  ; 0x73
    dd jit_op_ineg                  ; 0x74
    dd jit_op_lneg                  ; 0x75
    dd jit_op_fneg                  ; 0x76
    dd jit_op_dneg                  ; 0x77
    dd jit_op_ishl                  ; 0x78
    dd jit_op_ishl                  ; 0x79
    dd jit_op_ishr                  ; 0x7A
    dd jit_op_ishr                  ; 0x7B
    dd jit_op_iushr                 ; 0x7C
    dd jit_op_iushr                 ; 0x7D
    dd jit_op_iand                  ; 0x7E
    dd jit_op_iand                  ; 0x7F
    dd jit_op_ior                   ; 0x80
    dd jit_op_ior                   ; 0x81
    dd jit_op_ixor                  ; 0x82
    dd jit_op_ixor                  ; 0x83
    dd jit_op_iinc                  ; 0x84
    dd jit_op_i2l                   ; 0x85
    dd jit_op_i2f                   ; 0x86
    dd jit_op_i2d                   ; 0x87
    dd jit_op_l2i                   ; 0x88
    dd jit_op_l2f                   ; 0x89
    dd jit_op_l2d                   ; 0x8A
    dd jit_op_f2i                   ; 0x8B
    dd jit_op_f2l                   ; 0x8C
    dd jit_op_f2d                   ; 0x8D
    dd jit_op_d2i                   ; 0x8E
    dd jit_op_d2l                   ; 0x8F
    dd jit_op_d2f                   ; 0x90
    dd jit_op_i2b                   ; 0x91
    dd jit_op_i2c                   ; 0x92
    dd jit_op_i2s                   ; 0x93
    dd jit_op_lcmp                  ; 0x94
    dd jit_op_fcmpl                 ; 0x95
    dd jit_op_fcmpg                 ; 0x96
    dd jit_op_dcmpl                 ; 0x97
    dd jit_op_dcmpg                 ; 0x98
    dd jit_op_ifeq                  ; 0x99
    dd jit_op_ifne                  ; 0x9A
    dd jit_op_iflt                  ; 0x9B
    dd jit_op_ifge                  ; 0x9C
    dd jit_op_ifgt                  ; 0x9D
    dd jit_op_ifle                  ; 0x9E
    dd jit_op_if_icmpeq             ; 0x9F
    dd jit_op_if_icmpne             ; 0xA0
    dd jit_op_if_icmplt             ; 0xA1
    dd jit_op_if_icmpge             ; 0xA2
    dd jit_op_if_icmpgt             ; 0xA3
    dd jit_op_if_icmple             ; 0xA4
    dd jit_op_if_acmpeq             ; 0xA5
    dd jit_op_if_acmpne             ; 0xA6
    dd jit_op_goto                  ; 0xA7
    dd jit_op_jsr                   ; 0xA8
    dd jit_op_ret                   ; 0xA9
    dd jit_op_tableswitch           ; 0xAA
    dd jit_op_lookupswitch          ; 0xAB
    dd jit_op_ireturn               ; 0xAC
    dd jit_op_lreturn               ; 0xAD
    dd jit_op_freturn               ; 0xAE
    dd jit_op_dreturn               ; 0xAF
    dd jit_op_areturn               ; 0xB0
    dd jit_op_return                ; 0xB1
    dd jit_op_getstatic             ; 0xB2
    dd jit_op_putstatic             ; 0xB3
    dd jit_op_getfield              ; 0xB4
    dd jit_op_putfield              ; 0xB5
    dd jit_op_invokevirtual         ; 0xB6
    dd jit_op_invokespecial         ; 0xB7
    dd jit_op_invokestatic          ; 0xB8
    dd jit_op_invokeinterface       ; 0xB9
    dd jit_op_invokedynamic         ; 0xBA
    dd jit_op_new                   ; 0xBB
    dd jit_op_newarray              ; 0xBC
    dd jit_op_anewarray             ; 0xBD
    dd jit_op_arraylength           ; 0xBE
    dd jit_op_athrow                ; 0xBF
    dd jit_op_checkcast             ; 0xC0
    dd jit_op_instanceof            ; 0xC1
    dd jit_op_monitorenter          ; 0xC2
    dd jit_op_monitorexit           ; 0xC3
    dd jit_op_wide                  ; 0xC4
    dd jit_op_multianewarray        ; 0xC5
    dd jit_op_ifnull                ; 0xC6
    dd jit_op_ifnonnull             ; 0xC7
    dd jit_op_goto_w                ; 0xC8
    dd jit_op_jsr_w                 ; 0xC9
        
    ; Padding final para completar las 256 entradas
    times 54 dd jit_op_nop          ; 0xCA..0xFF

section .note.GNU-stack noalloc noexec nowrite progbits
