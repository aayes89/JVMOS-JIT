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

import java.lang.E;
import java.lang.T;
import java.lang.Object;
import java.lang.System;
import java.lang.Class;
import java.lang.StringBuilder;

public final class Arrays {
	// tomado de implementación original
	private static final long serialVersionUID = -2764017481108945198L;
	private final E[] a;
	
	// constructor
	public Arrays(){
		a = new E[1];
	}

	// comprobar longitud de registro y contar si es válido
	public static void checkOffsetAndCount(int arrayLength, int offset, int count) {
        if (offset < 0 || count < 0 || offset > arrayLength || arrayLength - offset < count) {
            System.out.println("Arrays: Offset and count out of bounds!");
            // Se dispara el jit_op_athrow (cli; hlt) No está implementado aún.
            //throw new IndexOutOfBoundsException();
        }
    }
	
	// True si existe el objeto, False de lo contrario
	public boolean contains(Object object) {
        if (object != null) {
			for(int i=0;i<a.length;i++){
                if (object.equals(a[i])) {
                    return true;
                }
            }
        } else {
            for(int i=0;i<a.length;i++){
                if (a[i] == null) {
                    return true;
                }
            }
        }
        return false;
    }
	
	// Retorna el objecto en la posición dada
	public E get(int location) {
		return (location>=0 && location<a.length)? a[location]: null;
        /*try {
            return a[location];
        } catch (ArrayIndexOutOfBoundsException e) {
            throw new IndexOutOfBoundsException(location, a.length);
        }*/
	}
	
	// Retorna el índice de la primera ocurrencia del objecto, sino está entonces -1
	public int indexOf(Object object) {
		if (object != null) {
			for (int i = 0; i < a.length; i++) {
				if (object.equals(a[i])) {
					return i;
                }
            }
        } else {
			for (int i = 0; i < a.length; i++) {
				if (a[i] == null) {
					return i;
                }
            }
        }
		return -1;
    }
	
	// Retorna el índice de la última ocurrencia del objecto, sino está entonces -1
	public int lastIndexOf(Object object) {
        if (object != null) {
			for (int i = a.length - 1; i >= 0; i--) {
				if (object.equals(a[i])) {
					return i;
                }
            }
        } else {
			for (int i = a.length - 1; i >= 0; i--) {
				if (a[i] == null) {
					return i;
                }
            }
        }
		return -1;
	}
	
	// Reemplaza el elemento de la posición dada y devuelve el que estaba
	public E set(int location, E object) {
        E result = a[location];
		a[location] = object;
		return result;
    }

	// Cantidad de elementos en el arreglo
    public int size() {
		return a.length;
    }

	// Devuelve un clon del arreglo
    public Object[] toArray() {
		return a.clone();
    }
	// Rellenar el arreglo con val en una región
	public static void fill(Object[] array, int val, int cantMax, Object n){
		for(int i=0;i<cantMax;i++){
			array[i] = val;	
		}		
	}
	
	// Comprobación de igualdad a partir de tipos de clase	
	private static boolean deepEqualsElements(Object e1, Object e2) {
        Class<?> cl1, cl2;

        if (e1 == e2) {
            return true;
        }

        if (e1 == null || e2 == null) {
            return false;
        }

        cl1 = e1.getClass().getComponentType();
        cl2 = e2.getClass().getComponentType();

        if (cl1 != cl2) {
            return false;
        }

        if (cl1 == null) {
            return e1.equals(e2);
        }

        /*
         * compare as arrays
         */
        if (e1 instanceof Object[]) {
            return deepEquals((Object[]) e1, (Object[]) e2);
        } else if (cl1 == int.class) {
            return equals((int[]) e1, (int[]) e2);
        } else if (cl1 == char.class) {
            return equals((char[]) e1, (char[]) e2);
        } else if (cl1 == boolean.class) {
            return equals((boolean[]) e1, (boolean[]) e2);
        } else if (cl1 == byte.class) {
            return equals((byte[]) e1, (byte[]) e2);
        } else if (cl1 == long.class) {
            return equals((long[]) e1, (long[]) e2);
        } else if (cl1 == float.class) {
            return equals((float[]) e1, (float[]) e2);
        } else if (cl1 == double.class) {
            return equals((double[]) e1, (double[]) e2);
        } else {
            return equals((short[]) e1, (short[]) e2);
        }
    }
	
