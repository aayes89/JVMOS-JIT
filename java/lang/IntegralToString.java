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

public final class IntegralToString {

    // TODO - Evitamos ThreadLocal. Usamos un buffer compartido con sincronización implícita.
    private static final char[] SHARED_BUFFER = new char[65];

    private static final String[] SMALL_NONNEGATIVE_VALUES = new String[100];
    private static final String[] SMALL_NEGATIVE_VALUES = new String[100];

    private static final char[] TENS = {
        '0', '0', '0', '0', '0', '0', '0', '0', '0', '0',
        '1', '1', '1', '1', '1', '1', '1', '1', '1', '1',
        '2', '2', '2', '2', '2', '2', '2', '2', '2', '2',
        '3', '3', '3', '3', '3', '3', '3', '3', '3', '3',
        '4', '4', '4', '4', '4', '4', '4', '4', '4', '4',
        '5', '5', '5', '5', '5', '5', '5', '5', '5', '5',
        '6', '6', '6', '6', '6', '6', '6', '6', '6', '6',
        '7', '7', '7', '7', '7', '7', '7', '7', '7', '7',
        '8', '8', '8', '8', '8', '8', '8', '8', '8', '8',
        '9', '9', '9', '9', '9', '9', '9', '9', '9', '9'
    };

    private static final char[] ONES = {
        '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
        '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
        '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
        '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
        '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
        '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
        '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
        '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
        '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
        '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
    };

    private static final char[] MOD_10_TABLE = {
        0, 1, 2, 2, 3, 3, 4, 5, 5, 6, 7, 7, 8, 8, 9, 0
    };

    private static final char[] DIGITS = {
        '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
        'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j',
        'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't',
        'u', 'v', 'w', 'x', 'y', 'z'
    };

    private static final char[] UPPER_CASE_DIGITS = {
        '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
        'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J',
        'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T',
        'U', 'V', 'W', 'X', 'Y', 'Z'
    };

    private IntegralToString() {
    }

    public static String intToString(int i, int radix) {
        if (radix < 2 || radix > 36) radix = 10;
        if (radix == 10) return intToString(i);

        boolean negative = false;
        if (i < 0) {
            negative = true;
        } else {
            i = -i;
        }

        int bufLen = radix < 8 ? 33 : 12;
        char[] buf = new char[bufLen];
        int cursor = bufLen;

        do {
            int q = i / radix;
            buf[--cursor] = DIGITS[radix * q - i];
            i = q;
        } while (i != 0);

        if (negative) {
            buf[--cursor] = '-';
        }

        byte[] resultBytes = new byte[bufLen - cursor];
        for(int j=0; j<(bufLen - cursor); j++) resultBytes[j] = (byte)buf[cursor + j];
        return new String(resultBytes);
    }

    public static String intToString(int i) {
        return convertInt(null, i);
    }

    public static void appendInt(StringBuilder sb, int i) {
        convertInt(sb, i);
    }

    private static String convertInt(StringBuilder sb, int i) {
        boolean negative = false;
        String quickResult = null;
        if (i < 0) {
            negative = true;
            i = -i;
            if (i < 100) {
                if (i < 0) quickResult = "-2147483648";
                else {
                    quickResult = SMALL_NEGATIVE_VALUES[i];
                    if (quickResult == null) {
                        SMALL_NEGATIVE_VALUES[i] = quickResult =
                                i < 10 ? stringOf('-', ONES[i]) : stringOf('-', TENS[i], ONES[i]);
                    }
                }
            }
        } else {
            if (i < 100) {
                quickResult = SMALL_NONNEGATIVE_VALUES[i];
                if (quickResult == null) {
                    SMALL_NONNEGATIVE_VALUES[i] = quickResult =
                            i < 10 ? stringOf(ONES[i]) : stringOf(TENS[i], ONES[i]);
                }
            }
        }
        if (quickResult != null) {
            if (sb != null) {
                sb.append(quickResult);
                return null;
            }
            return quickResult;
        }

        int bufLen = 11;
        char[] buf = (sb != null) ? SHARED_BUFFER : new char[bufLen];
        int cursor = bufLen;

        while (i >= (1 << 16)) {
            int q = (int) ((0x51EB851FL * i) >>> 37);
            int r = i - 100*q;
            buf[--cursor] = ONES[r];
            buf[--cursor] = TENS[r];
            i = q;
        }

        while (i != 0) {
            int q = (0xCCCD * i) >>> 19;
            int r = i - 10*q;
            buf[--cursor] = DIGITS[r];
            i = q;
        }

        if (negative) {
            buf[--cursor] = '-';
        }

        if (sb != null) {
            byte[] temp = new byte[bufLen - cursor];
            for(int j=0; j<(bufLen - cursor); j++) temp[j] = (byte)buf[cursor + j];
            sb.append(new String(temp));
            return null;
        } else {
            byte[] resultBytes = new byte[bufLen - cursor];
            for(int j=0; j<(bufLen - cursor); j++) resultBytes[j] = (byte)buf[cursor + j];
            return new String(resultBytes);
        }
    }

