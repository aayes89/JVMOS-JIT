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

; Driver Realtek RTL8111/8168/8411 Gigabit Ethernet para Baremetal
; Copyright (c) 2026 Allan (Slam)

section .bss
alignb 256
    rtl8168_tx_ring:    resb 16 * 16
alignb 256
    rtl8168_rx_ring:    resb 16 * 16

alignb 16
    rtl8168_rx_buffers: resd 1
    rtl8168_tx_buffers: resd 1
    rtl8168_io_port:    resd 1
    rtl8168_tx_idx:     resd 1
    rtl8168_rx_idx:     resd 1
    ; Mutex Spinlocks de Hardware 
    rtl8168_tx_lock:    resd 1
    rtl8168_rx_lock:    resd 1

extern sys_kalloc

section .text

MAC0        equ 0x00
TxDescStart equ 0x20
RxDescStart equ 0xE4
CMD_REG     equ 0x37
IMR         equ 0x3C
ISR         equ 0x3E
TCR         equ 0x40
RCR_REG     equ 0x44
Cfg9346     equ 0x50

CR_RST      equ 0x10
CR_RE       equ 0x08
CR_TE       equ 0x04

DESC_OWN    equ 0x80000000
DESC_EOR    equ 0x40000000
DESC_FS     equ 0x20000000
DESC_LS     equ 0x10000000

sys_rtl8168_init:
    push ebp
    mov ebp, esp
    push ebx
    
    mov eax, [ebp + 8]
    mov [rtl8168_io_port], eax
    mov dword [rtl8168_tx_idx], 0
    mov dword [rtl8168_rx_idx], 0
    mov dword [rtl8168_tx_lock], 0
    mov dword [rtl8168_rx_lock], 0
    
    push 32768
    call sys_kalloc
    add esp, 4
    mov [rtl8168_rx_buffers], eax

    push 32768
    call sys_kalloc
    add esp, 4
    mov [rtl8168_tx_buffers], eax

    mov dx, word [rtl8168_io_port]
    add dx, Cfg9346
    mov al, 0xC0
    out dx, al

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
    xor eax, eax
    jmp .done
.rst_done:

    xor ecx, ecx
.init_rx:
    mov eax, ecx
    shl eax, 11
    add eax, [rtl8168_rx_buffers]
    mov ebx, ecx
    shl ebx, 4
    add ebx, rtl8168_rx_ring

    mov dword [ebx + 8], eax
    mov dword [ebx + 12], 0

    mov eax, 2048
    or eax, DESC_OWN
    cmp ecx, 15
    jne .rx_no_eor
    or eax, DESC_EOR
.rx_no_eor:
    mov [ebx], eax
    inc ecx
    cmp ecx, 16
    jl .init_rx

    xor ecx, ecx
.init_tx:
    mov eax, ecx
    shl eax, 11
    add eax, [rtl8168_tx_buffers]
    mov ebx, ecx
    shl ebx, 4
    add ebx, rtl8168_tx_ring

    mov dword [ebx + 8], eax
    mov dword [ebx + 12], 0

    mov eax, 0
    cmp ecx, 15
    jne .tx_no_eor
    or eax, DESC_EOR
