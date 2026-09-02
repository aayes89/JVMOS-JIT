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

package java.math;

import java.io.Serializable;

public class BigInteger extends Number implements Comparable<BigInteger>, Serializable {

    private static final long serialVersionUID = -8287574255936472291L;

    final int signum;
    final int[] mag; // Almacenado en formato Big-Endian (mag[0] es el más significativo)

    public static final BigInteger ZERO = new BigInteger(new int[0], 0);
    public static final BigInteger ONE = valueOf(1);
    public static final BigInteger TEN = valueOf(10);

    // Constructor interno
    private BigInteger(int[] magnitude, int signum) {
        this.signum = (magnitude.length == 0) ? 0 : signum;
        this.mag = magnitude;
    }

    public static BigInteger valueOf(long val) {
        if (val == 0) return ZERO;
        if (val > 0) {
            return new BigInteger(new int[] { (int) val }, 1);
        }
        return new BigInteger(new int[] { (int) -val }, -1);
    }

    // Suma pura en Java manipulando el arreglo
    public BigInteger add(BigInteger val) {
        if (val.signum == 0) return this;
        if (signum == 0) return val;
        if (val.signum == signum) {
            return new BigInteger(add(mag, val.mag), signum);
        }
        int cmp = compareMagnitude(val);
        if (cmp == 0) return ZERO;
        int[] resultMag = (cmp > 0 ? subtract(mag, val.mag) : subtract(val.mag, mag));
        resultMag = trustedStripLeadingZeroInts(resultMag);
        return new BigInteger(resultMag, cmp == 1 ? signum : val.signum);
    }

    // Multiplicación pura en Java (Algoritmo de la escuela primaria)
    public BigInteger multiply(BigInteger val) {
        if (val.signum == 0 || signum == 0) return ZERO;
        int xlen = mag.length;
        int ylen = val.mag.length;
        int[] z = new int[xlen + ylen];
        long carry = 0;
        for (int j = ylen - 1, k = ylen + xlen - 1; j >= 0; j--, k--) {
            long product = (val.mag[j] & 0xFFFFFFFFL) * (mag[xlen - 1] & 0xFFFFFFFFL) + carry;
            z[k] = (int) product;
            carry = product >>> 32;
        }
        z[xlen - 1] = (int) carry;
        for (int i = xlen - 2; i >= 0; i--) {
            carry = 0;
            for (int j = ylen - 1, k = ylen + i; j >= 0; j--, k--) {
                long product = (val.mag[j] & 0xFFFFFFFFL) * (mag[i] & 0xFFFFFFFFL) + (z[k] & 0xFFFFFFFFL) + carry;
                z[k] = (int) product;
                carry = product >>> 32;
            }
            z[i] = (int) carry;
        }
        return new BigInteger(trustedStripLeadingZeroInts(z), signum == val.signum ? 1 : -1);
    }

    // Operaciones sobre el arreglo subyacente
    private static int[] add(int[] x, int[] y) {
        if (x.length < y.length) {
            int[] tmp = x; x = y; y = tmp;
        }
        int xIndex = x.length;
        int yIndex = y.length;
        int result[] = new int[xIndex];
        long sum = 0;
        if (yIndex == 1) {
            sum = (x[--xIndex] & 0xFFFFFFFFL) + (y[0] & 0xFFFFFFFFL);
            result[xIndex] = (int) sum;
        } else {
            while (yIndex > 0) {
                sum = (x[--xIndex] & 0xFFFFFFFFL) + (y[--yIndex] & 0xFFFFFFFFL) + (sum >>> 32);
                result[xIndex] = (int) sum;
            }
        }
        boolean carry = (sum >>> 32 != 0);
        while (xIndex > 0 && carry) {
            carry = ((result[--xIndex] = x[xIndex] + 1) == 0);
        }
        while (xIndex > 0) {
            result[--xIndex] = x[xIndex];
        }
        if (carry) {
            int bigger[] = new int[result.length + 1];
            System.arraycopy(result, 0, bigger, 1, result.length);
            bigger[0] = 0x01;
            return bigger;
        }
        return result;
    }

