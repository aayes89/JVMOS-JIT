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

import java.lang.System;
import java.lang.IndexOutOfBoundsException;
	
public class ArrayList {
	// Tomado de implementación original
	private static final long serialVersionUID = 8683452581122892189L;
	// Capacidad inicial		
	private static final int MIN_CAPACITY_INCREMENT = 12;
	// Cantidad de elementos
	int size = 0, modCount = 0;
	// arreglo de elementos
	Object[] array;
		
	// Constructores
	public ArrayList(int capacity) {
		if (capacity < 0) {
			//throw new IllegalArgumentException("capacity < 0: " + capacity);
			System.out.println("IllegalArgumentException(capacity < 0)");
		}
		array = (capacity == 0 ? EmptyArray.OBJECT : new Object[capacity]);
	}
	// Vacío 
	public ArrayList() {
		array = EmptyArray.OBJECT;
	}

	// Añadir un elemento
	public boolean add(E object) {
		Object[] a = array;
		int s = size;
		if (s == a.length) {
			Object[] newArray = new Object[s + (s < (MIN_CAPACITY_INCREMENT / 2) ? MIN_CAPACITY_INCREMENT : s >> 1)];
			System.arraycopy(a, 0, newArray, 0, s);
			array = a = newArray;
		}
		a[s] = object;
		size = s + 1;
		modCount++;
		return true;
	}
	
	// Agregar elemento en posición
	public void add(int index, E object) {
		Object[] a = array;
		int s = size;
		if (index > s || index < 0) {
			throw IndexOutOfBoundsException(index, s);
			//System.out.println("IndexOutOfBoundsException");
		}

		if (s < a.length) {
			System.arraycopy(a, index, a, index + 1, s - index);
		} else {
			// assert s == a.length;
			Object[] newArray = new Object[newCapacity(s)];
			System.arraycopy(a, 0, newArray, 0, index);
			System.arraycopy(a, index, newArray, index + 1, s - index);
			array = a = newArray;
		}
		a[index] = object;
		size = s + 1;
		modCount++;
	}
	
	// Aumentar capacidad del arreglo
	private static int newCapacity(int currentCapacity) {
		int increment = (currentCapacity < (MIN_CAPACITY_INCREMENT / 2) ? MIN_CAPACITY_INCREMENT : currentCapacity >> 1);
		return currentCapacity + increment;
	}
	
	// Limpiar el arreglo
	public void clear() {
		if (size != 0) {
			Arrays.fill(array, 0, size, null);
			size = 0;
			modCount++;
		}
	}
	
	// Devuelve clon del arreglo
	public Object clone() {
		try {
			ArrayList<?> result = (ArrayList<?>) super.clone();
			result.array = array.clone();
			return result;
		} catch (CloneNotSupportedException e) {
		   throw new AssertionError();
		}
	}
	// Garantizar capacidad
	public void ensureCapacity(int minimumCapacity) {
		Object[] a = array;
		if (a.length < minimumCapacity) {
			Object[] newArray = new Object[minimumCapacity];
			System.arraycopy(a, 0, newArray, 0, size);
			array = newArray;
			modCount++;
		}
	}
	// Cantidad de elementos del arreglo
	public int size() {
		return size;
	}
	
	// Está vacio?
	public boolean isEmpty() {
		return size == 0;		
	}
	
	// El elemento existe?
	public boolean contains(Object object) {
		Object[] a = array;
		int s = size;
		if (object != null) {
			for (int i = 0; i < s; i++) {
				if (object.equals(a[i])) {
					return true;
				}
			}
		} else {
			for (int i = 0; i < s; i++) {
				if (a[i] == null) {
					return true;
				}
			}
		}
		return false;
	}

	// Retorna el índice de la primera ocurrencia del objeto en el arreglo
	// -1 si no existe
	public int indexOf(Object object) {
		Object[] a = array;
		int s = size;
		if (object != null) {
			for (int i = 0; i < s; i++) {
				if (object.equals(a[i])) {
					return i;
				}
			}
		} else {
			for (int i = 0; i < s; i++) {
				if (a[i] == null) {
					return i;
				}
			}
		}
		return -1;
	}

