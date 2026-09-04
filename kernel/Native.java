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

public class Native {

    // TABLA DE SYSCALLS
    public static final int SYS_KALLOC           = 0;  // Asignación de memoria en Heap
    public static final int SYS_SET_COLOR        = 1;  // Color activo VRAM
    public static final int SYS_FILL_RECT        = 2;  // Rellenar rectángulo
    public static final int SYS_DRAW_RECT        = 3;  // Dibujar borde rectángulo
    public static final int SYS_DRAW_LINE        = 4;  // Dibujar línea
    public static final int SYS_DRAW_STRING      = 5;  // Imprimir cadena texto
    public static final int SYS_READ_KEYBOARD    = 6;  // Leer teclado PS/2 (FIFO)
    public static final int SYS_READ_MOUSE       = 7;  // Leer ratón PS/2 (X, Y, Botones)
    public static final int SYS_DISK_READ        = 8;  // Leer sector ATA IDE LBA28
    public static final int SYS_DISK_WRITE       = 9;  // Escribir sector ATA IDE LBA28
    public static final int SYS_INB              = 10; // Puerto E/S (inb)
    public static final int SYS_OUTB             = 11; // Puerto E/S (outb)
    public static final int SYS_SLEEP            = 12; // Retardo ms (PIT IRQ0)
    public static final int SYS_GET_TIME         = 13; // CMOS RTC (Hora/Fecha)
    public static final int SYS_GET_PIXEL        = 14; // Leer pixel de VRAM
    public static final int SYS_DRAW_CHAR        = 15; // Renderizar carácter en VRAM
    public static final int SYS_SET_KBD_LAYOUT   = 16; // Mapa Teclado (0=US, 1=ES)
    public static final int SYS_EXIT             = 17; // Apagar equipo
    public static final int SYS_GET_TICKS        = 18; // Consultar ticks del sistema
    public static final int SYS_SERIAL_PUTC      = 19; // Enviar carácter por COM1
    public static final int SYS_SERIAL_PUTS      = 20; // Enviar cadena por COM1 (Debug)
    public static final int SYS_PCI_READ         = 21; // Leer espacio de config PCI
    public static final int SYS_BEEP             = 22; // Audio PC Speaker (Frecuencia Hz)
    public static final int SYS_RTL8139_INIT     = 23; // Inicializar Tarjeta de Red
    public static final int SYS_RTL8139_SEND     = 24; // Enviar paquete de Red
    public static final int SYS_NET_RECEIVE      = 25; // Recibir paquete de Red
    public static final int SYS_MEM_WRITE_BYTE   = 26; // Escribir un byte en una direccion fisica
    public static final int SYS_MEM_READ_BYTE    = 27; // Leer un byte de una direccion fisica

    // FIRMAS NATIVAS
    public static native int sys(int id, int a, int b, Object c, int d);
    public static native int sys(int id, int a, int b, int c, int d);
}
