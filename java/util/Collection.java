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
import java.util.Collection;

public interface Collection<E> extends Iterable<E> {
	// Añade un objeto a la colección
    public boolean add(E object);
	// Añade una colección de objetos
    public boolean addAll(Collection<? extends E> collection);
	// Limpia la colección completa
    public void clear();
	// Verifica si existe el object en la colección
    public boolean contains(Object object);
	// Verifica si existen los elementos de la colección 
    public boolean containsAll(Collection<?> collection);
	// Compara el objeto con la colección
    public boolean equals(Object object);
	// Retorna el código hash de la colección
    public int hashCode();
	// Retorna True si está vacía, False de lo contrario
    public boolean isEmpty();
	// Rotorna una instancia del iterador
    //public Iterator<E> iterator();
	
	// Elimina el objeto en la colección
    public boolean remove(Object object);
	// Elimina todas las ocurrencias de los elementos en la colección indicada
    public boolean removeAll(Collection<?> collection);
	// Elimina todos los elementos distintos de la colección dada
    public boolean retainAll(Collection<?> collection);
	// Retorna el tamaño de la colección (cantidad de elementos)
    public int size();
	// Devuelve la colección como arreglo de Objetos
    public Object[] toArray();
	// Devuelve la colección como tipo arreglo
    public <T> T[] toArray(T[] array);
}
