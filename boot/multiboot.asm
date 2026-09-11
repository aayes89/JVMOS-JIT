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

MBALIGN  equ  1 << 0
MEMINFO  equ  1 << 1
VIDINFO  equ  1 << 2
FLAGS    equ  MBALIGN | MEMINFO | VIDINFO
MAGIC    equ  0x1BADB002
CHECKSUM equ -(MAGIC + FLAGS)

section .multiboot
align 4
    dd MAGIC
    dd FLAGS
    dd CHECKSUM
    dd 0, 0, 0, 0, 0
    ; Dejamos que GRUB decida usando grub.cfg
    dd 0
    dd 0
    dd 0
    dd 32

section .bootstrap_stack nobits
align 16
stack_bottom:
    resb 32768
stack_top:

global g_framebuffer
global g_width
global g_height
global g_pitch

section .data
g_framebuffer: dd 0
g_width:       dd 0
g_height:      dd 0
g_pitch:       dd 0

align 16
gdt_start:
    dd 0x00000000, 0x00000000 
    dd 0x0000FFFF, 0x00CF9A00 
    dd 0x0000FFFF, 0x00CF9200 
gdt_end:
gdtr:
    dw gdt_end - gdt_start - 1
    dd gdt_start

section .text
global _start
extern bootjvm_start
extern bss_start
extern bss_end

_start:
    cli
    mov esp, stack_top
	
	fninit	; coprocesador matemático FPU
	
	; 2. Limpiar la memoria BSS (Poner a cero para las variables de Java)
    mov edi, bss_start
    mov ecx, bss_end
    sub ecx, edi
    xor eax, eax
    rep stosb        ; Escribe EAX (0) en EDI repetidamente ECX veces
	
    lgdt [gdtr]
    jmp 0x08:.reload_segments

.reload_segments:
    mov ax, 0x10
    mov ds, ax
    mov es, ax
    mov fs, ax
    mov gs, ax
    mov ss, ax

    cmp ebx, 0
    je .hang

    mov eax, [ebx]           ; Leer banderas Multiboot

    test eax, (1 << 12)      ; ¿Estructura Framebuffer presente?
    jnz .parse_fb

    test eax, (1 << 11)      ; ¿Estructura VBE presente?
    jnz .parse_vbe

    jmp .hang                ; Sin video, detener (hlt) para evitar crash.

.parse_fb:
    mov eax, [ebx + 88]
    cmp eax, 0
    je .hang                 ; Protección estricta contra puntero nulo
    mov [g_framebuffer], eax
    
    mov eax, [ebx + 100]
    mov [g_width], eax
    
    mov eax, [ebx + 104]
    mov [g_height], eax
    
    mov eax, [ebx + 96]
    mov [g_pitch], eax
    jmp .start_jvm

.parse_vbe:
    mov edi, [ebx + 76]      ; vbe_mode_info
    cmp edi, 0
    je .hang

    mov eax, [edi + 40]      ; phys_base_ptr
    cmp eax, 0
    je .hang                 ; Protección estricta contra puntero nulo
    mov [g_framebuffer], eax

    movzx eax, word [edi + 18]
    mov [g_width], eax

    movzx eax, word [edi + 20]
    mov [g_height], eax

    movzx eax, word [edi + 16]
    mov [g_pitch], eax

.start_jvm:
    push ebx
    call bootjvm_start
    add esp, 4

.hang:
    hlt
    jmp .hang

section .note.GNU-stack noalloc noexec nowrite progbits
