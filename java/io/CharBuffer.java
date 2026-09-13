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

//import java.io.IOException;
//import java.io.BufferOverflowException;
import java.lang.IllegalArgumentException;
import java.util.Arrays;
import java.lang.Math;


public abstract class CharBuffer extends Buffer implements Comparable<CharBuffer>, CharSequence, Appendable, Readable {

	// Crear un Charbuffer con una capacidad dada
    public static CharBuffer allocate(int capacity) {
        if (capacity < 0) {
            throw new IllegalArgumentException("capacity < 0: " + capacity);
        }
        return new CharArrayBuffer(new char[capacity]);
    }

    // Crear un Charbuffer a partir de un arreglo dado
    public static CharBuffer wrap(char[] array) {
        return wrap(array, 0, array.length);
    }

    // Crear un Charbuffer a partir de un arreglo dado
    public static CharBuffer wrap(char[] array, int start, int charCount) {
        Arrays.checkOffsetAndCount(array.length, start, charCount);
        CharBuffer buf = new CharArrayBuffer(array);
        buf.position = start;
        buf.limit = start + charCount;
        return buf;
    }

    // Crear un Charbuffer a partir de un CharSequence
    public static CharBuffer wrap(CharSequence chseq) {
        return new CharSequenceAdapter(chseq);
    }

    // Crear un Charbuffer a partir de un CharSequence
    public static CharBuffer wrap(CharSequence cs, int start, int end) {
        if (start < 0 || end < start || end > cs.length()) {
            throw new IndexOutOfBoundsException("cs.length()=" + cs.length() + ", start=" + start + ", end=" + end);
        }
        CharBuffer result = new CharSequenceAdapter(cs);
        result.position = start;
        result.limit = end;
        return result;
    }

    CharBuffer(int capacity, long effectiveDirectAddress) {
        super(1, capacity, effectiveDirectAddress);
    }

    public final char[] array() {
        return protectedArray();
    }

    public final int arrayOffset() {
        return protectedArrayOffset();
    }

    // Bstracción que devuelve un CharBuffer de sólo lectura.
    public abstract CharBuffer asReadOnlyBuffer();

    // Devuelve el caracter en la posición dada 
    public final char charAt(int index) {
        if (index < 0 || index >= remaining()) {
            throw new IndexOutOfBoundsException("index=" + index + ", remaining()=" + remaining());
        }
        return get(position + index);
    }

    // Devuelve un Charbuffer sin nulos
    public abstract CharBuffer compact();

    // Compara un Charbuffer con otro, devuelve 1 si son iguales o -1 si no lo son
    public int compareTo(CharBuffer otherBuffer) {
        int compareRemaining = (remaining() < otherBuffer.remaining()) ? remaining()
                : otherBuffer.remaining();
        int thisPos = position;
        int otherPos = otherBuffer.position;
        char thisByte, otherByte;
        while (compareRemaining > 0) {
            thisByte = get(thisPos);
            otherByte = otherBuffer.get(otherPos);
            if (thisByte != otherByte) {
                return thisByte < otherByte ? -1 : 1;
            }
            thisPos++;
            otherPos++;
            compareRemaining--;
        }
        return remaining() - otherBuffer.remaining();
    }

    // Devuelve una copia del Charbuffer
    public abstract CharBuffer duplicate();

    // Verifica si son iguales 
    @Override
    public boolean equals(Object other) {
        if (!(other instanceof CharBuffer)) {
            return false;
        }
        CharBuffer otherBuffer = (CharBuffer) other;

        if (remaining() != otherBuffer.remaining()) {
            return false;
        }

        int myPosition = position;
        int otherPosition = otherBuffer.position;
        boolean equalSoFar = true;
        while (equalSoFar && (myPosition < limit)) {
            equalSoFar = get(myPosition++) == otherBuffer.get(otherPosition++);
        }

        return equalSoFar;
    }

    // Devuelve el caracter en la posición actual
    public abstract char get();

    // Lee caracteres dado un arreglo 
    public CharBuffer get(char[] dst) {
        return get(dst, 0, dst.length);
    }

    // Lee caracteres dado un rango de búsqueda en el arreglo 
    public CharBuffer get(char[] dst, int dstOffset, int charCount) {
        Arrays.checkOffsetAndCount(dst.length, dstOffset, charCount);
        if (charCount > remaining()) {
            throw new BufferUnderflowException();
        }
        for (int i = dstOffset; i < dstOffset + charCount; ++i) {
            dst[i] = get();
        }
        return this;
    }

