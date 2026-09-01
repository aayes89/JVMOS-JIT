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

public final class Long extends Number implements Comparable<Long> {

    private static final long serialVersionUID = 4290774380558885855L;

    /**
     * The value which the receiver represents.
     */
    private final long value;

    /**
     * Constant for the maximum {@code long} value, 2<sup>63</sup>-1.
     */
    public static final long MAX_VALUE = 0x7FFFFFFFFFFFFFFFL;

    public static final long MIN_VALUE = 0x8000000000000000L;

    @SuppressWarnings("unchecked")
    public static final Class<Long> TYPE
            = (Class<Long>) long[].class.getComponentType();
    // Note: Long.TYPE can't be set to "long.class", since *that* is
    // defined to be "java.lang.Long.TYPE";
    
    public static final int SIZE = 64;
    
    public Long(long value) {
        this.value = value;
    }

  
    public Long(String string) throws NumberFormatException {
        this(parseLong(string));
    }

    public byte byteValue() {
        return (byte) value;
    }

    public int compareTo(Long object) {
        return compare(value, object.value);
    }

   
    public static int compare(long lhs, long rhs) {
        return lhs < rhs ? -1 : (lhs == rhs ? 0 : 1);
    }

    private static NumberFormatException invalidLong(String s) {
        throw new NumberFormatException("Invalid long: \"" + s + "\"");
    }

   
    public static Long decode(String string) throws NumberFormatException {
        int length = string.length();
        if (length == 0) {
            throw invalidLong(string);
        }

        int i = 0;
        char firstDigit = string.charAt(i);
        boolean negative = firstDigit == '-';
        if (negative || firstDigit == '+') {
            if (length == 1) {
                throw invalidLong(string);
            }
            firstDigit = string.charAt(++i);
        }

        int base = 10;
        if (firstDigit == '0') {
            if (++i == length) {
                return valueOf(0L);
            }
            if ((firstDigit = string.charAt(i)) == 'x' || firstDigit == 'X') {
                if (i == length) {
                    throw invalidLong(string);
                }
                i++;
                base = 16;
            } else {
                base = 8;
            }
        } else if (firstDigit == '#') {
            if (i == length) {
                throw invalidLong(string);
            }
            i++;
            base = 16;
        }

        long result = parse(string, i, base, negative);
        return valueOf(result);
    }

    public double doubleValue() {
        return value;
    }

    
    public boolean equals(Object o) {
        return (o instanceof Long) && (((Long) o).value == value);
    }

    public float floatValue() {
        return value;
    }
   