.tx_no_eor:
    mov [ebx], eax
    inc ecx
    cmp ecx, 16
    jl .init_tx

    mov dx, word [rtl8168_io_port]
    add dx, RxDescStart
    mov eax, rtl8168_rx_ring
    out dx, eax
    add dx, 4
    xor eax, eax
    out dx, eax

    mov dx, word [rtl8168_io_port]
    add dx, TxDescStart
    mov eax, rtl8168_tx_ring
    out dx, eax
    add dx, 4
    xor eax, eax
    out dx, eax

    ; Apagar Modo Promiscuo (De 0x...E70F a 0x...E70E)
    mov dx, word [rtl8168_io_port]
    add dx, RCR_REG
    mov eax, 0x0000E70E
    out dx, eax

    mov dx, word [rtl8168_io_port]
    add dx, TCR
    mov eax, 0x03000700
    out dx, eax

    mov dx, word [rtl8168_io_port]
    add dx, CMD_REG
    mov al, CR_TE | CR_RE
    out dx, al

    mov dx, word [rtl8168_io_port]
    add dx, Cfg9346
    mov al, 0x00
    out dx, al
    
    mov dx, word [rtl8168_io_port]
    add dx, IMR
    xor ax, ax
    out dx, ax

    mov eax, 1
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

    ; Bloqueo Mutex (Anti-Colisión de Hilos)
    lock bts dword [rtl8168_tx_lock], 0
    jc .locked_tx

    mov esi, [ebp + 8]
    add esi, 4
    mov ecx, [ebp + 12]
    mov ebx, [rtl8168_tx_idx]

    mov edi, ebx
    shl edi, 4
    add edi, rtl8168_tx_ring

    mov eax, [edi]
    test eax, DESC_OWN
    jnz .done_tx

    push edi
    mov eax, ebx
    shl eax, 11
    add eax, [rtl8168_tx_buffers]
    mov edi, eax
    
    ; Copia segura y Runt Padding a 60 bytes
    push ecx
    cld
    rep movsb
    pop ecx

    cmp ecx, 60
    jge .skip_pad
    mov edx, ecx
    mov ecx, 60
    sub ecx, edx
    xor al, al
    rep stosb           ; Rellenar con ceros
    mov ecx, 60         ; Forzar longitud a 60
.skip_pad:
    pop edi

    mov eax, ecx
    or eax, DESC_OWN | DESC_FS | DESC_LS
    
    cmp ebx, 15
    jne .no_eor_tx
    or eax, DESC_EOR
.no_eor_tx:

    mov [edi], eax

    mov dx, word [rtl8168_io_port]
    add dx, 0x38
    mov al, 0x40
    out dx, al

    inc ebx
    and ebx, 15
    mov [rtl8168_tx_idx], ebx

.done_tx:
    mov dword [rtl8168_tx_lock], 0  ; Liberar Mutex
    xor eax, eax
    pop edi
    pop esi
    pop ebx
    pop ebp
    ret

.locked_tx:
    xor eax, eax
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

    ; Bloqueo Mutex (Anti-Colisión de Hilos)
    lock bts dword [rtl8168_rx_lock], 0
    jc .locked_rx

    mov ebx, [rtl8168_rx_idx]
    mov esi, ebx
    shl esi, 4
    add esi, rtl8168_rx_ring

    mov eax, [esi]
    test eax, DESC_OWN
    jnz .no_packet

    mov ecx, eax
    and ecx, 0x3FFF
    
    ; Descartar ruido de hardware con underflow protection
    cmp ecx, 4
    jle .garbage_packet
    sub ecx, 4
    
    mov edx, [ebp + 12]
    cmp ecx, edx
    jle .len_ok
    mov ecx, edx
.len_ok:

    mov eax, ebx
    shl eax, 11
    add eax, [rtl8168_rx_buffers]
    
    mov edi, [ebp + 8]
    add edi, 4
    
    push esi
    mov esi, eax
    cld
    push ecx
    rep movsb
    pop ecx
    pop esi
    
    mov eax, ecx            ; EAX = Longitud real para Java
    jmp .recycle

.garbage_packet:
    xor eax, eax            ; EAX = 0 (Basura)

.recycle:
    mov edx, 2048
    or edx, DESC_OWN
    
    cmp ebx, 15
    jne .no_eor_rx
    or edx, DESC_EOR
.no_eor_rx:
    mov [esi], edx

    inc ebx
    and ebx, 15
    mov [rtl8168_rx_idx], ebx
    
    mov dx, word [rtl8168_io_port]
    add dx, ISR
    push eax
    mov ax, 0xFFFF
    out dx, ax
    pop eax

    jmp .rx_done

.no_packet:
    xor eax, eax

.rx_done:
    mov dword [rtl8168_rx_lock], 0  ; Liberar Mutex
    pop edi
    pop esi
    pop ebx
    pop ebp
    ret

.locked_rx:
    xor eax, eax
    pop edi
    pop esi
    pop ebx
    pop ebp
    ret

section .note.GNU-stack noalloc noexec nowrite progbits
