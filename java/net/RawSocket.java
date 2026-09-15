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

public class RawSocket {
    private boolean initialized = false;

    // Inicializa la RTL8139 pasando su puerto I/O base de PCI
    public RawSocket() {
        initialized = true;
    }

    // Enviar usando memoria física directa (Evita corrupción del Object Header)
    public void send(DatagramPacket packet) {
        if (!initialized || packet == null) return;
        
        int txAddr = 0x02000000; // Escribir en los 32MB de la RAM (Lejos del Kernel)
        byte[] data = packet.getData();
        int len = packet.getLength();
        
        // Bajar los datos del Objeto Java a la RAM Física (Syscall 26)
        for(int i = 0; i < len; i++) {
            kernel.Native.sys(26, txAddr + i, data[i], 0, 0); 
        }
        
        // Transmitir enviando la dirección cruda, no el Objeto
        kernel.Native.sys(kernel.Native.SYS_RTL8139_SEND, 0, len, txAddr, 0);
    }

    // Recibir desde la memoria física directa
    public int receive(DatagramPacket packet) {
        if (!initialized || packet == null) return -1;
        
        int rxAddr = 0x02002000; // Leer en los 32MB + 8KB
        
        // Recibir la trama en la RAM Física directamente
        int bytesRead = kernel.Native.sys(kernel.Native.SYS_NET_RECEIVE, 0, packet.getLength(), rxAddr, 0);
        
        if (bytesRead > 0) {
            byte[] data = packet.getData();
            // Rescatar los bytes hacia Java de forma segura (Syscall 27)
            for(int i = 0; i < bytesRead; i++) {
                data[i] = (byte) kernel.Native.sys(27, rxAddr + i, 0, 0, 0);
            }
            packet.setLength(bytesRead);
        }
        return bytesRead;
    }
}
