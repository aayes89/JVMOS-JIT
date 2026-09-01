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

import java.io.Serializable;

public abstract class Enum<E extends Enum<E>> implements Serializable, Comparable<E> {

	// obtenido de la implementación original
    private static final long serialVersionUID = -4300926546619394005L;
	// variables locales
    private final String name;
    private final int ordinal;

	// constructor
    protected Enum(String name, int ordinal) {
        this.name = name;
        this.ordinal = ordinal;
    }

	// getters
    public final String name() {
        return name;
    }

    public final int ordinal() {
        return ordinal;
    }

    public String toString() {
        return name;
    }

    @Override
    public final boolean equals(Object other) {
        return this == other;
    }

    @Override
    public final int hashCode() {
        return ordinal + (name == null ? 0 : name.hashCode());
    }

    protected final Object clone() {		
        // Evitar lanzar CloneNotSupportedException
		// TODO
        return null;
    }

    // comparar objetos
	public final int compareTo(E o) {
        return ordinal - ((Enum<?>) o).ordinal;
    }

    // TODO - por implementar java.lang.Object.getClass() y java.lang.Class.getSuperclass()
    public final Class<E> getDeclaringClass() {
        return null; 
    }

    /*
    public final Class<E> getDeclaringClass() {
        Class<?> myClass = getClass();
        Class<?> mySuperClass = myClass.getSuperclass();
        if (Enum.class == mySuperClass) {
            return (Class<E>)myClass;
        }
        return (Class<E>)mySuperClass;
    }*/

    // Eliminado la reflexión y la caché compleja
    public static <T extends Enum<T>> T valueOf(Class<T> enumType, String name) {
		// temporal hasta implementar Reflection en el JIT
        return null;
    }

    // Stub temporal
    public static <T extends Enum<T>> T[] getSharedConstants(Class<T> enumType) {
        return null; 
    }

    // TODO
    protected final void finalize() {
        
    }
}
