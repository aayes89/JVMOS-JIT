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
import java.lang.String;
/*
	Nota: RandomAccessFile ya no será necesario con esta clase, será eliminado en la próxima iteración.
*/


public class DiskIO {
    
    private int deviceId;	// Disco 0
    private int bufferAddr; // Puntero físico privado alineado a 4 bytes para DMA

    // Constructor por defecto
    public DiskIO() {
		// (Disco primario IDE)
        this(0);
    }

    // Constructor para múltiples discos
    public DiskIO(int id) {
        deviceId = id;
        // Syscall 0: SYS_KALLOC
		// Asignar 512 bytes exactos en el Heap para operaciones de sector
        bufferAddr = Native.sys(Native.SYS_KALLOC, 512, 0, 0, 0);
    }

    // Lee un sector crudo a un arreglo de bytes (para sistemas de archivos)
    public byte[] readSector(int lba) {
        // Syscall 8: SYS_DISK_READ (Leer sector ATA)
        int resultRead = Native.sys(Native.SYS_DISK_READ, lba, 0, bufferAddr, 0);
        if(resultRead == 1){
			byte[] data = new byte[512];
			for (int i = 0; i < 512; i++) {
				// Syscall 27: SYS_MEM_READ_BYTE
				data[i] = (byte) Native.sys(27, bufferAddr + i, 0, 0, 0); 
			}
			return data;
		}
		// devolver arreglo vacio, evitar NULL
		return new byte[0];
    }

    // Lee un sector y lo interpreta directamente como String (hasta encontrar un byte 0)    
	public String readString(int lba) {
		// Syscall 8: SYS_DISK_READ (Leer sector ATA)
        int resultRead = Native.sys(Native.SYS_DISK_READ, lba, 0, bufferAddr, 0);
        if(resultRead == 1){
			byte[] bytes = new byte[512];
			int length = 0;
			// leer todo el bloque de 512 bytes
			for (int i = 0; i < 512; i++) {
				// Syscall 27: SYS_MEM_READ_BYTE
				int b = Native.sys(27, bufferAddr + i, 0, 0, 0); 
				if (b == 0) break; // Fin de la cadena
				bytes[length++] = (byte) b;				
			}
			
			// Evitando el constructor complejo ([CII)V
			String fullStr = new String(bytes); 
			return fullStr;
		}
		// devolver string vacio, evitar NULL
		return new String();
    }
	
    // Escribe un arreglo de bytes crudos al disco
    public boolean writeSector(int lba, byte[] data) {
        if (data == null || data.length > 512) return false;
        
		// Limpiar el bloque inicial de 512 bytes en RAM con 0s
		for (int i = 0; i < 512; i++) {
			// Syscall 26: SYS_MEM_WRITE_BYTE
            Native.sys(26, bufferAddr + i, 0, 0, 0);
        }
        
        // Escribo en el bloque inicial el arreglo
        for (int i = 0; i < data.length; i++) {
			// Syscall 26: SYS_MEM_WRITE_BYTE
            Native.sys(26, bufferAddr + i, data[i], 0, 0);
        }
        
        // Escribo en disco y espero resultado: 0-fallo | 1-ok
		// Syscall 9: SYS_DISK_WRITE
        int result = Native.sys(Native.SYS_DISK_WRITE, lba, 0, bufferAddr, 0);
        return (result == 1);
    }

	// Escribe un String directamente en un sector
    public boolean writeString(int lba, String text) {
        if (text == null) return false;        
		byte[] bytes = text.getBytes();        
        return writeSector(lba, bytes);
    }
	
    // Liberar la memoria (para cuando implemente SYS_KFREE en el futuro)
    public void close() {
        // Native.sys(SYS_KFREE, this.bufferAddr, ...);
    }
}
