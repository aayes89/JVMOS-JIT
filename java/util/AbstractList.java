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

import java.util.function.Consumer;
import java.lang.UnsupportedOperationException;
import java.lang.ConcurrentModificationException;
import java.lang.E;

public abstract class AbstractList<E> extends AbstractCollection<E> implements List<E> {
    // Constructor
    protected AbstractList() {
    }
	
    public boolean add(E e) {
        add(size(), e);
        return true;
    }

    public abstract E get(int index);

    // Reemplaza elemento en posición dada
    public E set(int index, E element) {
        throw new UnsupportedOperationException();
    }
	
	// Agrega elemento a posición, desplaza a la derecha
    public void add(int index, E element) {
        throw new UnsupportedOperationException();
    }

    // Elimina elemento en posición dada
    public E remove(int index) {
        throw new UnsupportedOperationException();
    }


    // Operaciones de búsqueda
	// Retorna el índice de la primera ocurrencia del objeto
	// -1 si no existe
    public int indexOf(Object o) {
        ListIterator<E> it = listIterator();
        if (o==null) {
            while (it.hasNext())
                if (it.next()==null)
                    return it.previousIndex();
        } else {
            while (it.hasNext())
                if (o.equals(it.next()))
                    return it.previousIndex();
        }
        return -1;
    }

    // Retorna el índice de la última ocurrencia del objeto
	// -1 si no está
    public int lastIndexOf(Object o) {
        ListIterator<E> it = listIterator(size());
        if (o==null) {
            while (it.hasPrevious())
                if (it.previous()==null)
                    return it.nextIndex();
        } else {
            while (it.hasPrevious())
                if (o.equals(it.previous()))
                    return it.nextIndex();
        }
        return -1;
    }


    // Operaciones de volcado
	// limpia toda la lista
    public void clear() {
        removeRange(0, size());
    }

    // Agrega a partir del índice dado, toda la colección
    public boolean addAll(int index, Collection<? extends E> c) {
        rangeCheckForAdd(index);
        boolean modified = false;
        for (E e : c) {
            add(index++, e);
            modified = true;
        }
        return modified;
    }


    // Iterators

    // Retorna el iterator correspondiente
    public Iterator<E> iterator() {
        return new Itr();
    }

    // Devuelve el iterator inicial
    public ListIterator<E> listIterator() {
        return listIterator(0);
    }

    // Devuelve la lista de iterator en posición dada
    public ListIterator<E> listIterator(final int index) {
        rangeCheckForAdd(index);

        return new ListItr(index);
    }

    private class Itr implements Iterator<E> {
        // índice
        int cursor = 0;

        // histórico del índice
        int lastRet = -1;

        // control de modificaciones
        int expectedModCount = modCount;

		// Hay más?
        public boolean hasNext() {
            return cursor != size();
        }
		// Devuelve el próximo elemento
        public E next() {
            checkForComodification();
            try {
                int i = cursor;
                E next = get(i);
                lastRet = i;
                cursor = i + 1;
                return next;
            } catch (IndexOutOfBoundsException e) {
                checkForComodification();
                throw new NoSuchElementException(e);
            }
        }

        public void remove() {
            if (lastRet < 0){				
                throw new IllegalStateException();				
			}
            checkForComodification();

            try {
                AbstractList.this.remove(lastRet);
                if (lastRet < cursor)
                    cursor--;
                lastRet = -1;
                expectedModCount = modCount;
            } catch (IndexOutOfBoundsException e) {
                throw new ConcurrentModificationException();
            }
        }

        final void checkForComodification() {
            if (modCount != expectedModCount){				
                throw new ConcurrentModificationException();
			}
        }
    }

	// Clase auxiliar
    private class ListItr extends Itr implements ListIterator<E> {
        ListItr(int index) {
            cursor = index;
        }

        public boolean hasPrevious() {
            return cursor != 0;
        }

        public E previous() {
            checkForComodification();
            try {
                int i = cursor - 1;
                E previous = get(i);
                lastRet = cursor = i;
                return previous;
            } catch (IndexOutOfBoundsException e) {
                checkForComodification();
                throw new NoSuchElementException(e);
            }
        }

        public int nextIndex() {
            return cursor;
        }

        public int previousIndex() {
            return cursor-1;
        }

        public void set(E e) {
            if (lastRet < 0){
                throw new IllegalStateException();
			}
            checkForComodification();

            try {
                AbstractList.this.set(lastRet, e);
                expectedModCount = modCount;
            } catch (IndexOutOfBoundsException ex) {
                throw new ConcurrentModificationException();
            }
        }

        public void add(E e) {
            checkForComodification();

            try {
                int i = cursor;
                AbstractList.this.add(i, e);
                lastRet = -1;
                cursor = i + 1;
                expectedModCount = modCount;
            } catch (IndexOutOfBoundsException ex) {
                throw new ConcurrentModificationException();
            }
        }
    }

    
    public List<E> subList(int fromIndex, int toIndex) {
        subListRangeCheck(fromIndex, toIndex, size());
        return (this instanceof RandomAccess ?
                new RandomAccessSubList<>(this, fromIndex, toIndex) :
                new SubList<>(this, fromIndex, toIndex));
    }

