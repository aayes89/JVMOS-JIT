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

; Driver Realtek RTL8111/8168/8411 Gigabit Ethernet para Baremetal
; Copyright (c) 2026 Allan (Slam)

section .bss
alignb 256
    rtl8168_tx_ring:    resb 16 * 16      ; 16 Descriptores Tx de 16 bytes (256 bytes)
    rtl8168_rx_ring:    resb 16 * 16      ; 16 Descriptores Rx de 16 bytes (256 bytes)

alignb 16
    rtl8168_rx_buffers: resb 1    ; Puntero al Headp (4 bytes)
    rtl8168_tx_buffers: resb 1    ; Puntero al Headp (4 bytes)
    rtl8168_io_port:    resd 1    ; Puerto BAR0
    rtl8168_tx_idx:     resd 1
    rtl8168_rx_idx:     resd 1

extern sys_kalloc

section .text

; Macros y Registros
MAC0        equ 0x00    ; MAC Address
TxDescStart equ 0x20    ; Tx Descriptor Ring Physical Address
RxDescStart equ 0xE4    ; Rx Descriptor Ring Physical Address
CMD_REG     equ 0x37    ; Command Register (Renombrado para evitar conflicto en NASM)
IMR         equ 0x3C    ; Interrupt Mask Register
ISR         equ 0x3E    ; Interrupt Status Register
TCR         equ 0x40    ; Transmit Configuration Register
RCR_REG     equ 0x44    ; Receive Configuration Register
Cfg9346     equ 0x50    ; 9346CR Command Register

; Bits del Command Register (CMD_REG)
CR_RST      equ 0x10    ; Reset
CR_RE       equ 0x08    ; Receive Enable
CR_TE       equ 0x04    ; Transmit Enable

; Bits del Descriptor RTL8168
DESC_OWN    equ 0x80000000  ; Ownership bit (1 = Tarjeta, 0 = CPU)
DESC_EOR    equ 0x40000000  ; End of Ring
DESC_FS     equ 0x20000000  ; First Segment
DESC_LS     equ 0x10000000  ; Last Segment

; sys_rtl8168_init(int ioPort)
sys_rtl8168_init:
    push ebp
    mov ebp, esp
    push ebx
    
    mov eax, [ebp + 8]      ; Puerto I/O base devuelto por NetworkShell
    mov [rtl8168_io_port], eax
    mov dword [rtl8168_tx_idx], 0
    mov dword [rtl8168_rx_idx], 0
	
	; Solicitar 64KB al Heap para evitar colisiones
    push 32768
    call sys_kalloc
    add esp, 4
    mov [rtl8168_rx_buffers], eax

    push 32768
    call sys_kalloc
    add esp, 4
    mov [rtl8168_tx_buffers], eax

    ; Desbloquear configuración (Escribir 0xC0 en 0x50)
    mov dx, ax
    add dx, Cfg9346
    mov al, 0xC0
    out dx, al

    ; Software Reset
    mov dx, word [rtl8168_io_port]
    add dx, CMD_REG
    mov al, CR_RST
    out dx, al
    mov ecx, 100000
.wait_rst:
    in al, dx
    test al, CR_RST
    jz .rst_done
    loop .wait_rst
    xor eax, eax            ; Retornar 0 (Fallo por timeout)
    jmp .done
.rst_done:

    ; Inicializar Anillo Rx (16 descriptores)
    xor ecx, ecx
.init_rx:
    mov eax, 2048
    mul ecx
    add eax, [rtl8168_rx_buffers]    ; Puntero al buffer físico
    mov ebx, ecx
    shl ebx, 4                     ; ecx * 16 bytes por descriptor
    add ebx, rtl8168_rx_ring

    ; Formato Descriptor: [0-3] Conf/Len, [4-7] VLAN, [8-11] AddrLow, [12-15] AddrHigh
    mov dword [ebx + 8], eax       ; AddrLow
    mov dword [ebx + 12], 0        ; AddrHigh (32-bit baremetal)

    ; Configurar Status: OWN=1, Tamaño=2048
    mov eax, 2048
    or eax, DESC_OWN               ; Entregar a la tarjeta
    cmp ecx, 15
    jne .rx_no_eor
    or eax, DESC_EOR               ; Marcar el último descriptor del anillo
.rx_no_eor:
    mov [ebx], eax
    inc ecx
    cmp ecx, 16
    jl .init_rx

    ; Inicializar Anillo Tx (16 descriptores)
    xor ecx, ecx
.init_tx:
    mov eax, 2048
    mul ecx
    add eax, [rtl8168_tx_buffers]
    mov ebx, ecx
    shl ebx, 4
    add ebx, rtl8168_tx_ring

    mov dword [ebx + 8], eax
    mov dword [ebx + 12], 0

    mov eax, 0                     ; OWN=0 (CPU controla)
    cmp ecx, 15
    jne .tx_no_eor
    or eax, DESC_EOR
