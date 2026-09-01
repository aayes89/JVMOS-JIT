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

package java.lang;

public class StringBuilder implements CharSequence {
    
    private byte[] value;
    private int count;

    public StringBuilder() {
        value = new byte[16];
        count = 0;
    }
    public StringBuilder(int size) {
        value = new byte[size];
        count = 0;
    }

    public StringBuilder(String str) {
        this();
        append(str);
    }

    private void expandCapacity(int minimumCapacity) {
        int newCapacity = value.length * 2 + 2;
        if (newCapacity < minimumCapacity) {
            newCapacity = minimumCapacity;
        }
        byte[] newValue = new byte[newCapacity];
        System.arraycopy(value, 0, newValue, 0, count);
        value = newValue;
    }

    @Override
    public int length() {
        return count;
    }

    @Override
    public char charAt(int index) {
        if (index >= 0 && index < count) {
            return (char) (value[index] & 0xFF);
        }
        System.out.println("StringBuilderIndexOutOfBounds");
        return 0;
    }

    @Override
    public CharSequence subSequence(int start, int end) {
        if (start < 0 || end > count || start > end) {
            System.out.println("StringBuilderIndexOutOfBounds");
            return new String(new byte[0]);
        }
        byte[] subBytes = new byte[end - start];
        System.arraycopy(this.value, start, subBytes, 0, end - start);
        return new String(subBytes);
    }

    // Permite concatenar cualquier interfaz CharSequence
    public StringBuilder append(CharSequence s) {
        if (s == null) {
            return append("null");
        }
        return append(s.toString());
    }

    // Permite concatenar cualquier Objeto en general
    public StringBuilder append(Object obj) {
        return append(String.valueOf(obj));
    }

    public StringBuilder append(String str) {
        if (str == null) str = "null";
        int len = str.length();
        if (count + len > value.length) {
            expandCapacity(count + len);
        }
        byte[] strBytes = str.getBytes(); 
        System.arraycopy(strBytes, 0, value, count, len);
        count += len;
        return this;
    }

    public StringBuilder append(char c) {
        if (count + 1 > value.length) {
            expandCapacity(count + 1);
        }
        value[count++] = (byte) c;
        return this;
    }

    public StringBuilder append(int i) {
        if (i == 0) return append("0");
        boolean negative = i < 0;
        if (negative) i = -i;
        
        int temp = i;
        int len = 0;
        while (temp > 0) {
            len++;
            temp /= 10;
        }
        if (negative) len++;
        
        if (count + len > value.length) {
            expandCapacity(count + len);
        }
        
        int pos = count + len - 1;
        temp = i;
        while (temp > 0) {
            value[pos--] = (byte) ('0' + (temp % 10));
            temp /= 10;
        }
        if (negative) value[pos] = '-';
        
        count += len;
        return this;
    }

    public StringBuilder append(long l) { return append(String.valueOf(l)); }
    public StringBuilder append(float f) { return append(String.valueOf(f)); }
    public StringBuilder append(double d) { return append(String.valueOf(d)); }
    public StringBuilder append(boolean b) { return append(String.valueOf(b)); }

    @Override
    public String toString() {
        byte[] result = new byte[count];
        System.arraycopy(value, 0, result, 0, count);
        return new String(result);
    }
}