    public static String longToString(long v, int radix) {
        int i = (int) v;
        if (i == v) return intToString(i, radix);

        if (radix < 2 || radix > 36) radix = 10;
        if (radix == 10) return longToString(v);

        boolean negative = false;
        if (v < 0) {
            negative = true;
        } else {
            v = -v;
        }

        int bufLen = radix < 8 ? 65 : 23;
        char[] buf = new char[bufLen];
        int cursor = bufLen;

        do {
            long q = v / radix;
            buf[--cursor] = DIGITS[(int) (radix * q - v)];
            v = q;
        } while (v != 0);

        if (negative) {
            buf[--cursor] = '-';
        }

        byte[] resultBytes = new byte[bufLen - cursor];
        for(int j=0; j<(bufLen - cursor); j++) resultBytes[j] = (byte)buf[cursor + j];
        return new String(resultBytes);
    }

    public static String longToString(long l) {
        return convertLong(null, l);
    }

    public static void appendLong(StringBuilder sb, long l) {
        convertLong(sb, l);
    }

    private static String convertLong(StringBuilder sb, long n) {
        int i = (int) n;
        if (i == n) return convertInt(sb, i);

        boolean negative = (n < 0);
        if (negative) {
            n = -n;
            if (n < 0) {
                String quickResult = "-9223372036854775808";
                if (sb != null) {
                    sb.append(quickResult);
                    return null;
                }
                return quickResult;
            }
        }

        int bufLen = 20;
        char[] buf = (sb != null) ? SHARED_BUFFER : new char[bufLen];

        int low = (int) (n % 1000000000);
        int cursor = intIntoCharArray(buf, bufLen, low);

        while (cursor != (bufLen - 9)) {
            buf[--cursor] = '0';
        }

        n = ((n - low) >>> 9) * 0x8E47CE423A2E9C6DL;

        if ((n & (-1L << 32)) == 0) {
            cursor = intIntoCharArray(buf, cursor, (int) n);
        } else {
            int lo32 = (int) n;
            int hi32 = (int) (n >>> 32);

            int midDigit = MOD_10_TABLE[(0x19999999 * lo32 + (lo32 >>> 1) + (lo32 >>> 3)) >>> 28];
            midDigit -= hi32 << 2; 
            if (midDigit < 0) midDigit += 10;
            buf[--cursor] = DIGITS[midDigit];

            int rest = ((int) ((n - midDigit) >>> 1)) * 0xCCCCCCCD;
            cursor = intIntoCharArray(buf, cursor, rest);
        }

        if (negative) {
            buf[--cursor] = '-';
        }
        
        if (sb != null) {
            byte[] temp = new byte[bufLen - cursor];
            for(int j=0; j<(bufLen - cursor); j++) temp[j] = (byte)buf[cursor + j];
            sb.append(new String(temp));
            return null;
        } else {
            byte[] resultBytes = new byte[bufLen - cursor];
            for(int j=0; j<(bufLen - cursor); j++) resultBytes[j] = (byte)buf[cursor + j];
            return new String(resultBytes);
        }
    }

    private static int intIntoCharArray(char[] buf, int cursor, int n) {
        while ((n & 0xffff0000) != 0) {
            int q = (int) ((0x51EB851FL * (n >>> 2)) >>> 35);
            int r = n - 100*q;
            buf[--cursor] = ONES[r];
            buf[--cursor] = TENS[r];
            n = q;
        }
        while (n != 0) {
            int q = (0xCCCD * n) >>> 19;
            int r = n - 10*q;
            buf[--cursor] = DIGITS[r];
            n = q;
        }
        return cursor;
    }

