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

; Driver Realtek RTL8139 para QEMU / Baremetal

section .bss
alignb 16
rtl8139_io_port: resd 1
rtl8139_tx_ptr:  resd 1
rtl8139_rx_ptr:  resd 1

alignb 16
rx_buffer:       resb 8192 + 16 + 1500 

section .text

; Inicialización
sys_rtl8139_init:
    push ebp
    mov ebp, esp
    mov ax, [ebp + 8]
    mov [rtl8139_io_port], ax
    mov dword [rtl8139_tx_ptr], 0
    mov dword [rtl8139_rx_ptr], 0

    ; Despertar la tarjeta (Power On)
    mov dx, ax
    add dx, 0x52
    mov al, 0x00
    out dx, al

    ; Software Reset
    mov dx, [rtl8139_io_port]
    add dx, 0x37
    mov al, 0x10
    out dx, al
    mov ecx, 100000         ; Contador de Timeout (100ms)
.wait_rst:
    in al, dx
    test al, 0x10
    jz .rst_done            ; si el bit 4 es 0, el reset terminó
    loop .wait_rst
    
    xor eax, eax            ; Devolver 0 (Fallo)
    jmp .exit

.rst_done:
    ; Configurar inicio del buffer RX (RBSTART - 0x30)
    mov dx, [rtl8139_io_port]
    add dx, 0x30
    mov eax, rx_buffer
    out dx, eax

    ; Configurar RCR (0x44): Aceptar Broadcast,
    ; Physical Match y Multicast (0x8F) + Wrap (bit 7 = 0)
    mov dx, [rtl8139_io_port]
    add dx, 0x44
    mov eax, 0x0000008F
    out dx, eax

    ; Habilitar Rx y Tx (Command Reg 0x37 = 0x0C)
    mov dx, [rtl8139_io_port]
    add dx, 0x37
    mov al, 0x0C
    out dx, al

    mov eax, 1              ; Devolver 1 (éxito)
.exit:  
    pop ebp
    ret

; Transmisión
sys_rtl8139_send_packet:
    push ebp
    mov ebp, esp
    push ebx
    push esi

    mov esi, [ebp + 8]   ; Puntero al byte[] de Java
    add esi, 4           ; Saltar los 4 bytes de longitud del JIT
    
    mov ecx, [ebp + 12]  ; Longitud enviada

    mov ebx, [rtl8139_tx_ptr]

    ; Apuntar DMA a los datos puros
    movzx edx, word [rtl8139_io_port]
    add edx, 0x20
    lea edx, [edx + ebx * 4]
    mov eax, esi
    out dx, eax

    ; Iniciar envío
    movzx edx, word [rtl8139_io_port]
    add edx, 0x10
    lea edx, [edx + ebx * 4]
    mov eax, ecx
    and eax, 0x0FFF
    out dx, eax

    ; Rotar descriptores (4 descriptores de TX)
    inc ebx
    and ebx, 3
    mov [rtl8139_tx_ptr], ebx

    pop esi
    pop ebx
    pop ebp
    ret

; Recepción
sys_net_receive_packet:
    push ebp
    mov ebp, esp
    push ebx
    push esi
    push edi

    ; Chequear buffer
    mov dx, [rtl8139_io_port]
    add dx, 0x37
    in al, dx
    test al, 0x01
    jnz .no_packet

    mov ebx, [rtl8139_rx_ptr]
    mov esi, rx_buffer
    add esi, ebx

    ; Extraer longitud del paquete de la cabecera RTL8139
    movzx ecx, word [esi + 2]

    mov edi, [ebp + 8]   ; Puntero al byte[] destino en Java
    add edi, 4           ; Escribir justo después de la longitud

    sub ecx, 4           ; Quitar CRC de hardware
    push ecx
    add esi, 4           ; Saltar cabecera HW (Status + Longitud)
    
    cld                  
    rep movsb            ; Copiar a la RAM de Java
    pop eax              ; Retornar la longitud en EAX

    ; Actualizar anillo RX
    add ebx, eax
    add ebx, 8           ; Compensar cabecera y CRC
    add ebx, 3           ; Alineación a 4 bytes (DWORD)
    and ebx, ~3
    cmp ebx, 8192
    jl .no_wrap
    sub ebx, 8192
.no_wrap:
    mov [rtl8139_rx_ptr], ebx

    ; Notificar al router HW que el búfer fue leído
    mov dx, [rtl8139_io_port]
    add dx, 0x38
    mov eax, ebx
    sub eax, 16
    out dx, ax

    ; Limpiar interrupciones
    mov dx, [rtl8139_io_port]
    add dx, 0x3E
    mov ax, 0x05
    out dx, ax

    jmp .done

.no_packet:
    xor eax, eax

.done:
    pop edi
    pop esi
    pop ebx
    pop ebp
    ret


section .note.GNU-stack noalloc noexec nowrite progbits
