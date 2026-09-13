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

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.Splitter;


final class HexStringParser {
	
	// declaraciones estáticas útiles y regexs

    private static final int DOUBLE_EXPONENT_WIDTH = 11;

    private static final int DOUBLE_MANTISSA_WIDTH = 52;

    private static final int FLOAT_EXPONENT_WIDTH = 8;

    private static final int FLOAT_MANTISSA_WIDTH = 23;

    private static final int HEX_RADIX = 16;

    private static final int MAX_SIGNIFICANT_LENGTH = 15;

    private static final String HEX_SIGNIFICANT = "0[xX](\\p{XDigit}+\\.?|\\p{XDigit}*\\.\\p{XDigit}+)";

    private static final String BINARY_EXPONENT = "[pP]([+-]?\\d+)";

    private static final String FLOAT_TYPE_SUFFIX = "[fFdD]?";

    private static final String HEX_PATTERN = "[\\x00-\\x20]*([+-]?)" + HEX_SIGNIFICANT
            + BINARY_EXPONENT + FLOAT_TYPE_SUFFIX + "[\\x00-\\x20]*";

    private static final Pattern PATTERN = Pattern.compile(HEX_PATTERN);

    private final int EXPONENT_WIDTH;

    private final int MANTISSA_WIDTH;

    private final long EXPONENT_BASE;

    private final long MAX_EXPONENT;

    private final long MIN_EXPONENT;

    private final long MANTISSA_MASK;

    private long sign;

    private long exponent;

    private long mantissa;

    private String abandonedNumber="";

    private Splitter splitter;

	// Constructor
    public HexStringParser(int exponentWidth, int mantissaWidth) {
        this.EXPONENT_WIDTH = exponentWidth;
        this.MANTISSA_WIDTH = mantissaWidth;

        this.EXPONENT_BASE = ~(-1L << (exponentWidth - 1));
        this.MAX_EXPONENT = ~(-1L << exponentWidth);
        this.MIN_EXPONENT = -(MANTISSA_WIDTH + 1);
        this.MANTISSA_MASK = ~(-1L << mantissaWidth);
    }

    // Parsear la cadena de texto hexadecimal a un double
    public static double parseDouble(String hexString) {
        HexStringParser parser = new HexStringParser(DOUBLE_EXPONENT_WIDTH, DOUBLE_MANTISSA_WIDTH);
        long result = parser.parse(hexString, true);
        return Double.longBitsToDouble(result);
    }

    // Parsear la cadena de texto hexadecimal a float
    public static float parseFloat(String hexString) {
        HexStringParser parser = new HexStringParser(FLOAT_EXPONENT_WIDTH, FLOAT_MANTISSA_WIDTH);
        int result = (int) parser.parse(hexString, false);
        return Float.intBitsToFloat(result);
    }
	
	// función de ayuda para el parseo
    private long parse(String hexString, boolean isDouble) {
        Matcher matcher = PATTERN.matcher(hexString);
        if (!matcher.matches()) {
            throw new NumberFormatException("Invalid hex " + (isDouble ? "double" : "float")+ ":" +
                    hexString);
        }

        String signStr = matcher.group(1);
        String significantStr = matcher.group(2);
        String exponentStr = matcher.group(3);

        parseHexSign(signStr);
        parseExponent(exponentStr);
        parseMantissa(significantStr);

        sign <<= (MANTISSA_WIDTH + EXPONENT_WIDTH);
        exponent <<= MANTISSA_WIDTH;
        return sign | exponent | mantissa;
    }

    // Parsear el campo de signo.
    private void parseHexSign(String signStr) {
        this.sign = signStr.equals("-") ? 1 : 0;
    }

    // Parsear el campo del exponente
    private void parseExponent(String exponentStr) {
        char leadingChar = exponentStr.charAt(0);
        int expSign = (leadingChar == '-' ? -1 : 1);
        if (!Character.isDigit(leadingChar)) {
            exponentStr = exponentStr.substring(1);
        }

        try {
            exponent = expSign * Long.parseLong(exponentStr);
            checkedAddExponent(EXPONENT_BASE);
        } catch (NumberFormatException e) {
            exponent = expSign * Long.MAX_VALUE;
        }
    }

    // Parsear la mantisa 
    private void parseMantissa(String significantStr) {
        Splitter splitter = new Splitter();
        String[] strings = splitter.split(PATTERN, "\\.", significantStr, significantStr.length());
        String strIntegerPart = strings[0];
        String strDecimalPart = strings.length > 1 ? strings[1] : "";

        String significand = getNormalizedSignificand(strIntegerPart,strDecimalPart);
        if (significand.equals("0")) {
            setZero();
            return;
        }

        int offset = getOffset(strIntegerPart, strDecimalPart);
        checkedAddExponent(offset);

        if (exponent >= MAX_EXPONENT) {
            setInfinite();
            return;
        }

        if (exponent <= MIN_EXPONENT) {
            setZero();
            return;
        }

        if (significand.length() > MAX_SIGNIFICANT_LENGTH) {
            abandonedNumber = significand.substring(MAX_SIGNIFICANT_LENGTH);
            significand = significand.substring(0, MAX_SIGNIFICANT_LENGTH);
        }

        mantissa = Long.parseLong(significand, HEX_RADIX);

        if (exponent >= 1) {
            processNormalNumber();
        } else{
            processSubNormalNumber();
        }

    }

