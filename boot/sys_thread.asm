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

; llamada así: void sys_switch_context(int[] espBox, int next_esp);

sys_switch_context:
    ; Guardo el estado del hilo actual en SU propia pila
    push ebp
    mov ebp, esp
    push ebx
    push esi
    push edi

    ; Leer argumentos de Java
    mov eax, [ebp + 8]   ; EAX = Puntero al arreglo 'espBox' del hilo saliente
	add eax, 4			 ; salto los 4 bytes de longitud del arreglo espBox
    mov edx, [ebp + 12]  ; EDX = Valor numérico del ESP del hilo entrante

    ; Cambio de contexto 
    mov [eax], esp       ; Guardo ESP actual en espBox[0]
    mov esp, edx         ; Inyecto a la pila el nuevo hilo en el procesador

    ; Restaurar el estado del nuevo hilo desde SU propia pila
    pop edi
    pop esi
    pop ebx
    pop ebp
    ret                  ; Al hacer 'ret', la CPU salta a donde el nuevo hilo se había quedado
	
section .note.GNU-stack noalloc noexec nowrite progbits	
