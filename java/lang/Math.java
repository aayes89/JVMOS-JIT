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

import java.util.Random;

public final class Math { 

    // Constante de Euler
    public static final double E = 2.71828182845904523536028747135266249775724709369995;
    // Constante PI
    public static final double PI = 3.14159265358979323846264338327950288419716939937510;

	// Más constantes
    private static final double HALF_PI = PI * 0.5d;
    private static final double QUARTER_PI = PI * 0.25d;
    private static final double TWO_PI = PI * 2.0d;

    private static final double LN2 = 0.693147180559945309417232121458176568;
    private static final double LN10 = 2.302585092994045684017991454684364208;
    private static final double INV_LN10 = 0.434294481903251827651128918916605082;
    private static final double SQRT2 = 1.414213562373095048801688724209698079;
    private static final double EPSILON = 1.0E-15d;


    // Clase interna para Random
    private static class NoImagePreloadHolder {
        private static final Random INSTANCE = new Random();
    }
	
	// Constructor 
    private Math() {
    }

	// Funciones básicas matemáticas
	// double absoluto
    public static double abs(double d) {
        if (d == 0.0d) {
            return 0.0d;
        }
        return d < 0.0d ? -d : d;
    }

	// float absoluto
	public static float abs(float f) {
        if (f == 0.0f) {
            return 0.0f;
        }
        return f < 0.0f ? -f : f;
    }

	// valor absoluto entero
    public static int abs(int i) {
        // Integer.MIN_VALUE no puede representarse como entero positivo.
        if (i == Integer.MIN_VALUE) {
            return Integer.MIN_VALUE;
        }
        return i < 0 ? -i : i;
    }
	
    public static long abs(long l) {
        //  Long.MIN_VALUE no puede representarse como long positivo.
        if (l == Long.MIN_VALUE) {
            return Long.MIN_VALUE;
        }
        return l < 0L ? -l : l;
    }

    public static double max(double d1, double d2) { 
		if (d1 != d1) return d1;
        if (d2 != d2) return d2;
		return (d1 >= d2) ? d1 : d2; 
	}
	
    // Funciones MAX
	// double
    public static double max(double d1, double d2) {
        if (d1 != d1) return d1;
        if (d2 != d2) return d2;
        return d1 >= d2 ? d1 : d2;
    }

	// float
    public static float max(float f1, float f2) {
        if (f1 != f1) return f1;
        if (f2 != f2) return f2;
        return f1 >= f2 ? f1 : f2;
    }

	// int
    public static int max(int i1, int i2) {
        return i1 >= i2 ? i1 : i2;
    }

	// long
    public static long max(long l1, long l2) {
        return l1 >= l2 ? l1 : l2;
    }

    // Funciones MIN
	// double
    public static double min(double d1, double d2) {
        if (d1 != d1) return d1;
        if (d2 != d2) return d2;
        return d1 <= d2 ? d1 : d2;
    }

	// float
    public static float min(float f1, float f2) {
        if (f1 != f1) return f1;
        if (f2 != f2) return f2;
        return f1 <= f2 ? f1 : f2;
    }
	
	// int
    public static int min(int i1, int i2) {
        return i1 <= i2 ? i1 : i2;
    }
	
	// long
    public static long min(long l1, long l2) {
        return l1 <= l2 ? l1 : l2;
    }

    // Funciones Trigonométricas con enteros

    // Resultado escalado por 256. sinInt(90) = aproximadamente 256
    public static int sinInt(int degrees) {
        degrees = degrees % 360;
        if (degrees < 0) {
            degrees += 360;
        }
        boolean negative = false;
        if (degrees >= 180) {
            negative = true;
            degrees -= 180;
        }
        int x = degrees * (180 - degrees);
        int num = 4 * x;
        int den = 40500 - x;
        int result = (num * 256) / den;
        return negative ? -result : result;
    }
	
	// cosInt
    public static int cosInt(int degrees) {
        return sinInt(degrees + 90);
    }

    // STUBS FLOTANTES - TODO
    public static double acos(double d) { return 0.0; }
    public static double asin(double d) { return 0.0; }
    public static double atan(double d) { return 0.0; }
    public static double atan2(double y, double x) { return 0.0; }
    public static double cbrt(double d) { return 0.0; }
    public static double ceil(double d) { return (double)((long)d); }
    public static double cos(double d) { return 0.0; }
    public static double cosh(double d) { return 0.0; }
    public static double exp(double d) { return 0.0; }
    public static double expm1(double d) { return 0.0; }
    public static double floor(double d) { return (double)((long)d); }
    public static double hypot(double x, double y) { return 0.0; }
    public static double IEEEremainder(double x, double y) { return 0.0; }
    public static double log(double d) { return 0.0; }
    public static double log10(double d) { return 0.0; }
    public static double log1p(double d) { return 0.0; }
    public static double pow(double x, double y) { return 0.0; }
    public static double rint(double d) { return (double)((long)(d + 0.5)); }
    public static double sin(double d) { return 0.0; }
    public static double sinh(double d) { return 0.0; }
    public static double sqrt(double d) { return 0.0; }
    public static double tan(double d) { return 0.0; }
    public static double tanh(double d) { return 0.0; }

    public static long round(double d) { return (long) floor(d + 0.5d); }
    public static int round(float f) { return (int) floor(f + 0.5f); }

    public static double signum(double d) { return (d > 0) ? 1.0 : (d < 0 ? -1.0 : 0.0); }
    public static float signum(float f) { return (f > 0) ? 1.0f : (f < 0 ? -1.0f : 0.0f); }

    public static double random() {
        return NoImagePreloadHolder.INSTANCE.nextDouble();
    }

    public static void setRandomSeedInternal(long seed) {
        NoImagePreloadHolder.INSTANCE.setSeed(seed);
    }

    public static int randomIntInternal() {
        return NoImagePreloadHolder.INSTANCE.nextInt();
    }

    public static double toRadians(double angdeg) { return angdeg / 180d * PI; }
    public static double toDegrees(double angrad) { return angrad * 180d / PI; }
}
