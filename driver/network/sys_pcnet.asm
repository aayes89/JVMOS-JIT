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

; Driver PCnet-PCI II/III para VirtualBox
; AMD Am79C970A / Am79C973
; Primera etapa: reset y detección

section .bss
align 16
pcnet_io_port:	resd 1
pcnet_rx_idx:	resd 1
pcnet_tx_idx:	resd 1

align 16
pcnet_init_block: resb 28	       ; bloque de inicialización 32-bit (28 bytes)
pcnet_rx_ring:    resb 16 * 4      ; 4 descriptores Rx (64 bytes)
pcnet_tx_ring:    resb 16 * 4      ; 4 descriptores Tx (64 bytes)
pcnet_rx_buffers: resb 1536 * 4    ; Buffers Rx (6 KB)
pcnet_tx_buffers: resb 1536 * 4    ; Buffers Tx (6 KB)

section .text
; Registros PCnet
PCNET_RDP      equ 0x10
PCNET_RAP      equ 0x14
PCNET_RESET    equ 0x18
PCNET_BDP      equ 0x1C

; CSR
;CSR0           equ 0
CSR0_STOP      equ 0x0004

; BCR
;BCR18          equ 18
BCR18_DWIO     equ 0x0080

; Inicializa la tarjeta PCnet
sys_pcnet_init:
    push ebp
    mov ebp, esp
	push ebx

    mov eax, [ebp + 8]			; Puerto I/O base
    mov [pcnet_io_port], eax
	mov dword [pcnet_rx_idx], 0
	mov dword [pcnet_tx_idx], 0

    ; Reset de la tarjeta
    mov edx, eax
    add edx, PCNET_RESET
    in eax, dx

	; Forzar modo I/O 32 bits (DWIO)
	mov edx, [pcnet_io_port]
	add edx, PCNET_RDP
	xor eax, eax
	out dx, eax
	
    ; Cambiar SWSTYLE a 2 estructuras de 32bits en RAM con BCR20
    mov edx, [pcnet_io_port]
    add edx, PCNET_RAP
	mov eax, 20
	out dx, eax    
    mov edx, [pcnet_io_port]
    add edx, PCNET_BDP
	mov eax, 0x0102			; SSIZE32 = 1, SWSTYLE = 2
	out dx, eax
	
	; Inicializar descriptores Rx y Tx
	mov ecx, 0
.init_rx_loop:
    mov eax, 1536
    mul ecx
    add eax, pcnet_rx_buffers
    mov ebx, ecx
    shl ebx, 4
    add ebx, pcnet_rx_ring
    mov [ebx], eax                   ; Buffer addr
    mov dword [ebx + 4], 0x8000FA00  ; OWN (0x8000) | ONES (0xF) | BCNT (-1536)
    mov dword [ebx + 8], 0
    inc ecx
    cmp ecx, 4
    jl .init_rx_loop

    mov ecx, 0
.init_tx_loop:
    mov eax, 1536
    mul ecx
    add eax, pcnet_tx_buffers
    mov ebx, ecx
    shl ebx, 4
    add ebx, pcnet_tx_ring
    mov [ebx], eax                   ; Buffer addr
    mov dword [ebx + 4], 0           ; No OWN aún
    mov dword [ebx + 8], 0
    inc ecx
    cmp ecx, 4
    jl .init_tx_loop

    ; Llenar Init Block
    mov word [pcnet_init_block], 0x0000      ; Mode
    mov word [pcnet_init_block + 2], 0x2020  ; RLEN=2 (4 desc), TLEN=2 (4 desc)
    
    ; Leer MAC de APROM y guardar en Init Block
    mov edx, [pcnet_io_port]
    in eax, dx
    mov [pcnet_init_block + 4], eax
    add edx, 4
    in eax, dx
    mov [pcnet_init_block + 8], ax
    
    mov dword [pcnet_init_block + 12], 0     ; Filtro Multicast bajo
    mov dword [pcnet_init_block + 16], 0     ; Filtro Multicast alto
    mov dword [pcnet_init_block + 20], pcnet_rx_ring
    mov dword [pcnet_init_block + 24], pcnet_tx_ring

    ; Pasar Init Block a la tarjeta (CSR1 y CSR2)
    mov edx, [pcnet_io_port]
    add edx, PCNET_RAP
    mov eax, 1
    out dx, eax
    mov edx, [pcnet_io_port]
    add edx, PCNET_RDP
    mov eax, pcnet_init_block
    and eax, 0xFFFF
    out dx, eax

    mov edx, [pcnet_io_port]
    add edx, PCNET_RAP
    mov eax, 2
    out dx, eax
    mov edx, [pcnet_io_port]
    add edx, PCNET_RDP
    mov eax, pcnet_init_block
    shr eax, 16
    out dx, eax

    ; Iniciar la Tarjeta (INIT -> Wait -> STRT)
    mov edx, [pcnet_io_port]
    add edx, PCNET_RAP
    mov eax, 0
    out dx, eax
    mov edx, [pcnet_io_port]
    add edx, PCNET_RDP
    mov eax, 1               ; Comando INIT
    out dx, eax
    
    mov ecx, 10000
