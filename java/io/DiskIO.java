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

package java.io;

import kernel.Native;
/*
	Nota: RandomAccessFile ya no será necesario con esta clase, será eliminado en la próxima iteración.
*/


public class DiskIO {
    
    private int deviceId;
    private int bufferAddr; // Puntero físico privado alineado a 4 bytes para DMA

    // Constructor por defecto (Disco primario IDE)
    public DiskIO() {
        this(0);
    }

    // Constructor para múltiples discos
    public DiskIO(int deviceId) {
        this.deviceId = deviceId;
        // Syscall 0: Asignar 512 bytes exactos en el Heap para operaciones de sector
        this.bufferAddr = Native.sys(Native.SYS_KALLOC, 512, 0, 0, 0);
    }

    // Lee un sector crudo a un arreglo de bytes (para sistemas de archivos)
    public byte[] readSector(int lba) {
        // Syscall 8: Leer sector ATA
        Native.sys(Native.SYS_DISK_READ, lba, 0, this.bufferAddr, 0);
        
        byte[] data = new byte[512];
        for (int i = 0; i < 512; i++) {
            data[i] = (byte) Native.sys(27, this.bufferAddr + i, 0, 0, 0); // SYS_MEM_READ_BYTE
        }
        return data;
    }

    // Lee un sector y lo interpreta directamente como String (hasta encontrar un byte 0)
    public String readString(int lba) {
        Native.sys(Native.SYS_DISK_READ, lba, 0, this.bufferAddr, 0);
        
        char[] chars = new char[512];
        int length = 0;
        
        for (int i = 0; i < 512; i++) {
            int b = Native.sys(27, this.bufferAddr + i, 0, 0, 0);
            if (b == 0) break; // Fin de cadena
            chars[length++] = (char) b;
        }
        return new String(chars, 0, length);
    }

    // Escribe un arreglo de bytes crudos al disco
    public boolean writeSector(int lba, byte[] data) {
        if (data == null || data.length > 512) return false;
        
        // 1. Limpiar el buffer físico con 0s
        for (int i = 0; i < 512; i++) {
            Native.sys(26, this.bufferAddr + i, 0, 0, 0); // SYS_MEM_WRITE_BYTE
        }
        
        // 2. Inyectar datos de Java a la RAM física
        for (int i = 0; i < data.length; i++) {
            Native.sys(26, this.bufferAddr + i, data[i], 0, 0);
        }
        
        // 3. Ordenar escritura al disco
        int result = Native.sys(Native.SYS_DISK_WRITE, lba, 0, this.bufferAddr, 0);
        return (result == 1);
    }

    // Escribe un String directamente en un sector
    public boolean writeString(int lba, String text) {
        if (text == null) return false;
        
        // Asumiendo que has implementado toCharArray() o un iterador similar en tu String básico
        char[] chars = text.toCharArray(); 
        byte[] bytes = new byte[chars.length];
        
        for (int i = 0; i < chars.length; i++) {
            bytes[i] = (byte) chars[i];
        }
        
        return writeSector(lba, bytes);
    }
    
    // Liberar la memoria (para cuando implemente SYS_KFREE en el futuro)
    public void close() {
        // Native.sys(SYS_KFREE, this.bufferAddr, ...);
    }
}