    // Devuelve el caracter en la posición dada
    public abstract char get(int index);

    public final boolean hasArray() {
        return protectedHasArray();
    }

    // Calcular el código hash
    @Override
    public int hashCode() {
        int myPosition = position;
        int hash = 0;
        while (myPosition < limit) {
            hash = hash + get(myPosition++);
        }
        return hash;
    }

    // Abstracción para determinar si el Charbuffer es directo
    public abstract boolean isDirect();

    // Devuelve la cantidad real de caracteres en el buffer
    public final int length() {
        return remaining();
    }

    // Ordena los caracteres en orden ascendente
    public abstract ByteOrder order();

    // Devuelve el arreglo con propiadad protegida
    abstract char[] protectedArray();

    // Proteccion de rango del arreglo
    abstract int protectedArrayOffset();

    // Determinar si está protegido el arreglo
    abstract boolean protectedHasArray();

    // Escribir el caracter dentro del arreglo (Operación de archivos)
    public abstract CharBuffer put(char c);

    // Escribe un arreglo dentro del buffer
    public final CharBuffer put(char[] src) {
        return put(src, 0, src.length);
    }

    // Escribe un arreglo dentro de un rango dado del buffer
    public CharBuffer put(char[] src, int srcOffset, int charCount) {
        Arrays.checkOffsetAndCount(src.length, srcOffset, charCount);
        if (charCount > remaining()) {
            throw new BufferOverflowException();
        }
        for (int i = srcOffset; i < srcOffset + charCount; ++i) {
            put(src[i]);
        }
        return this;
    }

    // Escribe un CharBuffer dentro del principal
    public CharBuffer put(CharBuffer src) {
        if (isReadOnly()) {
            throw new ReadOnlyBufferException();
        }
        if (src == this) {
            throw new IllegalArgumentException("src == this");
        }
        if (src.remaining() > remaining()) {
            throw new BufferOverflowException();
        }

        char[] contents = new char[src.remaining()];
        src.get(contents);
        put(contents);
        return this;
    }

    // Inserta un caracter en la posición dada
    public abstract CharBuffer put(int index, char c);

    // Inserta cadenad de texto en el buffer (reescribe todo)
    public final CharBuffer put(String str) {
        return put(str, 0, str.length());
    }

    // Inserta cadena de texto en posición dada del buffer
    public CharBuffer put(String str, int start, int end) {
        if (isReadOnly()) {
            throw new ReadOnlyBufferException();
        }
        if (start < 0 || end < start || end > str.length()) {
            throw new IndexOutOfBoundsException("str.length()=" + str.length() +
                    ", start=" + start + ", end=" + end);
        }
        if (end - start > remaining()) {
            throw new BufferOverflowException();
        }
        for (int i = start; i < end; i++) {
            put(str.charAt(i));
        }
        return this;
    }

    // Devuelve fragmento del CharBuffer
    public abstract CharBuffer slice();

    // Devuelve fragmento del Charbuffer a partir de rango dado
    public abstract CharBuffer subSequence(int start, int end);

    /**
     * Returns a string representing the current remaining chars of this buffer.
     */
    @Override
    public String toString() {
        StringBuilder result = new StringBuilder(limit - position);
        for (int i = position; i < limit; i++) {
            result.append(get(i));
        }
        return result.toString();
    }

    // Añade un caracter al final del buffer
    public CharBuffer append(char c) {
        return put(c);
    }

    // Agrega una CharBuffer al final del buffer
    public CharBuffer append(CharSequence csq) {
        if (csq != null) {
            return put(csq.toString());
        }
        return put("null");
    }

    // Agrega una sequencia completa en un rango dado (desplaza a la derecha contenido)
    public CharBuffer append(CharSequence csq, int start, int end) {
        if (csq == null) {
            csq = "null";
        }
        CharSequence cs = csq.subSequence(start, end);
        if (cs.length() > 0) {
            return put(cs.toString());
        }
        return this;
    }

    // Leer cantenido de un Charbuffer, 1 si había y -1 si no pudo o está vacío
    public int read(CharBuffer target) throws IOException {
        int remaining = remaining();
        if (target == this) {
            if (remaining == 0) {
                return -1;
            }
            throw new IllegalArgumentException("target == this");
        }
        if (remaining == 0) {
            return limit > 0 && target.remaining() == 0 ? 0 : -1;
        }
        remaining = Math.min(target.remaining(), remaining);
        if (remaining > 0) {
            char[] chars = new char[remaining];
            get(chars);
            target.put(chars);
        }
        return remaining;
    }
}
