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


public final class Integer extends Number implements Comparable<Integer> {

    private static final long serialVersionUID = 1360826667806852920L;

    // variable representiva del número
    private final int value;

    // constante para valor máximo posible en entero
    public static final int MAX_VALUE = 0x7FFFFFFF;

    // constante para valor mínimo posible en entero
    public static final int MIN_VALUE = 0x80000000;

    // para representar la cantidad de bits de un entero
    public static final int SIZE = 32;

    // tabla del algoritmo de Seal para el número de ceros finales.
    private static final byte[] NTZ_TABLE = {
        32,  0,  1, 12,  2,  6, -1, 13,   3, -1,  7, -1, -1, -1, -1, 14,
        10,  4, -1, -1,  8, -1, -1, 25,  -1, -1, -1, -1, -1, 21, 27, 15,
        31, 11,  5, -1, -1, -1, -1, -1,   9, -1, -1, 24, -1, -1, 20, 26,
        30, -1, -1, -1, -1, 23, -1, 19,  29, -1, 22, 18, 28, 17, 16, -1
    };
    
    public static final Class<Integer> TYPE = (Class<Integer>) int[].class.getComponentType();
	    
	// constructores	
    public Integer(int value) {
        this.value = value;
    }
   
    public Integer(String string) throws NumberFormatException {
        this(parseInt(string));
    }

	// obtener el byte de un entero
    @Override
    public byte byteValue() {
        return (byte) value;
    }
	
	// Comparar tipos de enteros 
    public int compareTo(Integer object) {
        return compare(value, object.value);
    }
    
	// comparar dos enteros según tamaño 
	// (-1, 0, 1) - menor, mayor que, igual
    public static int compare(int lhs, int rhs) {
        return lhs < rhs ? -1 : (lhs == rhs ? 0 : 1);
    }

	// lanzar expceción si la cadena de texto no es un entero
    private static NumberFormatException invalidInt(String s) {
        throw new NumberFormatException("Invalid int: \"" + s + "\"");
    }
	
	// obtener un entero de una cadena de texto   
    public static Integer decode(String string) throws NumberFormatException {
        int length = string.length();
        if (length == 0) {
            throw invalidInt(string);
        }
        int i = 0;
        char firstDigit = string.charAt(i);
        boolean negative = firstDigit == '-';
        if (negative || firstDigit == '+') {
            if (length == 1) {
                throw invalidInt(string);
            }
            firstDigit = string.charAt(++i);
        }

        int base = 10;
        if (firstDigit == '0') {
            if (++i == length) {
                return valueOf(0);
            }
            if ((firstDigit = string.charAt(i)) == 'x' || firstDigit == 'X') {
                if (++i == length) {
                    throw invalidInt(string);
                }
                base = 16;
            } else {
                base = 8;
            }
        } else if (firstDigit == '#') {
            if (++i == length) {
                throw invalidInt(string);
            }
            base = 16;
        }

        int result = parse(string, i, base, negative);
        return valueOf(result);
    }

    // obtener el double de una cadena de texto
	@Override
    public double doubleValue() {
        return value;
    }
    
	// comparar dos objetos como tipos de enteros
    @Override
    public boolean equals(Object o) {
        return (o instanceof Integer) && (((Integer) o).value == value);
    }

	// obtener el float de una cadena de texto
    @Override
    public float floatValue() {
        return value;
    }
    
	// obtener un entero de una cadena de texto
    public static Integer getInteger(String string) {
        if (string == null || string.length() == 0) {
            return null;
        }
        String prop = System.getProperty(string);
        if (prop == null) {
            return null;
        }
		// Evaluamos el String ANTES de decodificarlo
        if (isNumber(prop)) {
            return decode(prop);
        }
		// catch para NumberFormatException
        return null;
    }
   