	// Comprobación de igualdad en arreglos de objectos
	public static boolean deepEquals(Object[] a, Object[] b) {
        if (a == b) {
            return true;
        }
        if (a == null || b == null || a.length != b.length) {
            return false;
        }
        for (int i = 0; i < a.length; i++) {
            Object e1 = a[i], e2 = b[i];

            if (!deepEqualsElements(e1, e2)) {
                return false;
            }
        }
        return true;
    }
	
	
	public static boolean equals(boolean[] a, boolean[] b){
		if (a == b) {
            return true;
        }
        if (a == null || b == null || a.length != b.length) {
            return false;
        }
        for (int i = 0; i < a.length; i++) {
            boolean e1 = a[i], e2 = b[i];

            if (!deepEqualsElements(e1, e2)) {
                return false;
            }
        }
        return true;
		
	}
	public static boolean equals(byte[] a, byte[] b){
		if (a == b) {
            return true;
        }
        if (a == null || b == null || a.length != b.length) {
            return false;
        }
        for (int i = 0; i < a.length; i++) {
            byte e1 = a[i], e2 = b[i];

            if (!deepEqualsElements(e1, e2)) {
                return false;
            }
        }
        return true;
		
	}
	public static boolean equals(short[] a, short[] b){
		if (a == b) {
            return true;
        }
        if (a == null || b == null || a.length != b.length) {
            return false;
        }
        for (int i = 0; i < a.length; i++) {
            short e1 = a[i], e2 = b[i];

            if (!deepEqualsElements(e1, e2)) {
                return false;
            }
        }
        return true;
	}
	public static boolean equals(char[] a, char[] b){
		if (a == b) {
            return true;
        }
        if (a == null || b == null || a.length != b.length) {
            return false;
        }
        for (int i = 0; i < a.length; i++) {
            char e1 = a[i], e2 = b[i];

            if (!deepEqualsElements(e1, e2)) {
                return false;
            }
        }
        return true;
	}	
	public static boolean equals(int[] a, int[] b){
		if (a == b) {
            return true;
        }
        if (a == null || b == null || a.length != b.length) {
            return false;
        }
        for (int i = 0; i < a.length; i++) {
            int e1 = a[i], e2 = b[i];

            if (!deepEqualsElements(e1, e2)) {
                return false;
            }
        }
        return true;
	}
	public static boolean equals(float[] a, float[] b){
		if (a == b) {
            return true;
        }
        if (a == null || b == null || a.length != b.length) {
            return false;
        }
        for (int i = 0; i < a.length; i++) {
            float e1 = a[i], e2 = b[i];

            if (!deepEqualsElements(e1, e2)) {
                return false;
            }
        }
        return true;
	}
	public static boolean equals(double[] a, double[] b){
		if (a == b) {
            return true;
        }
        if (a == null || b == null || a.length != b.length) {
            return false;
        }
        for (int i = 0; i < a.length; i++) {
            double e1 = a[i], e2 = b[i];

            if (!deepEqualsElements(e1, e2)) {
                return false;
            }
        }
        return true;
	}
	public static boolean equals(long[] a, long[] b){
		if (a == b) {
            return true;
        }
        if (a == null || b == null || a.length != b.length) {
            return false;
        }
        for (int i = 0; i < a.length; i++) {
            long e1 = a[i], e2 = b[i];

            if (!deepEqualsElements(e1, e2)) {
                return false;
            }
        }
        return true;
	}
	
