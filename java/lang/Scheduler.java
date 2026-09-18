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

package java.lang;

import kernel.Native;

public final class Scheduler {

    private static Thread current;
    private static Thread[] threads = new Thread[16]; // Por el momento
    private static int count;
	//private static int currentIndex; // Para cuando tenga reflexión implementada

	// Constructor vacío simple
    private Scheduler() {}

	// Añadir hilo
    public static void add(Thread thread) {
        if (count < threads.length){
			threads[count] = thread;
			count++;
		}
    }
	
	// Soporte para versión moderna (Eventos cooperativos) - TODO
	// Cedo el procesador al siguiente hilo en la cola
	public static void yieldCPU() {
        if (count == 0) return;

        /*int start = currentIndex;
        while (true) {
            currentIndex++;
            if (currentIndex >= count) currentIndex = 0;
            
            Thread next = threads[currentIndex];
            if (next != null && (next.getState() == Thread.RUNNABLE || next.getState() == Thread.RUNNING)) {
                
                Thread oldThread = current;
                current = next;
                
                if (oldThread != null && oldThread.getState() == Thread.RUNNING) {
                    oldThread.setState(Thread.RUNNABLE);
                }
                
                // Syscall 34 (SYS_CONTEXT_SWITCH)
                // Pasa el arreglo (como puntero) y el valor numérico del ESP entrante
                Native.sys(Native.SYS_CONTEXT_SWITCH, oldThread.espBox, next.espBox[0], 0, 0);
                return;
            }
            if (currentIndex == start) return;
        }*/
    }

	// Iniciar hilo
    public static void start() {
		// No bloquea la CPU si no hay nada que hacer
        while (true) {
            boolean idle = true;
            
            for (int i = 0; i < count; i++) {
                Thread t = threads[i];
                if (t != null && (t.getState() == Thread.RUNNABLE || t.getState() == Thread.BACKGROUND)) {
                    idle = false;
                    current = t;
                    t.run(); // Ejecuta una fracción de trabajo y retorna inmediatamente
                    current = null;
                }
            }
            
            // Si todos los hilos están terminados o inactivos, descansamos 1ms
            if (idle) {
                Thread.sleep(1);
            }
        }
    
		// No usar hasta tener reflexión
        // Asigno el kernel base como el "hilo 0" 
		// para que tenga a dónde guardar su estado
        /*Thread kernelThread = new Thread();        
        current = kernelThread;
		add(kernelThread);	
        yieldCPU(); // Iniciar el carrusel
		*/
    }

    public static Thread currentThread() {
        return current;
    }
}
