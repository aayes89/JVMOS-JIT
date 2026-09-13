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

import java.util.Arrays;

final class CharSequenceAdapter extends CharBuffer {

    static CharSequenceAdapter copy(CharSequenceAdapter other) {
        CharSequenceAdapter buf = new CharSequenceAdapter(other.sequence);
        buf.limit = other.limit;
        buf.position = other.position;
        buf.mark = other.mark;
        return buf;
    }

    final CharSequence sequence;

    CharSequenceAdapter(CharSequence chseq) {
        super(chseq.length(), 0);
        sequence = chseq;
    }

    @Override
    public CharBuffer asReadOnlyBuffer() {
        return duplicate();
    }

    @Override
    public CharBuffer compact() {
        throw new ReadOnlyBufferException();
    }

    @Override
    public CharBuffer duplicate() {
        return copy(this);
    }

    @Override
    public char get() {
        if (position == limit) {
            throw new BufferUnderflowException();
        }
        return sequence.charAt(position++);
    }

    @Override
    public char get(int index) {
        checkIndex(index);
        return sequence.charAt(index);
    }

    @Override
    public final CharBuffer get(char[] dst, int dstOffset, int charCount) {
        Arrays.checkOffsetAndCount(dst.length, dstOffset, charCount);
        if (charCount > remaining()) {
            throw new BufferUnderflowException();
        }
        int newPosition = position + charCount;
        sequence.toString().getChars(position, newPosition, dst, dstOffset);
        position = newPosition;
        return this;
    }

    @Override
    public boolean isDirect() {
        return false;
    }

    @Override
    public boolean isReadOnly() {
        return true;
    }

    @Override
    public ByteOrder order() {
        return ByteOrder.nativeOrder();
    }

    @Override char[] protectedArray() {
        throw new UnsupportedOperationException();
    }

    @Override int protectedArrayOffset() {
        throw new UnsupportedOperationException();
    }

    @Override boolean protectedHasArray() {
        return false;
    }

    @Override
    public CharBuffer put(char c) {
        throw new ReadOnlyBufferException();
    }

    @Override
    public CharBuffer put(int index, char c) {
        throw new ReadOnlyBufferException();
    }

    @Override
    public final CharBuffer put(char[] src, int srcOffset, int charCount) {
        throw new ReadOnlyBufferException();
    }

    @Override
    public CharBuffer put(String src, int start, int end) {
        throw new ReadOnlyBufferException();
    }

    @Override
    public CharBuffer slice() {
        return new CharSequenceAdapter(sequence.subSequence(position, limit));
    }

    @Override public CharBuffer subSequence(int start, int end) {
        checkStartEndRemaining(start, end);
        CharSequenceAdapter result = copy(this);
        result.position = position + start;
        result.limit = position + end;
        return result;
    }
}