    public static Integer getInteger(String string, int defaultValue) {
        if (string == null || string.length() == 0) return valueOf(defaultValue);
        
        String prop = System.getProperty(string);
        if (prop == null) return valueOf(defaultValue);
        
        // Evaluamos el String ANTES de decodificarlo
        if (isNumber(prop)) {
            return decode(prop);
        }
		// catch para NumberFormatException
        return valueOf(defaultValue);
    }
    
    public static Integer getInteger(String string, Integer defaultValue) {
        if (string == null || string.length() == 0) {
            return defaultValue;
        }
        String prop = System.getProperty(string);
        if (prop == null) {
            return defaultValue;
        }
		
		// Evaluamos el String ANTES de decodificarlo
        if (isNumber(prop)) {
            return decode(prop);
        }
		// catch para NumberFormatException
        return defaultValue;		
    }
	
	// función auxiliar para validar si es número el texto
    private static boolean isNumber(String s) {
        if (s == null || s.length() == 0) return false;
        int start = (s.charAt(0) == '-' || s.charAt(0) == '+') ? 1 : 0;
        if (start == s.length()) return false; // Era solo "-" o "+"
        
        for (int i = start; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c < '0' || c > '9') return false; 
        }
        return true;
    }

	// TODO - devuelve el hash del value
    @Override
    public int hashCode() {
        return value;
    }
    
	// obtener el valor de value
    @Override
    public int intValue() {
        return value;
    }

	// obtener long del valor entero
    @Override
    public long longValue() {
        return value;
    }

    // llamada al parser de entero
    public static int parseInt(String string) throws NumberFormatException {
        return parseInt(string, 10);
    }
	// parser de entero
    public static int parseInt(String string, int radix) throws NumberFormatException {
        if (radix < Character.MIN_RADIX || radix > Character.MAX_RADIX) {
            throw new NumberFormatException("Invalid radix: " + radix);
        }
        if (string == null || string.isEmpty()) {
            throw invalidInt(string);
        }

        char firstChar = string.charAt(0);
        int firstDigitIndex = (firstChar == '-' || firstChar == '+') ? 1 : 0;
        if (firstDigitIndex == string.length()) {
            throw invalidInt(string);
        }

        return parse(string, firstDigitIndex, radix, firstChar == '-');
    }
    
	// llamada al parser de entero con signo +
    public static int parsePositiveInt(String string) throws NumberFormatException {
        return parsePositiveInt(string, 10);
    }
    // parser de entero con signo +
    public static int parsePositiveInt(String string, int radix) throws NumberFormatException {
        if (radix < Character.MIN_RADIX || radix > Character.MAX_RADIX) {
            throw new NumberFormatException("Invalid radix: " + radix);
        }
        if (string == null || string.length() == 0) {
            throw invalidInt(string);
        }
        return parse(string, 0, radix, false);
    }

	// función parser
    private static int parse(String string, int offset, int radix, boolean negative) throws NumberFormatException {
        int max = Integer.MIN_VALUE / radix;
        int result = 0;
        int length = string.length();
        while (offset < length) {
            int digit = Character.digit(string.charAt(offset++), radix);
            if (digit == -1) {
                throw invalidInt(string);
            }
            if (max > result) {
                throw invalidInt(string);
            }
            int next = result * radix - digit;
            if (next > result) {
                throw invalidInt(string);
            }
            result = next;
        }
        if (!negative) {
            result = -result;
            if (result < 0) {
                throw invalidInt(string);
            }
        }
        return result;
    }

	// obtener short del valor entero
    @Override
    public short shortValue() {
        return (short) value;
    }
    
	// llamada a función para convertir en binario una cadena con número entero
    public static String toBinaryString(int i) {
        return IntegralToString.intToBinaryString(i);
    }
    // llamada a función para convertir en hexadecimal una cadena con número entero
    public static String toHexString(int i) {
        return IntegralToString.intToHexString(i, false, 0);
    }
    // llamada a función para convertir en Octal una cadena de texto con número entero
    public static String toOctalString(int i) {
        return IntegralToString.intToOctalString(i);
    }

	// obtener el String del valor entero
    @Override
    public String toString() {
        return Integer.toString(value);
    }
    
    public static String toString(int i) {
        return IntegralToString.intToString(i);
    }
    
    public static String toString(int i, int radix) {
        return IntegralToString.intToString(i, radix);
    }
    
	// convertir una cadena de texto a tipo entero
    public static Integer valueOf(String string) throws NumberFormatException {
        return valueOf(parseInt(string));
    }
    
    public static Integer valueOf(String string, int radix) throws NumberFormatException {
        return valueOf(parseInt(string, radix));
    }
    
	// operaciones con registro y memoria
	// obtener el bit más elevado del entero 
    public static int highestOneBit(int i) {
        i |= (i >> 1);
        i |= (i >> 2);
        i |= (i >> 4);
        i |= (i >> 8);
        i |= (i >> 16);
        return i - (i >>> 1);
    }

	// obtener el bit más bajo del entero
    public static int lowestOneBit(int i) {
        return i & -i;
    }
   
	// obtener la cantidad de ceros en cabecera
    public static int numberOfLeadingZeros(int i) {
        if (i <= 0) {
            return (~i >> 26) & 32;
        }
        int n = 1;
        if (i >> 16 == 0) {
            n +=  16;
            i <<= 16;
        }
        if (i >> 24 == 0) {
            n +=  8;
            i <<= 8;
        }
        if (i >> 28 == 0) {
            n +=  4;
            i <<= 4;
        }
        if (i >> 30 == 0) {
            n +=  2;
            i <<= 2;
        }
        return n - (i >>> 31);
    }
    
    public static int numberOfTrailingZeros(int i) {
        return NTZ_TABLE[((i & -i) * 0x0450FBAF) >>> 26];
    }

	// cantidad de bits
    public static int bitCount(int i) {
        // Hacker's Delight, Figure 5-2
        i -= (i >> 1) & 0x55555555;
        i = (i & 0x33333333) + ((i >> 2) & 0x33333333);
        i = ((i >> 4) + i) & 0x0F0F0F0F;
        i += i >> 8;
        i += i >> 16;
        return i & 0x0000003F;
    }

    // operaciones shift
    public static int rotateLeft(int i, int distance) {
        // Shift distances are mod 32 (JLS3 15.19), so we needn't mask -distance
        return (i << distance) | (i >>> -distance);
    }

   
    public static int rotateRight(int i, int distance) {
        // Shift distances are mod 32 (JLS3 15.19), so we needn't mask -distance
        return (i >>> distance) | (i << -distance);
    }

    
    public static int reverseBytes(int i) {
        // Hacker's Delight 7-1, with minor tweak from Veldmeijer
        // http://graphics.stanford.edu/~seander/bithacks.html
        i =    ((i >>>  8) & 0x00FF00FF) | ((i & 0x00FF00FF) <<  8);
        return ( i >>> 16              ) | ( i               << 16);
    }

    
    public static int reverse(int i) {
        // Hacker's Delight 7-1, with minor tweak from Veldmeijer
        // http://graphics.stanford.edu/~seander/bithacks.html
        i =    ((i >>>  1) & 0x55555555) | ((i & 0x55555555) <<  1);
        i =    ((i >>>  2) & 0x33333333) | ((i & 0x33333333) <<  2);
        i =    ((i >>>  4) & 0x0F0F0F0F) | ((i & 0x0F0F0F0F) <<  4);
        i =    ((i >>>  8) & 0x00FF00FF) | ((i & 0x00FF00FF) <<  8);
        return ((i >>> 16)             ) | ((i             ) << 16);
    }

    
    public static int signum(int i) {
        return (i >> 31) | (-i >>> 31); // Hacker's delight 2-7
    }

	private static final Integer[] SMALL_VALUES = new Integer[256];
    
    public static Integer valueOf(int i) {
        return  i >= 128 || i < -128 ? new Integer(i) : SMALL_VALUES[i + 128];
    }

    static {
        for (int i = -128; i < 128; i++) {
            SMALL_VALUES[i + 128] = new Integer(i);
        }
    }
}
