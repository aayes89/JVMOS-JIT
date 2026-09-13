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

import java.io.CharBuffer;
import java.io.ReadOnlyBufferException;

public abstract class Reader implements Readable, Closeable {
    // Objeto usado para sincronizar con el lector
    protected Object lock;

    // Constructor
    protected Reader() {
        lock = this;
    }

    // Constructor de Objeto
    protected Reader(Object lock) {
        if (lock == null) {
            throw new NullPointerException("lock == null");
        }
        this.lock = lock;
    }

    // Cierra este lector
    public abstract void close() throws IOException;

    // establece marca con límite
    public void mark(int readLimit) throws IOException {
        throw new IOException();
    }

    // Indicar si soporta marcas
    public boolean markSupported() {
        return false;
    }

    // Leer un sólo caracter, -1 si no pudo
    public int read() throws IOException {
        synchronized (lock) {
            char[] charArray = new char[1];
            if (read(charArray, 0, 1) != -1) {
                return charArray[0];
            }
            return -1;
        }
    }

    // Lee caracteres y almacena en el buffer, -1 si no pudo
    public int read(char[] buffer) throws IOException {
        return read(buffer, 0, buffer.length);
    }

    // Leer caracteres en un buffer desde un rango, -1 si no pudo
    public abstract int read(char[] buffer, int offset, int count) throws IOException;

    // Indica si esta listo (recibió datos)
    public boolean ready() throws IOException {
        return false;
    }

    // Resetea estado
    public void reset() throws IOException {
        throw new IOException();
    }

    // Omite cierta cantidad de caracteres
    public long skip(long charCount) throws IOException {
        if (charCount < 0) {
            throw new IllegalArgumentException("charCount < 0: " + charCount);
        }
        synchronized (lock) {
            long skipped = 0;
            int toRead = charCount < 512 ? (int) charCount : 512;
            char[] charsSkipped = new char[toRead];
            while (skipped < charCount) {
                int read = read(charsSkipped, 0, toRead);
                if (read == -1) {
                    return skipped;
                }
                skipped += read;
                if (read < toRead) {
                    return skipped;
                }
                if (charCount - skipped < toRead) {
                    toRead = (int) (charCount - skipped);
                }
            }
            return skipped;
        }
    }

    // Lee un caracter y almacena en el buffer
    public int read(CharBuffer target) throws IOException {
        int length = target.length();
        char[] buf = new char[length];
        length = Math.min(length, read(buf));
        if (length > 0) {
            target.put(buf, 0, length);
        }
        return length;
    }
}
