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

public class Thread {
	
	private Runnable target;
	private int state;	
	//public int[] espBox = new int[1];	
	//private int stackBase;
	
	public static final int NEW = 0;
    public static final int RUNNABLE = 1;
    public static final int RUNNING = 2;
    public static final int TERMINATED = 3;
	public static final int BACKGROUND = 4;
	
	public Thread(){
		this.target = null;
        this.state = RUNNING;
        //this.stackBase = 0;
        //this.espBox[0] = 0;
	}
	public Thread(Runnable target){
		this.target = target;
		this.state = NEW;
		
		// Detenido mientras resuelvo como añadir Reflexión e intercambio de pilas en ASM
		
		// Asignar 4KB de memoria física para la pila de este hilo        
        //this.stackBase = Native.sys(Native.SYS_KALLOC, 4096, 0, 0, 0); 
        //this.espBox[0] = this.stackBase + 4096; // La pila crece hacia abajo

        // Pre-llenar la pila para engañar a 'sys_switch_context'
        // Empujar una dirección de retorno artificial que apunte a un envoltorio
        //pushToStack(Native.getAddressOf("java.lang.Thread.threadStarter")); 
        
        // Empujar 4 registros falsos (EBP, EBX, ESI, EDI) que 'pop' restaurará
        //pushToStack(0); // EDI
        //pushToStack(0); // ESI
        //pushToStack(0); // EBX
        //pushToStack(0); // EBP
	}
	
	// Detenido mientras resuelvo como añadir Reflexión e intercambio de pilas en ASM	
	/*private void pushToStack(int value) {
		// Bajo el puntero de la pila 4 bytes
        this.espBox[0] -= 4; 
		// Escribir los 32 bits en la memoria RAM
        Native.sys(Native.SYS_MEM_WRITE_DWORD, this.espBox[0], value, 0, 0); 
    }*/
	
	/*private static void threadStarter(){
		Thread current = Scheduler.currentThread();
        if (current != null && current.target != null) {
            current.state = RUNNING;
            current.target.run();
            current.state = TERMINATED;
        }
        // Al acabar, cede el control para no colgar el procesador
        Scheduler.yieldCPU();
	}*/
	
	public void start(){
		if(state != NEW){
			return;
		}
		state = BACKGROUND;
		Scheduler.add(this);
	}
	
	public void run(){
		if (target != null) {
            target.run();
        }
		// Solo terminar hilo si no es un demonio de fondo
		if(state != BACKGROUND){
			state = TERMINATED;
		}
    }

    public int getState() {
        return state;
    }
	public void setState(int state){
		this.state = state;
	}
	
    public static void sleep(long millis) {
        // Syscall 12 = SYS_SLEEP
        kernel.Native.sys(12, (int) millis, 0, 0, 0); 
    }
}