    private void setInfinite() {
        exponent = MAX_EXPONENT;
        mantissa = 0;
    }

    private void setZero() {
        exponent = 0;
        mantissa = 0;
    }

    // Establece la variable de exponente en Long.MAX_VALUE o -Long.MAX_VALUE 
	// si se produce desbordamiento (overflow) o subdesbordamiento (underflow).
    private void checkedAddExponent(long offset) {
        long result = exponent + offset;
        int expSign = Long.signum(exponent);
        if (expSign * Long.signum(offset) > 0 && expSign * Long.signum(result) < 0) {
            exponent = expSign * Long.MAX_VALUE;
        } else {
            exponent = result;
        }
    }

    private void processNormalNumber(){
        int desiredWidth = MANTISSA_WIDTH + 2;
        fitMantissaInDesiredWidth(desiredWidth);
        round();
        mantissa = mantissa & MANTISSA_MASK;
    }

    private void processSubNormalNumber(){
        int desiredWidth = MANTISSA_WIDTH + 1;
        desiredWidth += (int)exponent;//lends bit from mantissa to exponent
        exponent = 0;
        fitMantissaInDesiredWidth(desiredWidth);
        round();
        mantissa = mantissa & MANTISSA_MASK;
    }

    // Ajustar el tamaño de la mantisa para analisar
    private void fitMantissaInDesiredWidth(int desiredWidth){
        int bitLength = countBitsLength(mantissa);
        if (bitLength > desiredWidth) {
            discardTrailingBits(bitLength - desiredWidth);
        } else {
            mantissa <<= (desiredWidth - bitLength);
        }
    }

    // Almacena los bits descartados en abandonedNumber.
    private void discardTrailingBits(long num) {
        long mask = ~(-1L << num);
        abandonedNumber += (mantissa & mask);
        mantissa >>= num;
    }

    /* 
	 * El valor se redondea al alza o a la baja hacia el resultado de precisión infinita más cercano. 
	 * Si el valor se encuentra exactamente a medio camino entre dos resultados de precisión infinita,
	 * entonces debe redondearse al alza hacia el resultado de precisión infinita par más cercano.
	 */
    private void round() {
        String result = abandonedNumber.replaceAll("0+", "");
        boolean moreThanZero = (result.length() > 0 ? true : false);

        int lastDiscardedBit = (int) (mantissa & 1L);
        mantissa >>= 1;
        int tailBitInMantissa = (int) (mantissa & 1L);

        if (lastDiscardedBit == 1 && (moreThanZero || tailBitInMantissa == 1)) {
            int oldLength = countBitsLength(mantissa);
            mantissa += 1L;
            int newLength = countBitsLength(mantissa);

            //Rounds up to exponent when whole bits of mantissa are one-bits.
            if (oldLength >= MANTISSA_WIDTH && newLength > oldLength) {
                checkedAddExponent(1);
            }
        }
    }

    // Devuelve el significando normalizado tras eliminar los ceros iniciales.
    private String getNormalizedSignificand(String strIntegerPart, String strDecimalPart) {
        String significand = strIntegerPart + strDecimalPart;
        significand = significand.replaceFirst("^0+", "");
        if (significand.length() == 0) {
            significand = "0";
        }
        return significand;
    }

    /*
     * Calcular el desplazamiento entre el número normalizado y el no normalizado. 
	 * En una representación normalizada, el significando se representa mediante los
	 * caracteres "0x1." seguidos de la representación hexadecimal en minúsculas
	 * del resto del significando como una fracción.
     */
    private int getOffset(String strIntegerPart, String strDecimalPart) {
        strIntegerPart = strIntegerPart.replaceFirst("^0+", "");

        //If the Integer part is a nonzero number.
        if (strIntegerPart.length() != 0) {
            String leadingNumber = strIntegerPart.substring(0, 1);
            return (strIntegerPart.length() - 1) * 4 + countBitsLength(Long.parseLong(leadingNumber,HEX_RADIX)) - 1;
        }

        //If the Integer part is a zero number.
        int i;
        for (i = 0; i < strDecimalPart.length() && strDecimalPart.charAt(i) == '0'; i++);
        if (i == strDecimalPart.length()) {
            return 0;
        }
        String leadingNumber=strDecimalPart.substring(i,i + 1);
        return (-i - 1) * 4 + countBitsLength(Long.parseLong(leadingNumber, HEX_RADIX)) - 1;
    }

    private int countBitsLength(long value) {
        int leadingZeros = Long.numberOfLeadingZeros(value);
        return Long.SIZE - leadingZeros;
    }
}
