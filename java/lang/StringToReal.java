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

public final class StringToReal {

    private static final class StringExponentPair {
        String s;
        long e;
        boolean negative;

        // Banderas para casos de error espeaciales
        boolean infinity;
        boolean zero;

        public float specialValue() {
            if (infinity) {
                return negative ? Float.NEGATIVE_INFINITY : Float.POSITIVE_INFINITY;
            }
            return negative ? -0.0f : 0.0f;
        }
    }
   
    private static double parseDblImpl(String s, int e){
		// TODO
	}

    private static float parseFltImpl(String s, int e){
		// TODO
	}

    private static NumberFormatException invalidReal(String s, boolean isDouble) {
        throw new NumberFormatException("Invalid " + (isDouble ? "double" : "float") + ": \"" + s + "\"");
    }
    
    private static StringExponentPair initialParse(String s, int length, boolean isDouble) {
        StringExponentPair result = new StringExponentPair();
        if (length == 0) {
            throw invalidReal(s, isDouble);
        }
        result.negative = (s.charAt(0) == '-');

        // ignorar el acarreo de double o float
        char c = s.charAt(length - 1);
        if (c == 'D' || c == 'd' || c == 'F' || c == 'f') {
            length--;
            if (length == 0) {
                throw invalidReal(s, isDouble);
            }
        }

        int end = Math.max(s.indexOf('E'), s.indexOf('e'));
        if (end != -1) {
            // hay algo despues de 'e'?
            if (end + 1 == length) {
                throw invalidReal(s, isDouble);
            }

            // hay algun signo explícito opcional?
            int exponentOffset = end + 1;
            boolean negativeExponent = false;
            char firstExponentChar = s.charAt(exponentOffset);
            if (firstExponentChar == '+' || firstExponentChar == '-') {
                negativeExponent = (firstExponentChar == '-');
                ++exponentOffset;
            }

            // hay enteros positivos?
            String exponentString = s.substring(exponentOffset, length);
            if (exponentString.isEmpty()) {
                throw invalidReal(s, isDouble);
            }
            for (int i = 0; i < exponentString.length(); ++i) {
                char ch = exponentString.charAt(i);
                if (ch < '0' || ch > '9') {
                    throw invalidReal(s, isDouble);
                }
            }

            // parseo del exponente entero
            try {
                result.e = Integer.parseInt(exponentString);
                if (negativeExponent) {
                    result.e = -result.e;
                }
            } catch (NumberFormatException ex) {                
                // int.
                if (negativeExponent) {
                    result.zero = true;
                } else {
                    result.infinity = true;
                }
                
            }
        } else {
            end = length;
        }

        int start = 0;
        c = s.charAt(start);
        if (c == '-') {
            ++start;
            --length;
            result.negative = true;
        } else if (c == '+') {
            ++start;
            --length;
        }
        if (length == 0) {
            throw invalidReal(s, isDouble);
        }

        // cofirmar que la mantisa fue parseada
        int decimal = -1;
        for (int i = start; i < end; i++) {
            char mc = s.charAt(i);
            if (mc == '.') {
                if (decimal != -1) {
                    throw invalidReal(s, isDouble);
                }
                decimal = i;
            } else if (mc < '0' || mc > '9') {
                throw invalidReal(s, isDouble);
            }
        }
        if (decimal > -1) {
            result.e -= end - decimal - 1;
            s = s.substring(start, decimal) + s.substring(decimal + 1, end);
        } else {
            s = s.substring(start, end);
        }

        length = s.length();
        if (length == 0) {
            throw invalidReal(s, isDouble);
        }

        // Todos los chequeos siguientes devuelven excepcion
        if (result.infinity || result.zero) {
            return result;
        }

        end = length;
        while (end > 1 && s.charAt(end - 1) == '0') {
            --end;
        }

        start = 0;
        while (start < end - 1 && s.charAt(start) == '0') {
            start++;
        }

        if (end != length || start != 0) {
            result.e += length - end;
            s = s.substring(start, end);
        }

        // Hack obtenido de https://issues.apache.org/jira/browse/HARMONY-329
        // Recorta la longitud de los números muy pequeños; los tipos nativos solo admiten hasta E-309.
        final int APPROX_MIN_MAGNITUDE = -359;
        final int MAX_DIGITS = 52;
        length = s.length();
        if (length > MAX_DIGITS && result.e < APPROX_MIN_MAGNITUDE) {
            int d = Math.min(APPROX_MIN_MAGNITUDE - (int) result.e, length - 1);
            s = s.substring(0, length - d);
            result.e += d;
        }

        // hack obtenido de https://issues.apache.org/jira/browse/HARMONY-6641
        // El número mágico 1024 determinado experimentalmente:
		// Valores objetivos: -324 y +309, no funcionaron en pruebas.
        if (result.e < -1024) {
            result.zero = true;
            return result;
        } else if (result.e > 1024) {
            result.infinity = true;
            return result;
        }

        result.s = s;
        return result;
    }

    // Parsea "+Nan", "NaN", "-Nan", "+Infinity", "Infinity", y "-Infinity", case-insensitively.
    private static float parseName(String name, boolean isDouble) {
        // Signo explícito?
        boolean negative = false;
        int i = 0;
        int length = name.length();
        char firstChar = name.charAt(i);
        if (firstChar == '-') {
            negative = true;
            ++i;
            --length;
        } else if (firstChar == '+') {
            ++i;
            --length;
        }

        if (length == 8 && name.regionMatches(false, i, "Infinity", 0, 8)) {
            return negative ? Float.NEGATIVE_INFINITY : Float.POSITIVE_INFINITY;
        }
        if (length == 3 && name.regionMatches(false, i, "NaN", 0, 3)) {
            return Float.NaN;
        }
        throw invalidReal(name, isDouble);
    }

    public static double parseDouble(String s) {
        s = s.trim();
        int length = s.length();

        if (length == 0) {
            throw invalidReal(s, true);
        }

        // es double?
        char last = s.charAt(length - 1);
        if (last == 'y' || last == 'N') {
            return parseName(s, true);
        }

        // Es hexadecimal?        
        if (s.indexOf("0x") != -1 || s.indexOf("0X") != -1) {
            return HexStringParser.parseDouble(s);
        }

        StringExponentPair info = initialParse(s, length, true);
        if (info.infinity || info.zero) {
            return info.specialValue();
        }
        double result = parseDblImpl(info.s, (int) info.e);
        if (Double.doubleToRawLongBits(result) == 0xffffffffffffffffL) {
            throw invalidReal(s, true);
        }
        return info.negative ? -result : result;
    }
    
    public static float parseFloat(String s) {
        s = s.trim();
        int length = s.length();

        if (length == 0) {
            throw invalidReal(s, false);
        }

        // Es Float?
        char last = s.charAt(length - 1);
        if (last == 'y' || last == 'N') {
            return parseName(s, false);
        }

        // Se puede representar como hexadecimal?
        if (s.indexOf("0x") != -1 || s.indexOf("0X") != -1) {
            return HexStringParser.parseFloat(s);
        }

        StringExponentPair info = initialParse(s, length, false);
        if (info.infinity || info.zero) {
            return info.specialValue();
        }
        float result = parseFltImpl(info.s, (int) info.e);
        if (Float.floatToRawIntBits(result) == 0xffffffff) {
            throw invalidReal(s, false);
        }
        return info.negative ? -result : result;
    }
}
