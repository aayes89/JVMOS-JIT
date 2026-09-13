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

public abstract class Buffer {
    // sin marca
    static final int UNSET_MARK = -1;

    // cantidad real
    final int capacity;

    // límite
    int limit;

    // quitar marca
    int mark = UNSET_MARK;

    // posición actual
    int position = 0;

    // log base 2 de la longitud actual
    final int _elementSizeShift;
	
    final long effectiveDirectAddress;

    Buffer(int elementSizeShift, int capacity, long effectiveDirectAddress) {
        this._elementSizeShift = elementSizeShift;
        if (capacity < 0) {
            throw new IllegalArgumentException("capacity < 0: " + capacity);
        }
        this.capacity = this.limit = capacity;
        this.effectiveDirectAddress = effectiveDirectAddress;
    }

    // Devuelve arreglo como objetos
    public abstract Object array();

    // Devuelve posición del arreglo
    public abstract int arrayOffset();

    // Devuelve cantidad real 
    public final int capacity() {
        return capacity;
    }

    // para operaciones PUT/GET
    void checkIndex(int index) {
        if (index < 0 || index >= limit) {
            throw new IndexOutOfBoundsException("index=" + index + ", limit=" + limit);
        }
    }

    // Usado en operaciones de ByteBuffer
    void checkIndex(int index, int sizeOfType) {
        if (index < 0 || index > limit - sizeOfType) {
            throw new IndexOutOfBoundsException("index=" + index + ", limit=" + limit +
                    ", size of type=" + sizeOfType);
        }
    }

    int checkGetBounds(int bytesPerElement, int length, int offset, int count) {
        int byteCount = bytesPerElement * count;
        if ((offset | count) < 0 || offset > length || length - offset < count) {
            throw new IndexOutOfBoundsException("offset=" + offset +
                    ", count=" + count + ", length=" + length);
        }
        if (byteCount > remaining()) {
            throw new BufferUnderflowException();
        }
        return byteCount;
    }

    int checkPutBounds(int bytesPerElement, int length, int offset, int count) {
        int byteCount = bytesPerElement * count;
        if ((offset | count) < 0 || offset > length || length - offset < count) {
            throw new IndexOutOfBoundsException("offset=" + offset + ", count=" + count + ", length=" + length);
        }
        if (byteCount > remaining()) {
            throw new BufferOverflowException();
        }
        if (isReadOnly()) {
            throw new ReadOnlyBufferException();
        }
        return byteCount;
    }

    void checkStartEndRemaining(int start, int end) {
        if (end < start || start < 0 || end > remaining()) {
            throw new IndexOutOfBoundsException("start=" + start + ", end=" + end + ", remaining()=" + remaining());
        }
    }

    // limpiar el buffer
    public final Buffer clear() {
        position = 0;
        mark = UNSET_MARK;
        limit = capacity;
        return this;
    }

    // alterna orden de lectura en el bffer
    public final Buffer flip() {
        limit = position;
        position = 0;
        mark = UNSET_MARK;
        return this;
    }

    // True si hay arreglo, False de lo contrario
    public abstract boolean hasArray();

    // Indica si quedan algún elemento
    public final boolean hasRemaining() {
        return position < limit;
    }

    // True si es direct
    public abstract boolean isDirect();

    // Sólo lectura?
    public abstract boolean isReadOnly();

    final void checkWritable() {
        if (isReadOnly()) {
            throw new IllegalArgumentException("Read-only buffer");
        }
    }

    // Retorna variable limit
    public final int limit() {
        return limit;
    }

    // Establecer nuevo límite
    public final Buffer limit(int newLimit) {
        if (newLimit < 0 || newLimit > capacity) {
            throw new IllegalArgumentException("Bad limit (capacity " + capacity + "): " + newLimit);
        }

        limit = newLimit;
        if (position > newLimit) {
            position = newLimit;
        }
        if ((mark != UNSET_MARK) && (mark > newLimit)) {
            mark = UNSET_MARK;
        }
        return this;
    }

    // marcar la posición actual
    public final Buffer mark() {
        mark = position;
        return this;
    }

    // Devuelve posición actual
    public final int position() {
        return position;
    }

    // Establece nueva posición
    public final Buffer position(int newPosition) {
        positionImpl(newPosition);
        return this;
    }

    void positionImpl(int newPosition) {
        if (newPosition < 0 || newPosition > limit) {
            throw new IllegalArgumentException("Bad position (limit " + limit + "): " + newPosition);
        }

        position = newPosition;
        if ((mark != UNSET_MARK) && (mark > position)) {
            mark = UNSET_MARK;
        }
    }

    // Devuelve cantidad restante de posición
    public final int remaining() {
        return limit - position;
    }

    // reinicia la posición a por defecto
    public final Buffer reset() {
        if (mark == UNSET_MARK) {
            throw new InvalidMarkException("Mark not set");
        }
        position = mark;
        return this;
    }

    // revobina la posición actual
    public final Buffer rewind() {
        position = 0;
        mark = UNSET_MARK;
        return this;
    }

    // Devuelve descripción del buffer
    @Override public String toString() {
        return getClass().getName() +
            "[position=" + position + ",limit=" + limit + ",capacity=" + capacity + "]";
    }

    // Para pruebas nada más. (no se usa)
    public final int getElementSizeShift() {
        return _elementSizeShift;
    }
}