	// Orden ascendente
	public static void sort(byte[] array) {
		for (int i = 0; i < array.length - 1; i++) {
			int min = i;

			for (int j = i + 1; j < array.length; j++) {
				if (array[j] < array[min]) {
					min = j;
				}
			}

			if (min != i) {
				byte tmp = array[i];
				array[i] = array[min];
				array[min] = tmp;
			}
		}
	}
	
	// Devuelve arreglo con '[e]' añadidos 
	public static String toString(boolean[] array) {
        if (array == null) {
            return "null";
        }
        if (array.length == 0) {
            return "[]";
        }
        StringBuilder sb = new StringBuilder(array.length * 7);
        sb.append('[');
        sb.append(array[0]);
        for (int i = 1; i < array.length; i++) {
            sb.append(", ");
            sb.append(array[i]);
        }
        sb.append(']');
        return sb.toString();
    }
	// Devuelve arreglo con '[e]' añadidos 
	public static String toString(byte[] array) {
        if (array == null) {
            return "null";
        }
        if (array.length == 0) {
            return "[]";
        }
        StringBuilder sb = new StringBuilder(array.length * 7);
        sb.append('[');
        sb.append(array[0]);
        for (int i = 1; i < array.length; i++) {
            sb.append(", ");
            sb.append(array[i]);
        }
        sb.append(']');
        return sb.toString();
    }
	// Devuelve arreglo con '[e]' añadidos 
	public static String toString(char[] array) {
        if (array == null) {
            return "null";
        }
        if (array.length == 0) {
            return "[]";
        }
        StringBuilder sb = new StringBuilder(array.length * 7);
        sb.append('[');
        sb.append(array[0]);
        for (int i = 1; i < array.length; i++) {
            sb.append(", ");
            sb.append(array[i]);
        }
        sb.append(']');
        return sb.toString();
    }
	// Devuelve arreglo con '[e]' añadidos 
	public static String toString(double[] array) {
        if (array == null) {
            return "null";
        }
        if (array.length == 0) {
            return "[]";
        }
        StringBuilder sb = new StringBuilder(array.length * 7);
        sb.append('[');
        sb.append(array[0]);
        for (int i = 1; i < array.length; i++) {
            sb.append(", ");
            sb.append(array[i]);
        }
        sb.append(']');
        return sb.toString();
    }
	// Devuelve arreglo con '[e]' añadidos 
	public static String toString(float[] array) {
        if (array == null) {
            return "null";
        }
        if (array.length == 0) {
            return "[]";
        }
        StringBuilder sb = new StringBuilder(array.length * 7);
        sb.append('[');
        sb.append(array[0]);
        for (int i = 1; i < array.length; i++) {
            sb.append(", ");
            sb.append(array[i]);
        }
        sb.append(']');
        return sb.toString();
    }
	// Devuelve arreglo con '[e]' añadidos 
	public static String toString(int[] array) {
        if (array == null) {
            return "null";
        }
        if (array.length == 0) {
            return "[]";
        }
        StringBuilder sb = new StringBuilder(array.length * 7);
        sb.append('[');
        sb.append(array[0]);
        for (int i = 1; i < array.length; i++) {
            sb.append(", ");
            sb.append(array[i]);
        }
        sb.append(']');
        return sb.toString();
    }
	// Devuelve arreglo con '[e]' añadidos 
	public static String toString(long[] array) {
        if (array == null) {
            return "null";
        }
        if (array.length == 0) {
            return "[]";
        }
        StringBuilder sb = new StringBuilder(array.length * 7);
        sb.append('[');
        sb.append(array[0]);
        for (int i = 1; i < array.length; i++) {
            sb.append(", ");
            sb.append(array[i]);
        }
        sb.append(']');
        return sb.toString();
    }
	// Devuelve arreglo con '[e]' añadidos 
	public static String toString(short[] array) {
        if (array == null) {
            return "null";
        }
        if (array.length == 0) {
            return "[]";
        }
        StringBuilder sb = new StringBuilder(array.length * 7);
        sb.append('[');
        sb.append(array[0]);
        for (int i = 1; i < array.length; i++) {
            sb.append(", ");
            sb.append(array[i]);
        }
        sb.append(']');
        return sb.toString();
    }
	// Devuelve arreglo con '[e]' añadidos 
	public static String toString(Object[] array) {
        if (array == null) {
            return "null";
        }
        if (array.length == 0) {
            return "[]";
        }
        StringBuilder sb = new StringBuilder(array.length * 7);
        sb.append('[');
        sb.append(array[0]);
        for (int i = 1; i < array.length; i++) {
            sb.append(", ");
            sb.append(array[i]);
        }
        sb.append(']');
        return sb.toString();
    }
	