    public static String intToBinaryString(int i) {
        int bufLen = 32; 
        char[] buf = new char[bufLen];
        int cursor = bufLen;
        do { buf[--cursor] = DIGITS[i & 1]; }  while ((i >>>= 1) != 0);
        
        byte[] resultBytes = new byte[bufLen - cursor];
        for(int j=0; j<(bufLen - cursor); j++) resultBytes[j] = (byte)buf[cursor + j];
        return new String(resultBytes);
    }

    public static String longToBinaryString(long v) {
        int i = (int) v;
        if (v >= 0 && i == v) return intToBinaryString(i);

        int bufLen = 64; 
        char[] buf = new char[bufLen];
        int cursor = bufLen;
        do { buf[--cursor] = DIGITS[((int) v) & 1]; }  while ((v >>>= 1) != 0);

        byte[] resultBytes = new byte[bufLen - cursor];
        for(int j=0; j<(bufLen - cursor); j++) resultBytes[j] = (byte)buf[cursor + j];
        return new String(resultBytes);
    }

    public static StringBuilder appendByteAsHex(StringBuilder sb, byte b, boolean upperCase) {
        char[] digits = upperCase ? UPPER_CASE_DIGITS : DIGITS;
        byte[] tmp = new byte[1];
        tmp[0] = (byte)digits[(b >> 4) & 0xf]; sb.append(new String(tmp));
        tmp[0] = (byte)digits[b & 0xf]; sb.append(new String(tmp));
        return sb;
    }

    public static String byteToHexString(byte b, boolean upperCase) {
        char[] digits = upperCase ? UPPER_CASE_DIGITS : DIGITS;
        byte[] buf = new byte[2];
        buf[0] = (byte)digits[(b >> 4) & 0xf];
        buf[1] = (byte)digits[b & 0xf];
        return new String(buf);
    }

    public static String bytesToHexString(byte[] bytes, boolean upperCase) {
        char[] digits = upperCase ? UPPER_CASE_DIGITS : DIGITS;
        byte[] buf = new byte[bytes.length * 2];
        int c = 0;
        for (byte b : bytes) {
            buf[c++] = (byte)digits[(b >> 4) & 0xf];
            buf[c++] = (byte)digits[b & 0xf];
        }
        return new String(buf);
    }

    public static String intToHexString(int i, boolean upperCase, int minWidth) {
        int bufLen = 8;
        char[] buf = new char[bufLen];
        int cursor = bufLen;
        char[] digits = upperCase ? UPPER_CASE_DIGITS : DIGITS;
        do { buf[--cursor] = digits[i & 0xf]; } while ((i >>>= 4) != 0 || (bufLen - cursor < minWidth));
        
        byte[] resultBytes = new byte[bufLen - cursor];
        for(int j=0; j<(bufLen - cursor); j++) resultBytes[j] = (byte)buf[cursor + j];
        return new String(resultBytes);
    }

    public static String longToHexString(long v) {
        int i = (int) v;
        if (v >= 0 && i == v) return intToHexString(i, false, 0);

        int bufLen = 16;
        char[] buf = new char[bufLen];
        int cursor = bufLen;
        do { buf[--cursor] = DIGITS[((int) v) & 0xF]; } while ((v >>>= 4) != 0);

        byte[] resultBytes = new byte[bufLen - cursor];
        for(int j=0; j<(bufLen - cursor); j++) resultBytes[j] = (byte)buf[cursor + j];
        return new String(resultBytes);
    }

    public static String intToOctalString(int i) {
        int bufLen = 11;
        char[] buf = new char[bufLen];
        int cursor = bufLen;
        do { buf[--cursor] = DIGITS[i & 7]; } while ((i >>>= 3) != 0);

        byte[] resultBytes = new byte[bufLen - cursor];
        for(int j=0; j<(bufLen - cursor); j++) resultBytes[j] = (byte)buf[cursor + j];
        return new String(resultBytes);
    }

    public static String longToOctalString(long v) {
        int i = (int) v;
        if (v >= 0 && i == v) return intToOctalString(i);
        int bufLen = 22;
        char[] buf = new char[bufLen];
        int cursor = bufLen;
        do { buf[--cursor] = DIGITS[((int) v) & 7]; } while ((v >>>= 3) != 0);

        byte[] resultBytes = new byte[bufLen - cursor];
        for(int j=0; j<(bufLen - cursor); j++) resultBytes[j] = (byte)buf[cursor + j];
        return new String(resultBytes);
    }

    private static String stringOf(char... args) {
        byte[] resultBytes = new byte[args.length];
        for(int j=0; j<args.length; j++) resultBytes[j] = (byte)args[j];
        return new String(resultBytes);
    }
}