    public static Long getLong(String string) {
        if (string == null || string.length() == 0) {
            return null;
        }
        String prop = System.getProperty(string);
        if (prop == null) {
            return null;
        }
        try {
            return decode(prop);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

   
    public static Long getLong(String string, long defaultValue) {
        if (string == null || string.length() == 0) {
            return valueOf(defaultValue);
        }
        String prop = System.getProperty(string);
        if (prop == null) {
            return valueOf(defaultValue);
        }
        try {
            return decode(prop);
        } catch (NumberFormatException ex) {
            return valueOf(defaultValue);
        }
    }

   
    public static Long getLong(String string, Long defaultValue) {
        if (string == null || string.length() == 0) {
            return defaultValue;
        }
        String prop = System.getProperty(string);
        if (prop == null) {
            return defaultValue;
        }
        try {
            return decode(prop);
        } catch (NumberFormatException ex) {
            return defaultValue;
        }
    }

    
    public int hashCode() {
        return (int) (value ^ (value >>> 32));
    }

    public int intValue() {
        return (int) value;
    }

   
    public long longValue() {
        return value;
    }

    
    public static long parseLong(String string) throws NumberFormatException {
        return parseLong(string, 10);
    }
    
    public static long parseLong(String string, int radix) throws NumberFormatException {
        if (radix < Character.MIN_RADIX || radix > Character.MAX_RADIX) {
            throw new NumberFormatException("Invalid radix: " + radix);
        }
        if (string == null || string.isEmpty()) {
            throw invalidLong(string);
        }
        char firstChar = string.charAt(0);
        int firstDigitIndex = (firstChar == '-' || firstChar == '+') ? 1 : 0;
        if (firstDigitIndex == string.length()) {
            throw invalidLong(string);
        }

        return parse(string, firstDigitIndex, radix, firstChar == '-');
    }

    private static long parse(String string, int offset, int radix, boolean negative) {
        long max = Long.MIN_VALUE / radix;
        long result = 0;
        int length = string.length();
        while (offset < length) {
            int digit = Character.digit(string.charAt(offset++), radix);
            if (digit == -1) {
                throw invalidLong(string);
            }
            if (max > result) {
                throw invalidLong(string);
            }
            long next = result * radix - digit;
            if (next > result) {
                throw invalidLong(string);
            }
            result = next;
        }
        if (!negative) {
            result = -result;
            if (result < 0) {
                throw invalidLong(string);
            }
        }
        return result;
    }

    public static long parsePositiveLong(String string) throws NumberFormatException {
        return parsePositiveLong(string, 10);
    }

    public static long parsePositiveLong(String string, int radix) throws NumberFormatException {
        if (radix < Character.MIN_RADIX || radix > Character.MAX_RADIX) {
            throw new NumberFormatException("Invalid radix: " + radix);
        }
        if (string == null || string.length() == 0) {
            throw invalidLong(string);
        }
        return parse(string, 0, radix, false);
    }

    
    public short shortValue() {
        return (short) value;
    }

    public static String toBinaryString(long v) {
        return IntegralToString.longToBinaryString(v);
    }

    public static String toHexString(long v) {
        return IntegralToString.longToHexString(v);
    }

    public static String toOctalString(long v) {
        return IntegralToString.longToOctalString(v);
    }

    public String toString() {
        return Long.toString(value);
    }

    public static String toString(long n) {
        return IntegralToString.longToString(n);
    }

    public static String toString(long v, int radix) {
        return IntegralToString.longToString(v, radix);
    }

    public static Long valueOf(String string) throws NumberFormatException {
        return valueOf(parseLong(string));
    }

    public static Long valueOf(String string, int radix) throws NumberFormatException {
        return valueOf(parseLong(string, radix));
    }

    public static long highestOneBit(long v) {
        // Hacker's Delight, Figure 3-1
        v |= (v >> 1);
        v |= (v >> 2);
        v |= (v >> 4);
        v |= (v >> 8);
        v |= (v >> 16);
        v |= (v >> 32);
        return v - (v >>> 1);
    }

    public static long lowestOneBit(long v) {
        return v & -v;
    }

    public static int numberOfLeadingZeros(long v) {
        // After Hacker's Delight, Figure 5-6
        if (v < 0) {
            return 0;
        }
        if (v == 0) {
            return 64;
        }
        // On a 64-bit VM, the two previous tests should probably be replaced by
        // if (v <= 0) return ((int) (~v >> 57)) & 64;

        int n = 1;
        int i = (int) (v >>> 32);
        if (i == 0) {
            n +=  32;
            i = (int) v;
        }
        if (i >>> 16 == 0) {
            n +=  16;
            i <<= 16;
        }
        if (i >>> 24 == 0) {
            n +=  8;
            i <<= 8;
        }
        if (i >>> 28 == 0) {
            n +=  4;
            i <<= 4;
        }
        if (i >>> 30 == 0) {
            n +=  2;
            i <<= 2;
        }
        return n - (i >>> 31);
    }

 
    public static int numberOfTrailingZeros(long v) {
        int low = (int) v;
        return low !=0 ? Integer.numberOfTrailingZeros(low)
                       : 32 + Integer.numberOfTrailingZeros((int) (v >>> 32));
    }

    public static int bitCount(long v) {
        // Combines techniques from several sources
        v -=  (v >>> 1) & 0x5555555555555555L;
        v = (v & 0x3333333333333333L) + ((v >>> 2) & 0x3333333333333333L);
        int i =  ((int)(v >>> 32)) + (int) v;
        i = (i & 0x0F0F0F0F) + ((i >>> 4) & 0x0F0F0F0F);
        i += i >>> 8;
        i += i >>> 16;
        return i  & 0x0000007F;
    }


    public static long rotateLeft(long v, int distance) {
        // Shift distances are mod 64 (JLS3 15.19), so we needn't mask -distance
        return (v << distance) | (v >>> -distance);
    }

  
    public static long rotateRight(long v, int distance) {
        // Shift distances are mod 64 (JLS3 15.19), so we needn't mask -distance
        return (v >>> distance) | (v << -distance);
    }

   
    public static long reverseBytes(long v) {
        // Hacker's Delight 7-1, with minor tweak from Veldmeijer
        // http://graphics.stanford.edu/~seander/bithacks.html
        v = ((v >>> 8) & 0x00FF00FF00FF00FFL) | ((v & 0x00FF00FF00FF00FFL) << 8);
        v = ((v >>>16) & 0x0000FFFF0000FFFFL) | ((v & 0x0000FFFF0000FFFFL) <<16);
        return ((v >>>32)                   ) | ((v                      ) <<32);
    }

   
    public static long reverse(long v) {
        // Hacker's Delight 7-1, with minor tweak from Veldmeijer
        // http://graphics.stanford.edu/~seander/bithacks.html
        v = ((v >>> 1) & 0x5555555555555555L) | ((v & 0x5555555555555555L) << 1);
        v = ((v >>> 2) & 0x3333333333333333L) | ((v & 0x3333333333333333L) << 2);
        v = ((v >>> 4) & 0x0F0F0F0F0F0F0F0FL) | ((v & 0x0F0F0F0F0F0F0F0FL) << 4);
        v = ((v >>> 8) & 0x00FF00FF00FF00FFL) | ((v & 0x00FF00FF00FF00FFL) << 8);
        v = ((v >>>16) & 0x0000FFFF0000FFFFL) | ((v & 0x0000FFFF0000FFFFL) <<16);
        return ((v >>>32)                   ) | ((v                      ) <<32);
    }

  
    public static int signum(long v) {
        return v < 0 ? -1 : (v == 0 ? 0 : 1);
    }

   
    public static Long valueOf(long v) {
        return  v >= 128 || v < -128 ? new Long(v) : SMALL_VALUES[((int) v) + 128];
    }

    
    private static final Long[] SMALL_VALUES = new Long[256];

    static {
        for (int i = -128; i < 128; i++) {
            SMALL_VALUES[i + 128] = new Long(i);
        }
    }
}
