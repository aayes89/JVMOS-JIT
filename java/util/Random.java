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

package java.util;

import java.io.Serializable;

public class Random implements Serializable {
	
	// tomado de implementación original
    private static final long serialVersionUID = 3905348978240129619L;
	// tomado de implementación original	
    private static final long multiplier = 0x5deece66dL;
	// semilla
    private long seed;
	// valor base de semilla
    private static volatile long seedBase = 0;
    
	// Constructor (genera una semilla a partir de los milisegundos actuales)
	// OJO - posible mejora aquí pronto, muy fácil de romper esto
    public Random() {                
        setSeed(java.lang.System.currentTimeMillis() + seedBase);
        ++seedBase;
    }
   
    public Random(long seed) {
        setSeed(seed);
    }
    
	// genera un valor entero pseudo-aleatorio
    protected synchronized int next(int bits) {
        seed = (seed * multiplier + 0xbL) & ((1L << 48) - 1);
        return (int) (seed >>> (48 - bits));
    }
	
	// genera true o false según resultado de next
    public boolean nextBoolean() {
        return next(1) != 0;
    }
    
	// genera un conjunto de bytes en un arreglo dado
    public void nextBytes(byte[] buf) {
        int rand = 0, count = 0, loop = 0;
        while (count < buf.length) {
            if (loop == 0) {
                rand = nextInt();
                loop = 3;
            } else {
                loop--;
            }
            buf[count++] = (byte) rand;
            rand >>= 8;
        }
    }

	// genera un double pseudo-aleatorio
    public double nextDouble() {
        return ((((long) next(26) << 27) + next(27)) / (double) (1L << 53));
    }

	// genera un float pseudo-aleatorio
    public float nextFloat() {
        return (next(24) / 16777216f);
    }

	// genera un valor gausiano pseudo-aleatorio
    public synchronized double nextGaussian() {
        // No tengo aún StrictMath.sqrt ni StrictMath.log        
        return nextDouble() * 2.0 - 1.0; 
    }

    // genera un entero pseudo-aleatorio
	public int nextInt() {
        return next(32);
    }
    
	// igual a nextInt pero con límite
    public int nextInt(int n) {
        if (n <= 0) {
            return 0; // para evitar hlt y panic
        }
        if ((n & -n) == n) {
            return (int) ((n * (long) next(31)) >> 31);
        }
        int bits, val;
        do {
            bits = next(31);
            val = bits % n;
        } while (bits - val + (n - 1) < 0);
        return val;
    }

	// genera un long pseudo-aleatorio
    public long nextLong() {
        return ((long) next(32) << 32) + next(32);
    }
    
	// establecer la semilla para generar valores pseudo-aleatorios
    public synchronized void setSeed(long seed) {
        this.seed = (seed ^ multiplier) & ((1L << 48) - 1);
    }
}
