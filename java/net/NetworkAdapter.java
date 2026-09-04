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
    
	//public static final String RTL8139 = "RTL8139";
    private String modelName;
    private int ioPortBase;
    private boolean initialized;

    // Constructor que asocia la tarjeta a su puerto base PCI
    public NetworkAdapter(String modelName, int ioPortBase) {
        this.modelName = modelName;
        this.ioPortBase = ioPortBase;
        this.initialized = false;
    }

    // Inicializa la tarjeta según la especificación del modelo
    public boolean init() {
        if (this.modelName.equals("RTL8139")) {
            // Syscall 23: Inicializar Tarjeta de Red[cite: 8, 12]
            int status = Native.sys(Native.SYS_RTL8139_INIT, this.ioPortBase, 0, 0, 0);
            this.initialized = (status == 0);
            return this.initialized;
        }
        // Futuras tarjetas (ej. E1000, Virtio) irían aquí
        return false; 
    }

    // Transmite un paquete usando el arreglo de bytes del DatagramPacket
    public void send(DatagramPacket packet) {
        if (!this.initialized || packet == null) return;
        
        // Syscall 24: Enviar paquete de Red[cite: 8, 12]
        Native.sys(Native.SYS_RTL8139_SEND, 0, packet.getLength(), packet.getData(), 0);
    }

    // Recibe un paquete en el buffer del DatagramPacket
    public int receive(DatagramPacket packet) {
        if (!this.initialized || packet == null) return -1;
        
        // Syscall 25: Recibir paquete de Red[cite: 8, 12]
        int bytesRead = Native.sys(Native.SYS_NET_RECEIVE, 0, packet.getLength(), packet.getData(), 0);
        
        if (bytesRead > 0) {
            packet.setLength(bytesRead);
        }
        return bytesRead;
    }

    public boolean isInitialized() {
        return this.initialized;
    }
}