.tx_no_eor:
    mov [ebx], eax
    inc ecx
    cmp ecx, 16
    jl .init_tx

    ; Escribir direcciones base de los anillos en los registros
    mov dx, word [rtl8168_io_port]
    add dx, RxDescStart
    mov eax, rtl8168_rx_ring
    out dx, eax             ; Rx Base Low
    add dx, 4
    xor eax, eax
    out dx, eax             ; Rx Base High

    mov dx, word [rtl8168_io_port]
    add dx, TxDescStart
    mov eax, rtl8168_tx_ring
    out dx, eax             ; Tx Base Low
    add dx, 4
    xor eax, eax
    out dx, eax             ; Tx Base High

    ; Configurar RCR_REG (Receive Config) - Aceptar Bcast, Mcast, Phys y permitir wrap
    mov dx, word [rtl8168_io_port]
    add dx, RCR_REG
    mov eax, 0x0000E70F     ; DMA max burst, AAP, APM, AM, AB
    out dx, eax

    ; Configurar TCR (Transmit Config)
    mov dx, word [rtl8168_io_port]
    add dx, TCR
    mov eax, 0x03000700     ; IFG y Max DMA burst
    out dx, eax

    ; Habilitar Tx y Rx en CMD_REG
    mov dx, word [rtl8168_io_port]
    add dx, CMD_REG
    mov al, CR_TE | CR_RE
    out dx, al

    ; Bloquear configuración (Escribir 0x00 en 0x50)
    mov dx, word [rtl8168_io_port]
    add dx, Cfg9346
    mov al, 0x00
    out dx, al
	
	; Apagar interrupciones por Hardware
	mov dx, word [rtl8168_io_port]
	add dx, IMR
	xor ax, ax				; Escribir 0x0000
	out dx, ax

    mov eax, 1              ; Éxito
.done:
    pop ebx
    pop ebp
    ret

; sys_rtl8168_send_packet(byte[] array, int len)
sys_rtl8168_send_packet:
    push ebp
    mov ebp, esp
    push ebx
    push esi
    push edi

    mov esi, [ebp + 8]      ; Puntero Java
    add esi, 4              ; Evadir int length del array
    mov ecx, [ebp + 12]     ; Longitud
    mov ebx, [rtl8168_tx_idx]

    ; Encontrar el descriptor Tx actual
    mov edi, ebx
    shl edi, 4              ; Índice * 16
    add edi, rtl8168_tx_ring

    ; Verificar OWN bit (esperar a que la tarjeta lo libere si es 1)
    mov eax, [edi]
    test eax, DESC_OWN
    jnz .done_tx            ; Búfer lleno, no enviar

    ; Copiar datos al buffer asociado
    push edi
    mov eax, 2048
    imul eax, ebx
    add eax, [rtl8168_tx_buffers]
    mov edi, eax
    push ecx
    cld
    rep movsb
    pop ecx
    pop edi

    ; Preparar Word de Configuración
    mov eax, ecx            ; Longitud en los 16 bits bajos
    or eax, DESC_OWN | DESC_FS | DESC_LS  ; Entregar a tarjeta, primer y último fragmento
    
    ; Preservar el bit EOR si estamos en el último descriptor del anillo
    cmp ebx, 15
    jne .no_eor_tx
    or eax, DESC_EOR
.no_eor_tx:

    mov [edi], eax          ; Escribir al descriptor (dispara el Tx HW si Polling está activo)

    ; Avisarle a la tarjeta que hay datos nuevos (TxPoll)
    mov dx, word [rtl8168_io_port]
    add dx, 0x38            ; TxPoll Command
    mov al, 0x40            ; Normal Priority Tx
    out dx, al

    ; Rotar puntero de Tx
    inc ebx
    and ebx, 15             ; Modulo 16
    mov [rtl8168_tx_idx], ebx

.done_tx:
    xor eax, eax            ; Retorno nulo estándar
    pop edi
    pop esi
    pop ebx
    pop ebp
    ret


; sys_net_receive_packet_rtl8168(byte[] outBuffer, int maxLen)
sys_net_receive_packet_rtl8168:
    push ebp
    mov ebp, esp
    push ebx
    push esi
    push edi

    mov ebx, [rtl8168_rx_idx]
    
    ; Encontrar el descriptor Rx actual
    mov esi, ebx
    shl esi, 4              ; Índice * 16
    add esi, rtl8168_rx_ring

    ; Revisar OWN bit (Si es 1, la tarjeta aún está llenando el buffer)
    mov eax, [esi]
    test eax, DESC_OWN
    jnz .no_packet

    ; Extraer longitud recibida (14 bits bajos)
    mov ecx, eax
    and ecx, 0x3FFF         ; Mask length
    
    cmp ecx, 4
    jle .recycle            ; Ignorar basura
    sub ecx, 4              ; Quitar HW CRC para Java

    cmp ecx, 1536
    jg .recycle             ; Ignorar paquetes gigantes (Jumbo frames no soportados aquí)

    ; Copiar desde el buffer Rx al array de Java
    mov eax, 2048
    imul eax, ebx
    add eax, [rtl8168_rx_buffers]
    
    push esi                ; Guardar puntero al descriptor
    mov esi, eax            ; Origen: Buffer Físico
    mov edi, [ebp + 8]      ; Destino: Buffer Java
    add edi, 4              ; Evadir length del Array
    
    push ecx                ; Guardar longitud devuelta
    cld
    rep movsb               ; Copiar paquete
    pop eax                 ; EAX = longitud retornada a Java
    pop esi                 ; Restaurar descriptor

.recycle:
    ; Devolver descriptor a la tarjeta
    mov edx, 2048           ; Tamaño del buffer original
    or edx, DESC_OWN        ; Dar control al HW
    
    cmp ebx, 15             ; Restaurar EOR si aplica
    jne .no_eor_rx
    or edx, DESC_EOR
.no_eor_rx:

    mov [esi], edx          ; Escribir status limpio

    ; Rotar puntero Rx
    inc ebx
    and ebx, 15             ; Modulo 16
    mov [rtl8168_rx_idx], ebx

    ; Limpiar banderas ISR (Opcional, en polling agresivo no siempre es necesario)
    mov dx, word [rtl8168_io_port]
    add dx, ISR
    mov ax, 0xFFFF
    out dx, ax

    jmp .done_rx

.no_packet:
    xor eax, eax            ; Retornar 0

.done_rx:
    pop edi
    pop esi
    pop ebx
    pop ebp
    ret
	
section .note.GNU-stack noalloc noexec nowrite progbits	
