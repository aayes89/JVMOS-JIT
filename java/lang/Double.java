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

public final class Double extends Number implements Comparable<Double> {
    static final int EXPONENT_BIAS = 1023;

    static final int EXPONENT_BITS = 12;
    static final int MANTISSA_BITS = 52;
    static final int NON_MANTISSA_BITS = 12;

    static final long SIGN_MASK     = 0x8000000000000000L;
    static final long EXPONENT_MASK = 0x7ff0000000000000L;
    static final long MANTISSA_MASK = 0x000fffffffffffffL;

    private static final long serialVersionUID = -9172774392245257468L;

    
    private final double value;
    public static final double MAX_VALUE = 1.79769313486231570e+308;   
    public static final double MIN_VALUE = 5e-324;
    /* 4.94065645841246544e-324 gets rounded to 9.88131e-324 */   
    public static final double NaN = 0.0 / 0.0;   
    public static final double POSITIVE_INFINITY = 1.0 / 0.0;
    public static final double NEGATIVE_INFINITY = -1.0 / 0.0;
    public static final double MIN_NORMAL = 2.2250738585072014E-308;
    public static final int MAX_EXPONENT = 1023;
    public static final int MIN_EXPONENT = -1022;
	
    @SuppressWarnings("unchecked")
    public static final Class<Double> TYPE = (Class<Double>) double[].class.getComponentType();
    
    public static final int SIZE = 64;

    public Double(double value) {
        this.value = value;
    }
    
    public Double(String string) throws NumberFormatException {
        this(parseDouble(string));
    }
    
    public int compareTo(Double object) {
        return compare(value, object.value);
    }

    public byte byteValue() {
        return (byte) value;
    }
    
    public static long doubleToLongBits(double value) {
        if (value != value) {
            return 0x7ff8000000000000L;  // NaN.
        } else {
            return doubleToRawLongBits(value);
        }
    }
    
    public static long doubleToRawLongBits(double value){
		// TODO en el JIT
		return 0L;
	}
    
    public double doubleValue() {
        return value;
    }

    
    public boolean equals(Object object) {
        return (object instanceof Double) &&
                (doubleToLongBits(this.value) == doubleToLongBits(((Double) object).value));
    }

    public float floatValue() {
        return (float) value;
    }

    
    public int hashCode() {
        long v = doubleToLongBits(value);
        return (int) (v ^ (v >>> 32));
    }

    public int intValue() {
        return (int) value;
    }
   
    public boolean isInfinite() {
        return isInfinite(value);
    }
    
    public static boolean isInfinite(double d) {
        return (d == POSITIVE_INFINITY) || (d == NEGATIVE_INFINITY);
    }
    
    public boolean isNaN() {
        return isNaN(value);
    }
   
    public static boolean isNaN(double d) {
        return d != d;
    }

    public static double longBitsToDouble(long bits){
		// TODO en el JIT
		return 0.0;
	}

    
    public long longValue() {
        return (long) value;
    }
    
    public static double parseDouble(String string) throws NumberFormatException {
        return StringToReal.parseDouble(string);
    }

    public short shortValue() {
        return (short) value;
    }

    public String toString() {
        return Double.toString(value);
    }
    
    public static String toString(double d) {
        return RealToString.getInstance().doubleToString(d);
    }
    
    public static Double valueOf(String string) throws NumberFormatException {
        return parseDouble(string);
    }
    
    public static int compare(double double1, double double2) {
        // Non-zero, non-NaN checking.
        if (double1 > double2) {
            return 1;
        }
        if (double2 > double1) {
            return -1;
        }
        if (double1 == double2 && 0.0d != double1) {
            return 0;
        }

        // NaNs are equal to other NaNs and larger than any other double
        if (isNaN(double1)) {
            if (isNaN(double2)) {
                return 0;
            }
            return 1;
        } else if (isNaN(double2)) {
            return -1;
        }

        // Deal with +0.0 and -0.0
        long d1 = doubleToRawLongBits(double1);
        long d2 = doubleToRawLongBits(double2);
        // The below expression is equivalent to:
        // (d1 == d2) ? 0 : (d1 < d2) ? -1 : 1
        return (int) ((d1 >> 63) - (d2 >> 63));
    }
   
    public static Double valueOf(double d) {
        return new Double(d);
    }
    
    public static String toHexString(double d) {
        /*
         * Reference: http://en.wikipedia.org/wiki/IEEE_754-1985
         */
        if (d != d) {
            return "NaN";
        }
        if (d == POSITIVE_INFINITY) {
            return "Infinity";
        }
        if (d == NEGATIVE_INFINITY) {
            return "-Infinity";
        }

        long bitValue = doubleToLongBits(d);

        boolean negative = (bitValue & 0x8000000000000000L) != 0;
        // mask exponent bits and shift down
        long exponent = (bitValue & 0x7FF0000000000000L) >>> 52;
        // mask significand bits and shift up
        long significand = bitValue & 0x000FFFFFFFFFFFFFL;

        if (exponent == 0 && significand == 0) {
            return (negative ? "-0x0.0p0" : "0x0.0p0");
        }

        StringBuilder hexString = new StringBuilder(10);
        if (negative) {
            hexString.append("-0x");
        } else {
            hexString.append("0x");
        }

        if (exponent == 0) { // denormal (subnormal) value
            hexString.append("0.");
            // significand is 52-bits, so there can be 13 hex digits
            int fractionDigits = 13;
            // remove trailing hex zeros, so Integer.toHexString() won't print
            // them
            while ((significand != 0) && ((significand & 0xF) == 0)) {
                significand >>>= 4;
                fractionDigits--;
            }
            // this assumes Integer.toHexString() returns lowercase characters
            String hexSignificand = Long.toHexString(significand);

            // if there are digits left, then insert some '0' chars first
            if (significand != 0 && fractionDigits > hexSignificand.length()) {
                int digitDiff = fractionDigits - hexSignificand.length();
                while (digitDiff-- != 0) {
                    hexString.append('0');
                }
            }
            hexString.append(hexSignificand);
            hexString.append("p-1022");
        } else { // normal value
            hexString.append("1.");
            // significand is 52-bits, so there can be 13 hex digits
            int fractionDigits = 13;
            // remove trailing hex zeros, so Integer.toHexString() won't print
            // them
            while ((significand != 0) && ((significand & 0xF) == 0)) {
                significand >>>= 4;
                fractionDigits--;
            }
            // this assumes Integer.toHexString() returns lowercase characters
            String hexSignificand = Long.toHexString(significand);

            // if there are digits left, then insert some '0' chars first
            if (significand != 0 && fractionDigits > hexSignificand.length()) {
                int digitDiff = fractionDigits - hexSignificand.length();
                while (digitDiff-- != 0) {
                    hexString.append('0');
                }
            }

            hexString.append(hexSignificand);
            hexString.append('p');
            // remove exponent's 'bias' and convert to a string
            hexString.append(Long.toString(exponent - 1023));
        }
        return hexString.toString();
    }
}
