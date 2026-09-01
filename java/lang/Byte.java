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

public final class Byte extends Number implements Comparable<Byte> {
    // tomado de la implementación original
    private static final long serialVersionUID = -7183698231559129828L;
    
    // variables locales
    private final byte value;
    public static final byte MAX_VALUE = (byte) 0x7F;
    public static final byte MIN_VALUE = (byte) 0x80;
    public static final int SIZE = 8;

    @SuppressWarnings("unchecked")
    public static final Class<Byte> TYPE = (Class<Byte>) byte[].class.getComponentType();
   
    public Byte(byte value) {
        this.value = value;
    }
   
    public Byte(String string) throws NumberFormatException {
        this(parseByte(string));
    }
    
    public byte byteValue() {
        return value;
    }
    
    public int compareTo(Byte object) {
        return compare(value, object.value);
    }

    public static int compare(byte lhs, byte rhs) {
        return lhs > rhs ? 1 : (lhs < rhs ? -1 : 0);
    }
   
    public static Byte decode(String string) throws NumberFormatException {
        int intValue = Integer.decode(string);
        byte result = (byte) intValue;
        if (result == intValue) {
            return valueOf(result);
        }
        //throw 
        new NumberFormatException("Value out of range for byte: \"" + string + "\"");
    }

    public double doubleValue() {
        return value;
    }
   
    public boolean equals(Object object) {
        return (object == this) || ((object instanceof Byte) && (((Byte) object).value == value));
    }

    public float floatValue() {
        return value;
    }

    public int hashCode() {
        return value;
    }

    public int intValue() {
        return value;
    }

    public long longValue() {
        return value;
    }
    
    public static byte parseByte(String string) throws NumberFormatException {
        return parseByte(string, 10);
    }
   
    public static byte parseByte(String string, int radix) throws NumberFormatException {
        int intValue = Integer.parseInt(string, radix);
        byte result = (byte) intValue;
        if (result == intValue) {
            return result;
        }
        //throw 
        new NumberFormatException("Value out of range for byte: \"" + string + "\"");
    }

    public short shortValue() {
        return value;
    }

    public String toString() {
        return Integer.toString(value);
    }
   
    public static String toHexString(byte b, boolean upperCase) {
        return IntegralToString.byteToHexString(b, upperCase);
    }
    
    public static String toString(byte value) {
        return Integer.toString(value);
    }
    
    public static Byte valueOf(String string) throws NumberFormatException {
        return valueOf(parseByte(string));
    }
    
    public static Byte valueOf(String string, int radix) throws NumberFormatException {
        return valueOf(parseByte(string, radix));
    }
    
    public static Byte valueOf(byte b) {
        return VALUES[b + 128];
    }
   
    private static final Byte[] VALUES = new Byte[256];

    static {
        for (int i = -128; i < 128; i++) {
            VALUES[i + 128] = new Byte((byte) i);
        }
    }
}
