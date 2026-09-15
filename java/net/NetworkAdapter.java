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

package java.net;

import kernel.Native;

public class NetworkAdapter {
    
	public static final int TYPE_RTL8139 = 1;
	public static final int TYPE_E1000 = 2;
	public static final int TYPE_VIRTIO = 3;
	
    private int modelType;
    private int ioPortBase;
    private boolean initialized;

    // Constructor que asocia la tarjeta a su puerto base PCI
    public NetworkAdapter(int type, int ioPort) {
        modelType = type;
        ioPortBase = ioPort;
        initialized = false;
    }

    // Inicializa la tarjeta según la especificación del modelo
    public boolean init() {
        if (modelType == TYPE_RTL8139) {            
			// Inicializar Tarjeta de Red
			// Syscall 23:  arg_a = Puerto I/O (SYS_RTL8139_INIT)			
            int status = Native.sys(Native.SYS_RTL8139_INIT, ioPortBase, 0, 0, 0);
            initialized = (status == 1);
            return initialized;
        }
        // Soporte a futuras tarjetas
		else if(modelType == TYPE_E1000){
			// E1000			
		}else if(modelType == TYPE_VIRTIO){		
			// Virtio
		}
        return false; 
    }

    // Transmite un paquete usando el arreglo de bytes del DatagramPacket
    public void send_old(DatagramPacket packet) {
        if (!initialized || packet == null) return;
        
        // Syscall 24: Enviar paquete de Red
        Native.sys(Native.SYS_RTL8139_SEND, 0, packet.getLength(), packet.getData(), 0);
    }

    // Recibe un paquete en el buffer del DatagramPacket
    public int receive_old(DatagramPacket packet) {
        if (!initialized || packet == null) return -1;
        
        // Syscall 25: Recibir paquete de Red
        int bytesRead = Native.sys(Native.SYS_NET_RECEIVE, 0, packet.getLength(), packet.getData(), 0);
        
        if (bytesRead > 0) {
            packet.setLength(bytesRead);
        }
        return bytesRead;
    }

    // Transmite un paquete usando memoria física directa
    public void send(DatagramPacket packet) {
        if (!initialized || packet == null) return;
        
        // Marca de los 32MB (Seguro, lejos del Kernel)
        int txAddr = 0x02000000; 
        byte[] data = packet.getData();
        int len = packet.getLength();
        
        // Escribir el paquete byte por byte en la RAM física (Syscall 26)
        for(int i = 0; i < len; i++) {
            Native.sys(26, txAddr + i, data[i], 0, 0); 
        }
        
        // Syscall 24: Enviar indicando la dirección física
        Native.sys(Native.SYS_RTL8139_SEND, 0, len, txAddr, 0);
    }

    // Recibe un paquete hacia memoria física directa
    public int receive(DatagramPacket packet) {
        if (!initialized || packet == null){ 
            return -1;
        }
        // Marca de los 32MB + 8KB
        int rxAddr = 0x02002000; 
        
        // Syscall 25: Recibir indicando la dirección física
        int bytesRead = Native.sys(Native.SYS_NET_RECEIVE, 0, packet.getLength(), rxAddr, 0);
        
        if (bytesRead > 0) {
            byte[] data = packet.getData();
            // Rescatar los bytes de la memoria física hacia nuestro arreglo Java (Syscall 27)
            for(int i = 0; i < bytesRead; i++) {
                data[i] = (byte) Native.sys(27, rxAddr + i, 0, 0, 0);
            }
            packet.setLength(bytesRead);
        }
        return bytesRead;
    }

    public boolean isInitialized() {
        return initialized;
    }
}
