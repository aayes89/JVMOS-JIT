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

// Usar DiskIO para IO en discos duros, esta clase será eliminada en la próxima iteración

public class RandomAccessFile {
	
    public void readSector(int lba, byte[] buffer) {
		// Syscall 8: Leer sector ATA IDE LBA28
        kernel.Native.sys(kernel.Native.SYS_DISK_READ, lba, 0, buffer, 0);
    }
	
    public void writeSector(int lba, byte[] buffer) {
		// Syscall 9: Escribir sector ATA IDE LBA28
        kernel.Native.sys(kernel.Native.SYS_DISK_WRITE, lba, 0, buffer, 0);
    }
}
