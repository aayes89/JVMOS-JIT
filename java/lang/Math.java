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
    private static final Random INSTANCE = new Random();
	
	// Constructor 
    private Math() {
    }

	// Funciones básicas matemáticas extraídas de la clase Math Java.
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

    // Reducción de ángulo (extraido de Math)

    private static double reduceAngle(double x) {
        if (x != x) {
            return Double.NaN;
        }
        if (x > TWO_PI || x < -TWO_PI) {
            long n = (long)(x / TWO_PI);
            x -= n * TWO_PI;
        }
        if (x > PI) {
            x -= TWO_PI;
        }
        if (x < -PI) {
            x += TWO_PI;
        }
        return x;
    }


    // SIN
	// calcular seno para double
    public static double sin(double d) {
        if (d != d) {
            return Double.NaN;
        }
        if (d == 0.0d) {
            return d;
        }
        d = reduceAngle(d);

        // sin(x) es más preciso en el intervalo [-PI/2, PI/2].
        if (d > HALF_PI) {
            d = PI - d;
        } else if (d < -HALF_PI) {
            d = -PI - d;
        }
        double x2 = d * d;
        // Serie de Taylor: sin(x) = x - x^3/3! + x^5/5! - ...
        double term = d;
        double result = d;
        for (int i = 1; i < 16; i++) {
            double a = 2.0d * i;
            double b = a + 1.0d;
            term *= -x2 / (a * b);
            result += term;
            if (abs(term) < EPSILON) {
                break;
            }
        }
        return result;
    }

    // COS
	// calcular coseno en double
    public static double cos(double d) {
        if (d != d) {
            return Double.NaN;
        }
        d = reduceAngle(d);
        // cos(x) es par.
        if (d < 0.0d) {
            d = -d;
        }
        if (d > HALF_PI) {
            d = PI - d;
            return -cos(d);
        }
        double x2 = d * d;
        // cos(x) = 1 - x²/2! + x^4/4! - ...
        double term = 1.0d;
        double result = 1.0d;
        for (int i = 1; i < 16; i++) {
            double a = 2.0d * i - 1.0d;
            double b = 2.0d * i;
            term *= -x2 / (a * b);
            result += term;
            if (abs(term) < EPSILON) {
                break;
            }
        }
        return result;
    }

    // TAN
	// calcular tangente en double
    public static double tan(double d) {
        double c = cos(d);
        if (c == 0.0d) {
            return Double.NaN;
        }
        return sin(d) / c;
    }

    // ATAN
	// calcular arco-tangente en double
    public static double atan(double d) {
        if (d != d) {
            return Double.NaN;
        }
        if (d == Double.POSITIVE_INFINITY) {
            return HALF_PI;
        }
        if (d == Double.NEGATIVE_INFINITY) {
            return -HALF_PI;
        }
        if (d == 0.0d) {
            return d;
        }
        boolean negative = d < 0.0d;
        if (negative) {
            d = -d;
        }
        double result;
        // atan(x) = PI/2 - atan(1/x)
        if (d > 1.0d) {
            result = HALF_PI - atan(1.0d / d);
        } else if (d > 0.5d) {
            // atan(x) = PI/4 + atan((x-1)/(x+1))
            result = QUARTER_PI + atan((d - 1.0d) / (d + 1.0d));
        } else {
            double x2 = d * d;
            double term = d;
            double resultLocal = d;
            // atan(x) = x - x³/3 + x⁵/5 - x⁷/7 ...
            for (int i = 1; i < 80; i++) {
                term *= -x2;
                double add = term / (2.0d * i + 1.0d);
                resultLocal += add;
                if (abs(add) < EPSILON) {
                    break;
                }
            }
            result = resultLocal;
        }
        return negative ? -result : result;
    }


    // ATAN2
	// calcular arco-tangente 2
    public static double atan2(double y, double x) {
        if (x != x || y != y) {
            return Double.NaN;
        }
        if (x > 0.0d) {
            return atan(y / x);
        }
        if (x < 0.0d && y >= 0.0d) {
            return atan(y / x) + PI;
        }
        if (x < 0.0d && y < 0.0d) {
            return atan(y / x) - PI;
        }
        if (x == 0.0d && y > 0.0d) {
            return HALF_PI;
        }
        if (x == 0.0d && y < 0.0d) {
            return -HALF_PI;
        }
        return 0.0d;
    }


    // ASIN
	// calcular arco-seno
    public static double asin(double d) {
        if (d != d) {
            return Double.NaN;
        }
        if (d < -1.0d || d > 1.0d) {
            return Double.NaN;
        }
        if (d == 1.0d) {
            return HALF_PI;
        }
        if (d == -1.0d) {
            return -HALF_PI;
        }
        // asin(x) = atan(x / sqrt(1-x²))
        return atan(d / sqrt(1.0d - d * d));
    }

    // ACOS
	// calcular arco-coseno 
    public static double acos(double d) {
        if (d != d) {
            return Double.NaN;
        }
        if (d < -1.0d || d > 1.0d) {
            return Double.NaN;
        }
        return HALF_PI - asin(d);
    }

    // SQRT
	// calcular raiz cuadrada
    public static double sqrt(double d) {
        if (d != d) {
            return Double.NaN;
        }
        if (d < 0.0d) {
            return Double.NaN;
        }
        if (d == 0.0d) {
            return d;
        }
        if (d == Double.POSITIVE_INFINITY) {
            return d;
        }
        // Aproximación inicial.
        double x;
        if (d >= 1.0d) {
            x = d * 0.5d;
        } else {
            x = 1.0d;
        }

        // Newton-Raphson: x(n+1) = (x + d/x) / 2
        for (int i = 0; i < 30; i++) {
            double next = 0.5d * (x + d / x);
            if (abs(next - x) < EPSILON) {
                return next;
            }
            x = next;
        }
        return x;
    }

    // CBRT
	// calcular raiz cúbica
    public static double cbrt(double d) {
        if (d != d) {
            return Double.NaN;
        }
        if (d == 0.0d) {
            return d;
        }
        if (d == Double.POSITIVE_INFINITY || d == Double.NEGATIVE_INFINITY) {
            return d;
        }

        boolean negative = d < 0.0d;

        if (negative) {
            d = -d;
        }

        double x;

        if (d >= 1.0d) {
            x = d / 3.0d;
        } else {
            x = 1.0d;
        }

        // Newton-Raphson para x³ = d * x(n+1) = (2x + d/x²) / 3
        for (int i = 0; i < 40; i++) {
            double next = (2.0d * x + d / (x * x)) / 3.0d;
            if (abs(next - x) < EPSILON) {
                x = next;
                break;
            }
            x = next;
        }
        return negative ? -x : x;
    }


    // EXP
	// calcular exponente
    public static double exp(double d) {
        if (d != d) {
            return Double.NaN;
        }
        if (d == Double.POSITIVE_INFINITY) {
            return Double.POSITIVE_INFINITY;
        }
        if (d == Double.NEGATIVE_INFINITY) {
            return 0.0d;
        }
        // e^x = 2^n * e^r
        // x = n*ln(2) + r
        int n = (int)(d / LN2);
        double r = d - n * LN2;

        // Serie de Taylor para e^r.
        double term = 1.0d;
        double result = 1.0d;
        for (int i = 1; i < 100; i++) {
            term *= r / i;
            result += term;
            if (abs(term) < EPSILON) {
                break;
            }
        }

        // Multiplicación por 2^n.
        if (n > 0) {
            for (int i = 0; i < n; i++) {
                if (result >
                    Double.MAX_VALUE * 0.5d) {
                    return Double.POSITIVE_INFINITY;
                }
                result *= 2.0d;
            }
        } else if (n < 0) {
            for (int i = 0; i > n; i--) {
                result *= 0.5d;
            }
        }
        return result;
    }


    // EXPM1
	// calcular exponente matricial
    public static double expm1(double d) {
        if (d != d) {
            return Double.NaN;
        }
        /*
         * Para valores pequeños:         
         * e^x - 1         
         * debe calcularse directamente para evitar
         * pérdida de precisión.
         */

        if (abs(d) < 0.1d) {
            double term = d;
            double result = d;
            for (int i = 2; i < 80; i++) {
                term *= d / i;
                result += term;
                if (abs(term) < EPSILON) {
                    break;
                }
            }
            return result;
        }
        return exp(d) - 1.0d;
    }

    // LOG NATURAL
	// calcular logaritmo natural
    public static double log(double d) {
        if (d != d) {
            return Double.NaN;
        }
        if (d < 0.0d) {
            return Double.NaN;
        }
        if (d == 0.0d) {
            return Double.NEGATIVE_INFINITY;
        }
        if (d == Double.POSITIVE_INFINITY) {
            return Double.POSITIVE_INFINITY;
        }
        /*
         * Normalización:
         *
         * d = m * 2^exponent
         *
         * 0.5 <= m < 2
         */

        int exponent = 0;
        while (d > SQRT2) {
            d *= 0.5d;
            exponent++;
        }
        while (d < (1.0d / SQRT2)) {
            d *= 2.0d;
            exponent--;
        }
        /*
         * log(x) = 2 * (y + y³/3 + y⁵/5 + ...)
         * y = (x-1)/(x+1)
         */

        double y = (d - 1.0d) /(d + 1.0d);
        double y2 = y * y;
        double term = y;
        double result = y;
        for (int i = 1; i < 150; i++) {
            term *= y2;
            double add = term / (2.0d * i + 1.0d);
            result += add;
            if (abs(add) < EPSILON) {
                break;
            }
        }

        return 2.0d * result + exponent * LN2;
    }


    // LOG10
	// logaritmo base 10
    public static double log10(double d) {
        return log(d) * INV_LN10;
    }

    // LOG1P
    public static double log1p(double d) {
        if (d != d) {
            return Double.NaN;
        }
        if (d == -1.0d) {
            return Double.NEGATIVE_INFINITY;
        }
        if (d < -1.0d) {
            return Double.NaN;
        }
        /*
         * Para x pequeño:
         *
         * log(1+x) =
         *
         * x - x²/2 + x³/3 - ...
         */

        if (abs(d) < 0.5d) {

            double term = d;
            double result = d;

            for (int i = 2; i < 150; i++) {
                term *= -d;
                double add = term / i;
                result += add;
                if (abs(add) < EPSILON) {
                    break;
                }
			}
            return result;
        }
        return log(1.0d + d);
    }

    // POW
	// calcular potencia
    public static double pow(double x, double y) {
        if (x != x || y != y) {
            return Double.NaN;
        }
        if (y == 0.0d) {
            return 1.0d;
        }
        if (x == 1.0d) {
            return 1.0d;
        }
        // Optimización para exponentes enteros.
        long yi = (long)y;
        if ((double)yi == y) {
            return powInteger(x, yi);
        }
        /*
         * Una base negativa con exponente real
         * no entero no pertenece a los reales.
         */
        if (x < 0.0d) {
            return Double.NaN;
        }
        if (x == 0.0d) {
            if (y > 0.0d) {
                return 0.0d;
            }
            return Double.POSITIVE_INFINITY;
        }
        return exp(y * log(x));
    }

	// calcular potencia enteros
    private static double powInteger(double x, long exponent) {
        if (exponent == 0L) {
            return 1.0d;
        }
        boolean negativeExponent = exponent < 0L;
        /*
         * Evitamos negación directa problemática
         * de Long.MIN_VALUE.
         */
        long n;
        if (exponent == Long.MIN_VALUE) {
            /*
             * x^Long.MIN_VALUE =
             *
             * 1 / (x^Long.MAX_VALUE * x)
             */
            double result = powInteger(x, Long.MAX_VALUE);
            result *= x;
            return 1.0d / result;
        }

        n = negativeExponent ? -exponent : exponent;
        double result = 1.0d;
        double base = x;
        // Exponenciación binaria.
        while (n > 0L) {
            if ((n & 1L) != 0L) {
                result *= base;
            }
            base *= base;
            n >>= 1;
        }
        return negativeExponent ? 1.0d / result : result;
    }

    // HYPOT
	// calcular hipotenusa
    public static double hypot(double x, double y) {
        x = abs(x);
        y = abs(y);
        if (x < y) {
            double temp = x;
            x = y;
            y = temp;
        }
        if (x == 0.0d) {
            return 0.0d;
        }
        double ratio = y / x;
        return x * sqrt(1.0d + ratio * ratio);
    }

    // IEEE REMAINDER
	// calcular resto
    public static double IEEEremainder(double x, double y) {
        if (x != x || y != y) {
            return Double.NaN;
        }
        if (y == 0.0d) {
            return Double.NaN;
        }
        if (x == Double.POSITIVE_INFINITY || x == Double.NEGATIVE_INFINITY) {
            return Double.NaN;
        }
        // remainder = x - y * n
		// donde n es el entero más cercano a x/y.
        double quotient = x / y;
        double n = rint(quotient);
        return x - y * n;
    }

    // FLOOR
	// calcular piso
    public static double floor(double d) {
        if (d != d) {
            return Double.NaN;
        }
        if (d == Double.POSITIVE_INFINITY || d == Double.NEGATIVE_INFINITY) {
            return d;
        }
        // El cast a long trunca hacia cero.
        if (d >= (double)Long.MAX_VALUE || d <= (double)Long.MIN_VALUE) {
            return d;
        }
        long value = (long)d;
        if (d < 0.0d && d != (double)value) {
            value--;
        }
        return (double)value;
    }

    // CEIL
	// calcular techo
    public static double ceil(double d) {
        if (d != d) {
            return Double.NaN;
        }
        if (d == Double.POSITIVE_INFINITY || d == Double.NEGATIVE_INFINITY) {
            return d;
        }
        if (d >= (double)Long.MAX_VALUE || d <= (double)Long.MIN_VALUE) {
            return d;
        }
        long value = (long)d;
        if (d > 0.0d && d != (double)value) {
            value++;
        }
        return (double)value;
    }


    // RINT
	// calcular redondeo al número par más cercano
    public static double rint(double d) {
        if (d != d) {
            return Double.NaN;
		}
        if (d == Double.POSITIVE_INFINITY || d == Double.NEGATIVE_INFINITY) {
            return d;
        }
        //aproximación inicial a IEEE-754.
        if (d >= 0.0d) {
            return floor(d + 0.5d);
        }
        return ceil(d - 0.5d);
    }

    // COSH
    // calcular coseno hiperbólico
    public static double cosh(double d) {
        if (d < 0.0d) {
            d = -d;
        }
        if (d > 20.0d) {
			// cosh(x) ~ e^x / 2
            return exp(d) * 0.5d;
        }
        double e = exp(d);
        return (e + 1.0d / e) * 0.5d;
    }


    // SINH
	// calcular seno hiperbólico
    public static double sinh(double d) {
        if (d == 0.0d) {
            return d;
        }
        boolean negative = d < 0.0d;
        if (negative) {
            d = -d;
        }
        double result;
        if (d > 20.0d) {
            result = exp(d) * 0.5d;
        } else {
            double e = exp(d);
            result = (e - 1.0d / e) * 0.5d;
        }
        return negative ? -result : result;
    }


    // TANH
	// calcular tangente hiperbólica
    public static double tanh(double d) {
        if (d != d) {
            return Double.NaN;
        }
        if (d > 20.0d) {
            return 1.0d;
        }
        if (d < -20.0d) {
            return -1.0d;
        }
        // tanh(x) = (e^(2x)-1) / (e^(2x)+1)
        double e = exp(2.0d * d);
        return (e - 1.0d) / (e + 1.0d);
    }


    // ROUND
	// redondeo long por exceso
    public static long round(double d) {
        if (d != d) {
            return 0L;
        }
        return (long)floor(d + 0.5d);
    }

	// redondeo float por exceso
    public static int round(float f) {
        if (f != f) {
            return 0;
        }
        return (int)floor(f + 0.5f);
    }


    // SIGNUM
	// cambiar signo a un double
    public static double signum(double d) {
        if (d != d) {
            return Double.NaN;
        }
        if (d > 0.0d) {
            return 1.0d;
        }
        if (d < 0.0d) {
            return -1.0d;
        }
        return d;
    }

	// cambiar signo a un float
    public static float signum(float f) {
        if (f != f) {
            return Float.NaN;
        }
        if (f > 0.0f) {
            return 1.0f;
        }
        if (f < 0.0f) {
            return -1.0f;
        }
        return f;
    }


    // Random
	// generar double
    public static double random() {
        return INSTANCE.nextDouble();
    }
	
	// establecer semilla
    public static void setRandomSeedInternal(long seed) {
        INSTANCE.setSeed(seed);
    }
	
	// generar entero
    public static int randomIntInternal() {
        return INSTANCE.nextInt();
    }

    // CONVERSIONES ANGULARES
	// a radianes
    public static double toRadians(double angdeg) {
        return angdeg * PI / 180.0d;
    }
	// a grados
    public static double toDegrees( double angrad) {
        return angrad * 180.0d /PI;
	}
}