.wait_init:
    in eax, dx
    test eax, 0x0100         ; Esperar IDON (Bit 8)
    jnz .start_mac
    loop .wait_init
    
    xor eax, eax             ; Falló el Init
    jmp .done

.start_mac:
    mov eax, 0x0102          ; Limpiar IDON y enviar STRT (Bit 1)
    out dx, eax
    mov eax, 1               ; Éxito

.done:
    pop ebx
    pop ebp
    ret

; Transmitir
sys_pcnet_send_packet:
    push ebp
    mov ebp, esp
    push ebx
    push esi
    push edi

    mov esi, [ebp + 8]       ; byte[] origen en Java
    add esi, 4               ; Saltar longitud de Java Header
    mov ecx, [ebp + 12]      ; Longitud a enviar

    mov ebx, [pcnet_tx_idx]

    ; Copiar payload a pcnet_tx_buffers
    mov eax, 1536
    mul ebx
    add eax, pcnet_tx_buffers
    mov edi, eax
    
    push ecx
    cld
    rep movsb
    pop ecx

    ; Preparar Status: OWN(bit 31), STP(bit 25), ENP(bit 24), ONES(bits 12-15) y BCNT
    mov eax, ecx
    neg eax
    and eax, 0x0FFF
    or eax, 0x8300F000
    
    mov edi, ebx
    shl edi, 4
    add edi, pcnet_tx_ring
    mov [edi + 4], eax       ; Entregar a la tarjeta

    ; Rotar puntero
    inc ebx
    and ebx, 3
    mov [pcnet_tx_idx], ebx

    ; Notificar a la tarjeta (TDMD = bit 3 en CSR0)
    mov edx, [pcnet_io_port]
    add edx, PCNET_RAP
    xor eax, eax
    out dx, eax

    mov edx, [pcnet_io_port]
    add edx, PCNET_RDP
    mov eax, 0x000A          ; TDMD + STRT
    out dx, eax

    pop edi
    pop esi
    pop ebx
    pop ebp
    ret


; Recibir
sys_net_receive_packet_pcnet:
    push ebp
    mov ebp, esp
    push ebx
    push esi
    push edi

    mov ebx, [pcnet_rx_idx]
    
    ; Comprobar si la tarjeta nos devolvió el descriptor
    mov esi, ebx
    shl esi, 4
    add esi, pcnet_rx_ring
    
    mov eax, [esi + 4]
    test eax, 0x80000000
    jnz .no_packet           ; Si OWN = 1, la tarjeta aún procesa

    ; Extraer longitud (Word 2) y quitar 4 bytes del HW CRC
    mov ecx, [esi + 8]
    and ecx, 0x0FFF
    sub ecx, 4
    push ecx

    ; Copiar a Java
    mov eax, 1536
    mul ebx
    add eax, pcnet_rx_buffers
    
    mov esi, eax             ; Buffer físico
    mov edi, [ebp + 8]
    add edi, 4               ; Evadir Java Header
    
    cld
    rep movsb

    ; Limpiar descriptor y devolver a la tarjeta
    mov esi, ebx
    shl esi, 4
    add esi, pcnet_rx_ring
    mov dword [esi + 4], 0x8000FA00  ; OWN(1) + ONES(F) + BCNT(-1536)
    mov dword [esi + 8], 0           ; Resetear Msg Byte Count

    ; Rotar puntero
    inc ebx
    and ebx, 3
    mov [pcnet_rx_idx], ebx

    ; Limpiar interrupción (RINT = bit 10 en CSR0)
    mov edx, [pcnet_io_port]
    add edx, PCNET_RAP
    xor eax, eax
    out dx, eax

    mov edx, [pcnet_io_port]
    add edx, PCNET_RDP
    mov eax, 0x0400          ; Clear RINT
    out dx, eax

    pop eax                  ; Retornar tamaño del paquete en EAX
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
