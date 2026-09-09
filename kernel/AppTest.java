/*MIT License

Copyright (c) 2026 Allan (Slam)

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.*/

package kernel;

public class AppTest {
    public static void main(String[] args) {
        // Syscall 1: SYS_SET_COLOR (Rojo)
        kernel.Native.sys(1, 0x00FF0000, 0, 0, 0);
        // Syscall 2: SYS_FILL_RECT
        kernel.Native.sys(2, 200, 200, 300, 300);
		// Syscall 1: SYS_SET_COLOR (Azul)
        kernel.Native.sys(1, 0x000000FF, 0, 0, 0);
		// Imprime texto en el rectángulo
		kernel.Native.sys(5, 210, 220, "Hola mundo JVMOS-JIT!",0);
        // Pausar 3 segundos para observar el cambio
        kernel.Native.sys(12, 3000, 0, 0, 0); 
    }
}
