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

public final class Class<T> {

	// transitorios para manejo de la clase
    private transient ClassLoader classLoader;
    private transient Class<?> componentType;
    private transient String name;
    private transient Class<? super T> superClass;

	// constructor
    private Class() {}

	// getter
    public String toString() {
        return getName();
    }
	
	// obtener nombre de la clase
    public String getName() {
        if (this.name == null) {
            // asignamos un nombre genérico para evitar el cuelgue.
            this.name = "UnknownClass"; 
        }        
        return this.name;
    }

	// obtener el loader
    public ClassLoader getClassLoader() {
        if (isPrimitive()) {
            return null;
        }
        return classLoader;
    }

    public boolean isPrimitive() {        
        return false;
    }
    
    // Métodos útiles para dar soporte a 'Array.java'
    public boolean isArray() {
        return componentType != null;
    }
    
    public Class<?> getComponentType() {
        return componentType;
    }
}
