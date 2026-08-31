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

public final class String {
	// variable local
    private final byte[] value;
    // Constructor
    public String(byte[] bytes) {
        this.value = bytes;
    }   
    // comparar case-sensitive
    public boolean equals(Object anObject) {
        if (this == anObject) return true;
        if (anObject instanceof String) {
            String aString = (String)anObject;
            if (this.length() != aString.length()) return false;
            for (int i = 0; i < this.length(); i++) {
                if (this.value[i] != aString.getBytes()[i]) return false;
            }
            return true;
        }
        return false;
    }
    
	// obtener el texto de un número
    public static String valueOf(int i) {
        return new StringBuilder().append(i).toString();
    }
	
	// útil para StringBuilder y PrintStream
    public byte[] getBytes() {
        return value;
    }
	
	// longitud del texto
	public int length() {
        return value != null ? value.length : 0;
    }    
    // útil para calcular hashes
    public int hashCode() {
        int h = 0;
        for (int i = 0; i < length(); i++) {
            h = 31 * h + value[i];
        }
        return h;
    }
}