    private static int[] subtract(int[] big, int[] little) {
        int bigIndex = big.length;
        int result[] = new int[bigIndex];
        int littleIndex = little.length;
        long difference = 0;
        while (littleIndex > 0) {
            difference = (big[--bigIndex] & 0xFFFFFFFFL) - (little[--littleIndex] & 0xFFFFFFFFL) + (difference >> 32);
            result[bigIndex] = (int) difference;
        }
        boolean borrow = (difference >> 32 != 0);
        while (bigIndex > 0 && borrow) {
            borrow = ((result[--bigIndex] = big[bigIndex] - 1) == -1);
        }
        while (bigIndex > 0) {
            result[--bigIndex] = big[bigIndex];
        }
        return result;
    }

    final int compareMagnitude(BigInteger val) {
        int[] m1 = mag;
        int[] m2 = val.mag;
        if (m1.length < m2.length) return -1;
        if (m1.length > m2.length) return 1;
        for (int i = 0; i < m1.length; i++) {
            int a = m1[i];
            int b = m2[i];
            if (a != b) {
                return ((a & 0xFFFFFFFFL) < (b & 0xFFFFFFFFL)) ? -1 : 1;
            }
        }
        return 0;
    }

    private static int[] trustedStripLeadingZeroInts(int val[]) {
        int vlen = val.length;
        int keep;
        for (keep = 0; keep < vlen && val[keep] == 0; keep++);
        int result[] = new int[vlen - keep];
        System.arraycopy(val, keep, result, 0, vlen - keep);
        return result;
    }

    @Override
    public int compareTo(BigInteger val) {
        if (signum == val.signum) {
            switch (signum) {
                case 1: return compareMagnitude(val);
                case -1: return val.compareMagnitude(this);
                default: return 0;
            }
        }
        return signum > val.signum ? 1 : -1;
    }

    @Override
    public boolean equals(Object x) {
        if (x == this) return true;
        if (!(x instanceof BigInteger)) return false;
        BigInteger xInt = (BigInteger) x;
        if (xInt.signum != signum || xInt.mag.length != mag.length) return false;
        for (int i = 0; i < mag.length; i++) {
            if (xInt.mag[i] != mag[i]) return false;
        }
        return true;
    }

    @Override
    public String toString() {
        if (signum == 0) return "0";
        // Conversión simple para números de un solo bloque (32 bits)
        if (mag.length == 1) {
            long v = mag[0] & 0xFFFFFFFFL;
            return (signum < 0 ? "-" : "") + v;
        }
        // Para números más grandes, devolvemos un hash hexadecimal para no saturar el toString sin el algoritmo completo de división
        StringBuilder sb = new StringBuilder(signum < 0 ? "-" : "");
        for (int i = 0; i < mag.length; i++) {
            sb.append(Integer.toHexString(mag[i]));
        }
        return sb.toString();
    }

    // TODO - para operaciones complejas restantes
    public BigInteger divide(BigInteger val) { return ZERO; }
    public BigInteger remainder(BigInteger val) { return ZERO; }
    public BigInteger[] divideAndRemainder(BigInteger val) { return new BigInteger[]{ZERO, ZERO}; }
    public BigInteger pow(int exponent) { return ZERO; }
    public BigInteger abs() { return (signum >= 0 ? this : new BigInteger(mag, 1)); }
    public BigInteger negate() { return new BigInteger(this.mag, -this.signum); }
    public int signum() { return this.signum; }
    
    @Override public int intValue() { return (mag.length >= 1 ? mag[mag.length - 1] : 0) * signum; }
    @Override public long longValue() { return intValue(); } // Simplificado
    @Override public float floatValue() { return (float) longValue(); }
    @Override public double doubleValue() { return (double) longValue(); }
}