	// Retorna el último índice del objeto en el arreglo
	// -1 si no existe
	public int lastIndexOf(Object object) {
		Object[] a = array;
		if (object != null) {
			for (int i = size - 1; i >= 0; i--) {
				if (object.equals(a[i])) {
					return i;
				}
			}
		} else {
			for (int i = size - 1; i >= 0; i--) {
				if (a[i] == null) {
					return i;
				}
			}
		}
		return -1;
	}
	
	// Elimina el objeto en la posición dada
	public E remove(int index) {
		Object[] a = array;
		int s = size;
		if (index >= s) {
			throw IndexOutOfBoundsException(index, s);
		}		
		System.arraycopy(a, index + 1, a, index, --s - index);
		a[s] = null;  // Prevenir fuga, por determinar
		size = s;
		modCount++;
		return result;
	}

	// Elimina el objeto
	public boolean remove(Object object) {
		Object[] a = array;
		int s = size;
		if (object != null) {
			for (int i = 0; i < s; i++) {
				if (object.equals(a[i])) {
					System.arraycopy(a, i + 1, a, i, --s - i);
					a[s] = null;
					size = s;
					modCount++;
					return true;
				}
			}
		} else {
			for (int i = 0; i < s; i++) {
				if (a[i] == null) {
					System.arraycopy(a, i + 1, a, i, --s - i);
					a[s] = null;
					size = s;
					modCount++;
					return true;
				}
			}
		}
		return false;
	}
	
	// Eliminar objetos en un rango dado
	protected void removeRange(int fromIndex, int toIndex) {
		if (fromIndex == toIndex) {
			return;
		}
		Object[] a = array;
		int s = size;
		if (fromIndex >= s) {
			throw new IndexOutOfBoundsException("fromIndex " + fromIndex
					+ " >= size " + size);
		}
		if (toIndex > s) {
			throw new IndexOutOfBoundsException("toIndex " + toIndex
					+ " > size " + size);
		}
		if (fromIndex > toIndex) {
			throw new IndexOutOfBoundsException("fromIndex " + fromIndex
					+ " > toIndex " + toIndex);
		}

		System.arraycopy(a, toIndex, a, fromIndex, s - toIndex);
		int rangeSize = toIndex - fromIndex;
		Arrays.fill(a, s - rangeSize, s, null);
		size = s - rangeSize;
		modCount++;
	}
	
	// Reemplazar elemento en posición dada
	public E set(int index, E object) {
		Object[] a = array;
		if (index >= size) {
			throw IndexOutOfBoundsException(index, size);
		}		
		a[index] = object;
		return result;
	}

	// Retorna el arreglo
	public Object[] toArray() {
		int s = size;
		Object[] result = new Object[s];
		System.arraycopy(array, 0, result, 0, s);
		return result;
	}
	
	// Acorta el arreglo para evitar elementos null
	// Como sanitizar podría decir
	public void trimToSize() {
		int s = size;
		if (s == array.length) {
			return;
		}
		if (s == 0) {
			array = EmptyArray.OBJECT;
		} else {
			Object[] newArray = new Object[s];
			System.arraycopy(array, 0, newArray, 0, s);
			array = newArray;
		}
		modCount++;
	}
	// Calcula el hashCode
	public int hashCode() {
		Object[] a = array;
		int hashCode = 1;
		for (int i = 0, s = size; i < s; i++) {
			Object e = a[i];
			hashCode = 31 * hashCode + (e == null ? 0 : e.hashCode());
		}
		return hashCode;
	}
	
	// Son iguales?
	public boolean equals(Object o) {
		if (o == this) {
			return true;
		}
		if (!(o instanceof List)) {
			return false;
		}
		List<?> that = (List<?>) o;
		int s = size;
		if (that.size() != s) {
			return false;
		}
		Object[] a = array;
		if (that instanceof RandomAccess) {
			for (int i = 0; i < s; i++) {
				Object eThis = a[i];
				Object ethat = that.get(i);
				if (eThis == null ? ethat != null : !eThis.equals(ethat)) {
					return false;
				}
			}
		} else {  // Argument list is not random access; use its iterator
			Iterator<?> it = that.iterator();
			for (int i = 0; i < s; i++) {
				Object eThis = a[i];
				Object eThat = it.next();
				if (eThis == null ? eThat != null : !eThis.equals(eThat)) {
					return false;
				}
			}
		}
		return true;
	}

	

}
