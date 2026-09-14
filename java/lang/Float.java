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

public final class Float extends Number implements Comparable<Float> {
    static final int EXPONENT_BIAS = 127;
    static final int EXPONENT_BITS = 9;
    static final int MANTISSA_BITS = 23;
    static final int NON_MANTISSA_BITS = 9;

    static final int SIGN_MASK     = 0x80000000;
    static final int EXPONENT_MASK = 0x7f800000;
    static final int MANTISSA_MASK = 0x007fffff;

    private static final long serialVersionUID = -2671257302660747028L;

    private final float value;

    public static final float MAX_VALUE = 3.40282346638528860e+38f;
    public static final float MIN_VALUE = 1.40129846432481707e-45f;
    public static final float NaN = 0.0f / 0.0f;
    public static final float POSITIVE_INFINITY = 1.0f / 0.0f;
    public static final float NEGATIVE_INFINITY = -1.0f / 0.0f;
    public static final float MIN_NORMAL = 1.1754943508222875E-38f;

    public static final int MAX_EXPONENT = 127;
    public static final int MIN_EXPONENT = -126;

    @SuppressWarnings("unchecked")
    public static final Class<Float> TYPE = (Class<Float>) float[].class.getComponentType();
    public static final int SIZE = 32;

    public Float(float value) { this.value = value; }
    public Float(double value) { this.value = (float) value; }
    public Float(String string) { this(parseFloat(string)); }

    public int compareTo(Float object) { return compare(value, object.value); }

    @Override public byte byteValue() { return (byte) value; }
    @Override public double doubleValue() { return value; }
    @Override public float floatValue() { return value; }
    @Override public int intValue() { return (int) value; }
    @Override public long longValue() { return (long) value; }
    @Override public short shortValue() { return (short) value; }

    @Override
    public boolean equals(Object object) {
        return (object instanceof Float) &&
                (floatToIntBits(this.value) == floatToIntBits(((Float) object).value));
    }

    public static int floatToIntBits(float value) {
        if (value != value) {
			return 0x7fc00000;  // NaN.
		}
        return floatToRawIntBits(value);
    }

    // BAREMETAL STUB: El cast puro a nivel de bits requiere soporte en el JIT/HAL
    public static int floatToRawIntBits(float value) {
        return 0; 
    }

    // BAREMETAL STUB
    public static float intBitsToFloat(int bits) {
        return 0.0f; 
    }

    @Override
    public int hashCode() {
        return floatToIntBits(value);
    }

    public boolean isInfinite() { 
		return isInfinite(value); 
	}
    public static boolean isInfinite(float f) { 
		return (f == POSITIVE_INFINITY) || (f == NEGATIVE_INFINITY); 
	}

    public boolean isNaN() { 
		return isNaN(value); 
	}
    public static boolean isNaN(float f) {
		return f != f;
	}

    // Obtener float de una cadena de texto    
    public static float parseFloat(String string) {
        if (string == null) {
			return 0.0f;
		}
        String s = string.trim();
        int len = s.length();
        if (len == 0){
			return 0.0f;
		}

        boolean negative = false;
        int i = 0;
        char first = s.charAt(0);
        
        if (first == '-') {
            negative = true;
            i++;
        } else if (first == '+') {
            i++;
        }

        // Casos especiales (Infinity, NaN)
        char last = s.charAt(len - 1);
        if (last == 'N' || last == 'y') {
            if (s.indexOf("NaN") != -1) return NaN;
            if (s.indexOf("Infinity") != -1) return negative ? NEGATIVE_INFINITY : POSITIVE_INFINITY;
        }

        float integerPart = 0.0f;
        float fractionPart = 0.0f;
        float fractionScale = 0.1f;
        boolean inFraction = false;

        // Parseo matemático directo sin instanciar ningún objeto
        while (i < len) {
            char c = s.charAt(i);
            
            // Ignoramos la E (notación científica) y la f final para modelos 3D convencionales
            if (c == 'f' || c == 'F' || c == 'e' || c == 'E') {
                break;
            }
            if (c == '.') {
                inFraction = true;
                i++;
                continue;
            }
            if (c >= '0' && c <= '9') {
                if (!inFraction) {
                    integerPart = integerPart * 10.0f + (c - '0');
                } else {
                    fractionPart += (c - '0') * fractionScale;
                    fractionScale *= 0.1f;
                }
            }
            i++;
        }

        float result = integerPart + fractionPart;
        return negative ? -result : result;
    }

    @Override
    public String toString() { 
		return Float.toString(value); 
	}

    // Devuelve float como cadena de texto
    public static String toString(float f) {
		StringBuilder sb = new StringBuilder();
		sb.append(f);
        return sb.toString(); 
    }

    public static Float valueOf(String string) { 
		return new Float(parseFloat(string));
	}
    public static Float valueOf(float f) { 
		return new Float(f); 
	}
    public static int compare(float float1, float float2) {

		if (float1 > float2) {
			return 1;
		}

		if (float2 > float1) {
			return -1;
		}

		if (float1 == float2 && 0.0f != float1) {
			return 0;
		}

		if (isNaN(float1)) {
			return isNaN(float2) ? 0 : 1;
		}

		if (isNaN(float2)) {
			return -1;
		}

		int f1 = floatToRawIntBits(float1);
		int f2 = floatToRawIntBits(float2);

		return (f1 >> 31) - (f2 >> 31);
	}

    public static String toHexString(float f) {
		if (isNaN(f)) {
			return "NaN";
		}
		if (f == POSITIVE_INFINITY) {
			return "Infinity";
		}
		if (f == NEGATIVE_INFINITY) {
			return "-Infinity";
		}
		if (f == 0.0f) {
			return (1.0f / f < 0.0f) ? "-0x0.0p0" : "0x0.0p0";
		}
		boolean negative = f < 0.0f;
		if (negative) {
			f = -f;
		}
		int exponent = 0;
		if (f >= MIN_NORMAL) {
			while (f >= 2.0f) {
				f *= 0.5f;
				exponent++;
			}
			while (f < 1.0f) {
				f *= 2.0f;
				exponent--;
			}
		} else {
			exponent = -126;
			while (f < MIN_NORMAL) {
				f *= 2.0f;
			}
			while (f >= 2.0f) {
				f *= 0.5f;
				exponent++;
			}
		}
		int mantissa = (int) ((f - 1.0f) * 8388608.0f);
		String result = negative ? "-0x1." : "0x1.";
		String hex = Integer.toHexString(mantissa);
		while (hex.length() < 6) {
			hex = "0" + hex;
		}
		while (hex.length() > 0 && hex.charAt(hex.length() - 1) == '0') {
			hex = hex.substring(0, hex.length() - 1);
		}
		if (hex.length() == 0) {
			hex = "0";
		}
		result += hex;
		result += "p";
		result += Integer.toString(exponent);
		return result;
	}
}
