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

public class Object {
    
    // Constructor básico
    public Object() {}

    // Comparación de punteros nativa (funciona perfectamente)
    public boolean equals(Object obj) {
        return this == obj; 
    }

    // Debe devolver la dirección de memoria física del objeto en el Heap.
    // TODO: Implementar System.identityHashCode en el JIT
    public int hashCode() {
        return 0; 
    }

    // Forma estándar y segura de la JVM sin causar recursión infinita
    public String toString() {
        // En Java estándar esto es: getClass().getName() + "@" + Integer.toHexString(hashCode());
        // simplificado por ahora
        return "Object@" + Integer.toHexString(hashCode());
    }

    // Retorna los metadatos de la clase.
    public final Class<?> getClass() {
        // TODO - Hasta que el JIT soporte RTTI (Run-Time Type Information)
        return null; 
    }

    protected Object clone() {
        // TODO - evitar lanzar CloneNotSupportedException
        return null; 
    }

    protected void finalize() {
        // TODO- Usado por el Garbage Collector (cuando esté) antes de liberar memoria.
    }


    // MÉTODOS DE SINCRONIZACIÓN - TODO
    public final void notify() {}
    public final void notifyAll() {}
    public final void wait() {}
    public final void wait(long timeout) {}
    public final void wait(long timeout, int nanos) {}
}