    static void subListRangeCheck(int fromIndex, int toIndex, int size) {
        if (fromIndex < 0)
            throw new IndexOutOfBoundsException("fromIndex = " + fromIndex);
        if (toIndex > size)
            throw new IndexOutOfBoundsException("toIndex = " + toIndex);
        if (fromIndex > toIndex)
            throw new IllegalArgumentException("fromIndex(" + fromIndex +
                                               ") > toIndex(" + toIndex + ")");
    }

    // Comparaciones y hashes
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof List))
            return false;

        ListIterator<E> e1 = listIterator();
        ListIterator<?> e2 = ((List<?>) o).listIterator();
        while (e1.hasNext() && e2.hasNext()) {
            E o1 = e1.next();
            Object o2 = e2.next();
            if (!(o1==null ? o2==null : o1.equals(o2)))
                return false;
        }
        return !(e1.hasNext() || e2.hasNext());
    }

    // Retorna el código hash 
    public int hashCode() {
        int hashCode = 1;
        for (E e : this)
            hashCode = 31*hashCode + (e==null ? 0 : e.hashCode());
        return hashCode;
    }

    // Elimina los elementos en un rango dado
    protected void removeRange(int fromIndex, int toIndex) {
        ListIterator<E> it = listIterator(fromIndex);
        for (int i=0, n=toIndex-fromIndex; i<n; i++) {
            it.next();
            it.remove();
        }
    }
    
    protected int modCount = 0;

    private void rangeCheckForAdd(int index) {
        if (index < 0 || index > size()){
            throw new IndexOutOfBoundsException(outOfBoundsMsg(index));
		}
    }

    private String outOfBoundsMsg(int index) {
        return "Index: "+index+", Size: "+size();
    }

    // Clase auxiliar
    static final class RandomAccessSpliterator<E> implements Spliterator<E> {

        private final List<E> list;
        private int index; // índice actual
        private int fence; // -1 hasta ser usado; entonces será el último valor de index

        // Campos 
        private final AbstractList<E> alist;
        private int expectedModCount; 

		// Constructor por lista
        RandomAccessSpliterator(List<E> list) {
			// No tengo assert implementado aún, menos con soporte
            //assert list instanceof RandomAccess;
			if (!(list instanceof RandomAccess)) {
				throw new IllegalArgumentException("La lista debe implementar RandomAccess");
			}

            this.list = list;
            this.index = 0;
            this.fence = -1;

            this.alist = list instanceof AbstractList ? (AbstractList<E>) list : null;
            this.expectedModCount = alist != null ? alist.modCount : 0;
        }

        // Crea un Spliterator dado un rango dado
        private RandomAccessSpliterator(RandomAccessSpliterator<E> parent,int origin, int fence) {
            this.list = parent.list;
            this.index = origin;
            this.fence = fence;

            this.alist = parent.alist;
            this.expectedModCount = parent.expectedModCount;
        }

        private int getFence() { // inicializa fence con el tamaño en el primer uso
            int hi;
            List<E> lst = list;
            if ((hi = fence) < 0) {
                if (alist != null) {
                    expectedModCount = alist.modCount;
                }
                hi = fence = lst.size();
            }
            return hi;
        }

		// Intentar dividir
        public Spliterator<E> trySplit() {
            int hi = getFence(), lo = index, mid = (lo + hi) >>> 1;
            return (lo >= mid) ? null : // divide a la mitad
                    new RandomAccessSpliterator<>(this, lo, index = mid);
        }

		// Prueba avanzar adelante
        public boolean tryAdvance(Consumer<? super E> action) {
			Objects.requireNonNull(action);
			int hi = getFence(), i = index;
			if (i < hi) {
				index = i + 1;
				action.accept(get(list, i));
				checkAbstractListModCount(alist, expectedModCount);
				return true;
			}
			return false;
		}
	
		// Ejecutar acción para elementos restantes
        public void forEachRemaining(Consumer<? super E> action) {
            Objects.requireNonNull(action);
            List<E> lst = list;
            int hi = getFence();
            int i = index;
            index = hi;
            for (; i < hi; i++) {
                action.accept(get(lst, i));
            }
            checkAbstractListModCount(alist, expectedModCount);
        }

		// Cantidad estimada
        public long estimateSize() {
            return (long) (getFence() - index);
        }

		// Características 
        public int characteristics() {
            return Spliterator.ORDERED | Spliterator.SIZED | Spliterator.SUBSIZED;
        }

		// Devuelve el elemento dada posición
        private static <E> E get(List<E> list, int i) {
            try {
                return list.get(i);
            } catch (IndexOutOfBoundsException ex) {
                throw new ConcurrentModificationException();
            }
        }

        static void checkAbstractListModCount(AbstractList<?> alist, int expectedModCount) {
            if (alist != null && alist.modCount != expectedModCount) {
                throw new ConcurrentModificationException();
            }
        }
    }

	// Clase auxiliar
    private static class SubList<E> extends AbstractList<E> {
        private final AbstractList<E> root;
        private final SubList<E> parent;
        private final int offset;
        protected int size;

        // Constructor
        public SubList(AbstractList<E> root, int fromIndex, int toIndex) {
            this.root = root;
            this.parent = null;
            this.offset = fromIndex;
            this.size = toIndex - fromIndex;
            this.modCount = root.modCount;
        }

        // Genera una sublista a partir de rango
        protected SubList(SubList<E> parent, int fromIndex, int toIndex) {
            this.root = parent.root;
            this.parent = parent;
            this.offset = parent.offset + fromIndex;
            this.size = toIndex - fromIndex;
            this.modCount = root.modCount;
        }
		
		// Reemplaza elemento dada posición
        public E set(int index, E element) {
            Objects.checkIndex(index, size);
            checkForComodification();
            return root.set(offset + index, element);
        }
		
		// Devuelve elemento dado posición		
        public E get(int index) {
            Objects.checkIndex(index, size);
            checkForComodification();
            return root.get(offset + index);
        }
		
		// Está vacio?
		public boolean isEmpty() {
            checkForComodification();
            return size == 0;
        }

		// Devuelve cantidad real existente de elementos
        public int size() {
            checkForComodification();
            return size;
        }

		// Agrega un elemento en posición dada y desplaza a la derecha el resto
        public void add(int index, E element) {
            rangeCheckForAdd(index);
            checkForComodification();
            root.add(offset + index, element);
            updateSizeAndModCount(1);
        }

		// Elimina elemento en posición dada
        public E remove(int index) {
            Objects.checkIndex(index, size);
            checkForComodification();
            E result = root.remove(offset + index);
            updateSizeAndModCount(-1);
            return result;
        }

		// Elimina elementos dado un rango 
        protected void removeRange(int fromIndex, int toIndex) {
            checkForComodification();
            root.removeRange(offset + fromIndex, offset + toIndex);
            updateSizeAndModCount(fromIndex - toIndex);
        }

		// Añade colección al final
        public boolean addAll(Collection<? extends E> c) {
            return addAll(size, c);
        }

		// Añade colección a partir de posición dada
        public boolean addAll(int index, Collection<? extends E> c) {
            rangeCheckForAdd(index);
            int cSize = c.size();
            if (cSize==0)
                return false;
            checkForComodification();
            root.addAll(offset + index, c);
            updateSizeAndModCount(cSize);
            return true;
        }

		// Devuelve iterator
        public Iterator<E> iterator() {
            return listIterator();
        }
		
		// Devuelve lista de iterator correspondiente a elemento en posición dada
        public ListIterator<E> listIterator(int index) {
            checkForComodification();
            rangeCheckForAdd(index);

            return new ListIterator<E>() {
                private final ListIterator<E> i =
                        root.listIterator(offset + index);

                public boolean hasNext() {
                    return nextIndex() < size;
                }

                public E next() {
                    if (hasNext()){
                        return i.next();
					} else {
                        throw new NoSuchElementException();
					}
                }

                public boolean hasPrevious() {
                    return previousIndex() >= 0;
                }

                public E previous() {
                    if (hasPrevious()){
                        return i.previous();
					} else {
                        throw new NoSuchElementException();
					}
                }

                public int nextIndex() {
                    return i.nextIndex() - offset;
                }

                public int previousIndex() {
                    return i.previousIndex() - offset;
                }

                public void remove() {
                    i.remove();
                    updateSizeAndModCount(-1);
                }

                public void set(E e) {
                    i.set(e);
                }

                public void add(E e) {
                    i.add(e);
                    updateSizeAndModCount(1);
                }
            };
        }
		
		// Devuelve sublista a partir de rango dado
        public List<E> subList(int fromIndex, int toIndex) {
            subListRangeCheck(fromIndex, toIndex, size);
            return new SubList<>(this, fromIndex, toIndex);
        }
		
		// Verifica si index tiene valor correcto
        private void rangeCheckForAdd(int index) {
            if (index < 0 || index > size){
                throw new IndexOutOfBoundsException(outOfBoundsMsg(index));
			}
        }

        private String outOfBoundsMsg(int index) {
            return "Index: "+index+", Size: "+size;
        }

        private void checkForComodification() {
            if (root.modCount != this.modCount){
                throw new ConcurrentModificationException();
			}
        }

        private void updateSizeAndModCount(int sizeChange) {
            SubList<E> slist = this;
            do {
                slist.size += sizeChange;
                slist.modCount = root.modCount;
                slist = slist.parent;
            } while (slist != null);
        }
    }

    private static class RandomAccessSubList<E> extends SubList<E> implements RandomAccess {

        // Constructor
        RandomAccessSubList(AbstractList<E> root, int fromIndex, int toIndex) {
            super(root, fromIndex, toIndex);
        }

        // Constructor
        RandomAccessSubList(RandomAccessSubList<E> parent,
                int fromIndex, int toIndex) {
            super(parent, fromIndex, toIndex);
        }

        public List<E> subList(int fromIndex, int toIndex) {
            subListRangeCheck(fromIndex, toIndex, size);
            return new RandomAccessSubList<>(this, fromIndex, toIndex);
        }
    }
}