	public static <T, U> T[] copyOf(U[] original, int newLength, Class<? extends T[]> newType) {
        T[] copy = ((Object) newType == (Object) Object[].class)
            ? (T[]) new Object[newLength]
            : (T[]) java.lang.reflect.Array.newInstance(newType.getComponentType(), newLength);
        System.arraycopy(original, 0, copy, 0, Math.min(original.length, newLength));
        return copy;
    }

    public static <T> T[] copyOf(T[] original, int newLength) {
        return (T[]) copyOf(original, newLength, original.getClass());
    }
	
	// Copia de un arreglo según rango
	public static boolean[] copyOfRange(boolean[] original, int start, int end) {
        if (start > end) {
            //throw new IllegalArgumentException();
			System.out.println("Arrays: copyOfRange boolean IllegalArgumentException");
        }
        int originalLength = original.length;
        if (start < 0 || start > originalLength) {
            //throw new ArrayIndexOutOfBoundsException();
			System.out.println("Arrays: copyOfRange boolean ArrayIndexOutOfBoundsException");
        }
        int resultLength = end - start;
        int copyLength = Math.min(resultLength, originalLength - start);
        boolean[] result = new boolean[resultLength];
        System.arraycopy(original, start, result, 0, copyLength);
        return result;
    }
	// Copia de un arreglo según rango
	public static char[] copyOfRange(char[] original, int start, int end) {
        if (start > end) {
            //throw new IllegalArgumentException();
			System.out.println("Arrays: copyOfRange char IllegalArgumentException");
        }
        int originalLength = original.length;
        if (start < 0 || start > originalLength) {
            //throw new ArrayIndexOutOfBoundsException();
			System.out.println("Arrays: copyOfRange char ArrayIndexOutOfBoundsException");
        }
        int resultLength = end - start;
        int copyLength = Math.min(resultLength, originalLength - start);
        char[] result = new char[resultLength];
        System.arraycopy(original, start, result, 0, copyLength);
        return result;
    }
	// Copia de un arreglo según rango
	public static int[] copyOfRange(int[] original, int start, int end) {
        if (start > end) {
            //throw new IllegalArgumentException();
			System.out.println("Arrays: copyOfRange int IllegalArgumentException");
        }
        int originalLength = original.length;
        if (start < 0 || start > originalLength) {
            //throw new ArrayIndexOutOfBoundsException();
			System.out.println("Arrays: copyOfRange int ArrayIndexOutOfBoundsException");
        }
        int resultLength = end - start;
        int copyLength = Math.min(resultLength, originalLength - start);
        int[] result = new int[resultLength];
        System.arraycopy(original, start, result, 0, copyLength);
        return result;
    }
	// Copia de un arreglo según rango
	public static byte[] copyOfRange(byte[] original, int start, int end) {
        if (start > end) {
            //throw new IllegalArgumentException();
			System.out.println("Arrays: copyOfRange byte IllegalArgumentException");
        }
        int originalLength = original.length;
        if (start < 0 || start > originalLength) {
            //throw new ArrayIndexOutOfBoundsException();
			System.out.println("Arrays: copyOfRange byte ArrayIndexOutOfBoundsException");
        }
        int resultLength = end - start;
        int copyLength = Math.min(resultLength, originalLength - start);
        byte[] result = new byte[resultLength];
        System.arraycopy(original, start, result, 0, copyLength);
        return result;
    }
	// Copia de un arreglo según rango
	public static short[] copyOfRange(short[] original, int start, int end) {
        if (start > end) {
            //throw new IllegalArgumentException();
			System.out.println("Arrays: copyOfRange short IllegalArgumentException");
        }
        int originalLength = original.length;
        if (start < 0 || start > originalLength) {
            //throw new ArrayIndexOutOfBoundsException();
			System.out.println("Arrays: copyOfRange short ArrayIndexOutOfBoundsException");
        }
        int resultLength = end - start;
        int copyLength = Math.min(resultLength, originalLength - start);
        short[] result = new short[resultLength];
        System.arraycopy(original, start, result, 0, copyLength);
        return result;
    }
	// Copia de un arreglo según rango
	public static float[] copyOfRange(float[] original, int start, int end) {
        if (start > end) {
            //throw new IllegalArgumentException();
			System.out.println("Arrays: copyOfRange float IllegalArgumentException");
        }
        int originalLength = original.length;
        if (start < 0 || start > originalLength) {
            //throw new ArrayIndexOutOfBoundsException();
			System.out.println("Arrays: copyOfRange float ArrayIndexOutOfBoundsException");
        }
        int resultLength = end - start;
        int copyLength = Math.min(resultLength, originalLength - start);
        float[] result = new float[resultLength];
        System.arraycopy(original, start, result, 0, copyLength);
        return result;
    }
	// Copia de un arreglo según rango
	public static double[] copyOfRange(double[] original, int start, int end) {
        if (start > end) {
            //throw new IllegalArgumentException();
			System.out.println("Arrays: copyOfRange double IllegalArgumentException");
        }
        int originalLength = original.length;
        if (start < 0 || start > originalLength) {
            //throw new ArrayIndexOutOfBoundsException();
			System.out.println("Arrays: copyOfRange double ArrayIndexOutOfBoundsException");
        }
        int resultLength = end - start;
        int copyLength = Math.min(resultLength, originalLength - start);
        double[] result = new double[resultLength];
        System.arraycopy(original, start, result, 0, copyLength);
        return result;
    }
	// Copia de un arreglo según rango
	public static long[] copyOfRange(long[] original, int start, int end) {
        if (start > end) {
            //throw new IllegalArgumentException();
			System.out.println("Arrays: copyOfRange long IllegalArgumentException");
        }
        int originalLength = original.length;
        if (start < 0 || start > originalLength) {
            //throw new ArrayIndexOutOfBoundsException();
			System.out.println("Arrays: copyOfRange long ArrayIndexOutOfBoundsException");
        }
        int resultLength = end - start;
        int copyLength = Math.min(resultLength, originalLength - start);
        long[] result = new long[resultLength];
        System.arraycopy(original, start, result, 0, copyLength);
        return result;
    }
	// Copia de un arreglo según rango
	public static String[] copyOfRange(String[] original, int start, int end) {
        if (start > end) {
            //throw new IllegalArgumentException();
			System.out.println("Arrays: copyOfRange String IllegalArgumentException");
        }
        int originalLength = original.length;
        if (start < 0 || start > originalLength) {
            //throw new ArrayIndexOutOfBoundsException();
			System.out.println("Arrays: copyOfRange String ArrayIndexOutOfBoundsException");
        }
        int resultLength = end - start;
        int copyLength = Math.min(resultLength, originalLength - start);
        String[] result = new String[resultLength];
        System.arraycopy(original, start, result, 0, copyLength);
        return result;
    }
	
	public static int hashCode(Object o) {
		return (o == null) ? 0 : o.hashCode();
	}
	
}
