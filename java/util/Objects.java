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

import java.util.Arrays;
import java.util.Comparator;

public final class Objects {
  private Objects() {}

  // Retorna 0 si a==b o c.compare(a, b)
  public static <T> int compare(T a, T b, Comparator<? super T> c) {
    if (a == b) {
      return 0;
    }
    return c.compare(a, b);
  }

  // True si ambos argumentos son null
  public static boolean deepEquals(Object a, Object b) {
    if (a == null || b == null) {
      return a == b;
    } else if (a instanceof Object[] && b instanceof Object[]) {
      return Arrays.deepEquals((Object[]) a, (Object[]) b);
    } else if (a instanceof boolean[] && b instanceof boolean[]) {
      return Arrays.equals((boolean[]) a, (boolean[]) b);
    } else if (a instanceof byte[] && b instanceof byte[]) {
      return Arrays.equals((byte[]) a, (byte[]) b);
    } else if (a instanceof char[] && b instanceof char[]) {
      return Arrays.equals((char[]) a, (char[]) b);
    } else if (a instanceof double[] && b instanceof double[]) {
      return Arrays.equals((double[]) a, (double[]) b);
    } else if (a instanceof float[] && b instanceof float[]) {
      return Arrays.equals((float[]) a, (float[]) b);
    } else if (a instanceof int[] && b instanceof int[]) {
      return Arrays.equals((int[]) a, (int[]) b);
    } else if (a instanceof long[] && b instanceof long[]) {
      return Arrays.equals((long[]) a, (long[]) b);
    } else if (a instanceof short[] && b instanceof short[]) {
      return Arrays.equals((short[]) a, (short[]) b);
    }
    return a.equals(b);
  }

  // Son iguales?
  public static boolean equals(Object a, Object b) {
    return (a == null) ? (b == null) : a.equals(b);
  }

  // Calcula el hash del arreglo
  public static int hash(Object... values) {
    return Arrays.hashCode(values);
  }

	// Devuelve 0 o null 
	public static int hashCode(Object o) {
		return (o == null) ? 0 : o.hashCode();
	}

	// Retorna objeto si no es null, sino excepción
	public static <T> T requireNonNull(T o) {
		return o;
		//if(o==null) throw new NullPointerException("Object null");
	}

  // Retorna objeto si no es null, sino excepción con el mensaje
  public static <T> T requireNonNull(T o, String message) {
    if (o == null) {
      //throw new NullPointerException(message);
	  System.out.println("Objects: NullPointerException." + message);
    }
    return o;
  }
  
  // Verifica que el índice se encuentre dentro del rango [0, length)
  public static int checkIndex(int index, int length) {
    if (index < 0 || index >= length) {
      throw new IndexOutOfBoundsException("Index " + index + " out of bounds for length " + length);
    }
    return index;
  }

  // Retorna cadena de texto equivalente del objeto sino null
  public static String toString(Object o) {
    return (o == null) ? "null" : o.toString();
  }
  public static String toString(Object o, String nullString) {
    return (o == null) ? nullString : o.toString();
  }
  
}
