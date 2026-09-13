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

public final class CharArrayBuffer extends CharBuffer {
	private final char[] backingArray;
	private final int arrayOffset;
	private final boolean isReadOnly;
	
	// Constructores
    public CharArrayBuffer(int size) {
		super(size, 0);
		this.arrayOffset = 0;	
		this.isReadOnly = false;	
        backingArray = new char[size];
    }
    
	public CharArrayBuffer(char[] array) {
		this(array.length, array, 0, false);
	}

	private CharArrayBuffer(int capacity, char[] backingArray, int arrayOffset, boolean isReadOnly) {
		super(capacity, 0);
		this.backingArray = backingArray;
		this.arrayOffset = arrayOffset;
		this.isReadOnly = isReadOnly;
	}

	private static CharArrayBuffer copy(CharArrayBuffer other, int markOfOther, boolean isReadOnly) {
		CharArrayBuffer buf = new CharArrayBuffer(other.capacity(), other.backingArray, other.arrayOffset, isReadOnly);
		buf.limit = other.limit;
		buf.position = other.position();
		buf.mark = markOfOther;
		return buf;
	}

    @Override 
	public CharBuffer asReadOnlyBuffer() {
		return copy(this, mark, true);
	}

    @Override 
	public CharBuffer compact() {
		if (isReadOnly) {
		  throw new ReadOnlyBufferException();
		}
		System.arraycopy(backingArray, position + arrayOffset, backingArray, arrayOffset, remaining());
		position = limit - position;
		limit = capacity;
		mark = UNSET_MARK;
		return this;
    }
	
	@Override 
	public CharBuffer duplicate() {
		return copy(this, mark, isReadOnly);
	}

    @Override 
	public CharBuffer slice() {
		return new CharArrayBuffer(remaining(), backingArray, arrayOffset + position, isReadOnly);
    }

    @Override 
	public boolean isReadOnly() {
		return isReadOnly;
    }

    @Override 
	char[] protectedArray() {
		if (isReadOnly) {
		  throw new ReadOnlyBufferException();
		}
		return backingArray;
    }

    @Override 
	int protectedArrayOffset() {
		if (isReadOnly) {
		  throw new ReadOnlyBufferException();
		}
		return arrayOffset;
    }

	@Override 
	boolean protectedHasArray() {
		if (isReadOnly) {
		  return false;
		}
		return true;
    }

	@Override 
	public final char get() {
		if (position == limit) {
		  throw new BufferUnderflowException();
		}
		return backingArray[arrayOffset + position++];
    }

	@Override 
	public final char get(int index) {
		checkIndex(index);
		return backingArray[arrayOffset + index];
    }

    @Override 
	public final CharBuffer get(char[] dst, int srcOffset, int charCount) {
		if (charCount > remaining()) {
		  throw new BufferUnderflowException();
		}
		System.arraycopy(backingArray, arrayOffset + position, dst, srcOffset, charCount);
		position += charCount;
		return this;
    }

    @Override public final boolean isDirect() {
		return false;
	}

    @Override 
	public final ByteOrder order() {
		return ByteOrder.nativeOrder();
	}

	@Override 
	public final CharBuffer subSequence(int start, int end) {
		checkStartEndRemaining(start, end);
		CharBuffer result = duplicate();
		result.limit(position + end);
		result.position(position + start);
		return result;
    }

	@Override 
	public CharBuffer put(char c) {
		if (isReadOnly) {
		  throw new ReadOnlyBufferException();
		}
		if (position == limit) {
		  throw new BufferOverflowException();
		}
		backingArray[arrayOffset + position++] = c;
		return this;
    }

	@Override 
	public CharBuffer put(int index, char c) {
		if (isReadOnly) {
		  throw new ReadOnlyBufferException();
		}
		checkIndex(index);
		backingArray[arrayOffset + index] = c;
		return this;
    }

	@Override 
	public CharBuffer put(char[] src, int srcOffset, int charCount) {
		if (isReadOnly) {
		  throw new ReadOnlyBufferException();
		}
		if (charCount > remaining()) {
		  throw new BufferOverflowException();
		}
		System.arraycopy(src, srcOffset, backingArray, arrayOffset + position, charCount);
		position += charCount;
		return this;
    }

	@Override 
	public final String toString() {
		return String.copyValueOf(backingArray, arrayOffset + position, remaining());
	}
}
