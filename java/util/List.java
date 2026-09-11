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
import java.util.Collection;

public interface List<E> extends Collection<E> {
	// Añadir un elemento en cierta posición
    public void add(int location, E object);
	// Añadir un elemento al final de lista
    public boolean add(E object);
	// Inserta una colección de elementos a partir de una posición
    public boolean addAll(int location, Collection<? extends E> collection);
	// Añadir una colección de elementos al final 
    public boolean addAll(Collection<? extends E> collection);
	// Elimina toda la lista
    public void clear();
	// Comprueba si existe el objeto en la lista
    public boolean contains(Object object);
	// Comprueba si existe la colección de elementos en la lista
    public boolean containsAll(Collection<?> collection);
	// Compara un elemento con el actual en la lista
    public boolean equals(Object object);
	// Retorna el elemento de la posición indicada
    public E get(int location);
	// Retorna el código hash de la lista.
    public int hashCode();
	// Retorna la posición del objeto en la lista.
    public int indexOf(Object object);
	// True si la lista está vacia, de lo contrario False
    public boolean isEmpty();
	// Retorna el iterador de la lista.
    //public Iterator<E> iterator();
	// Retorna el índice de la última aparición del objeto en la lista, -1 si no
    public int lastIndexOf(Object object);
	// Retorna el iterador de lista
    //public ListIterator<E> listIterator();
	// Retorna el iterador de cierto elemento segun posición
    //public ListIterator<E> listIterator(int location);
	// Operación similar a POP en pila (obtiene elemento al inicio y lo elimina de la lista)
    public E remove(int location);
	// Elimina el elemento si existe
    public boolean remove(Object object);
	// Elimina todas las ocurrencias el los elementos de la colección en la lista
    public boolean removeAll(Collection<?> collection);
	// Elimina todos los objetos que no estén en la colección dada
    public boolean retainAll(Collection<?> collection);
	// Reemplaza el elemento en la posición por el dado
    public E set(int location, E object);
	// Retorna la cantidad de elementos en la lista.
    public int size();
    // retornar un subconjunto de la lista
    public List<E> subList(int start, int end);
    // Retorna el arreglo de la clase en Objetos
    public Object[] toArray();
    // Retorna un arreglo con todos los elementos 
    public <T> T[] toArray(T[] array);
}
