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

public final class String implements CharSequence {
    
    private final byte[] value;

    // Constructores
	public String(){
		value = new byte[0];
	}
    public String(byte[] bytes) {
        this.value = bytes;
    }   
    
    public String(char[] value, int offset, int count) {
        if (offset < 0 || count < 0 || offset + count > value.length) {
            System.out.println("StringIndexOutOfBounds");
            this.value = new byte[0];
            return;
        }
        this.value = new byte[count];
        for (int i = 0; i < count; i++) {
            this.value[i] = (byte) value[offset + i];
        }
    }
	
	public String(byte[] value, int offset, int count) {
        if (offset < 0 || count < 0 || offset + count > value.length) {
            System.out.println("StringIndexOutOfBounds");
            this.value = new byte[0];
            return;
        }
        this.value = new byte[count];        
        System.arraycopy(value, offset, this.value, 0, count);
    }

    // Constructor auxiliar muy útil (por si luego haces 'new String(charArray)')
    public String(char[] value) {
        this(value, 0, value.length);
    }
    
    @Override
    public int length() {
        return value != null ? value.length : 0;
    }

    @Override
    public char charAt(int index) {
        if (index >= 0 && index < length()) {
            return (char) (value[index] & 0xFF);
        }
        System.out.println("StringIndexOutOfBounds");
        return 0;
    }

    @Override
    public CharSequence subSequence(int start, int end) {
        return substring(start, end);
    }

    public String substring(int start) {
        return substring(start, length());
    }

    public String substring(int start, int end) {
        if (start < 0 || end > length() || start > end) {
            System.out.println("StringIndexOutOfBounds");
            return new String(new byte[0]);
        }
        int len = end - start;
        byte[] subBytes = new byte[len];
        System.arraycopy(this.value, start, subBytes, 0, len);
		/*for (int i = 0; i < len; i++) {
            subBytes[i] = this.value[start + i];
        }*/
        return new String(subBytes);
    }
	

    public boolean isEmpty() {
        return length() == 0;
    }
    // Devuelve el índice de un caracter o '-1' si no existe
    public int indexOf(char c) {
        return indexOf(c, 0);
    }

    // Devuelve índice de un caracter buscando a partir de una posición o '-1' si no la encontró
    public int indexOf(char c, int begin) {
        if (begin < 0) {
            begin = 0;
        }
        for (int i = begin; i < length(); i++) {
            if (charAt(i) == c) {
                return i;
            }
        }
        return -1;
    }

    // Devuelve el índice de la cadena o '-1' si no existe
    public int indexOf(String str) {
        return indexOf(str, 0);
    }

    // Devuelve el índice de una subcadena a partir de una posición dada
    public int indexOf(String str, int fromIndex) {
        if (fromIndex < 0) {
            fromIndex = 0;
        }
        if (str == null || str.length() == 0) {
            return fromIndex < length() ? fromIndex : length();
        }
        
        int strLen = str.length();
        int max = length() - strLen;
        
        for (int i = fromIndex; i <= max; i++) {
            boolean match = true;
            for (int j = 0; j < strLen; j++) {
                if (charAt(i + j) != str.charAt(j)) {
                    match = false;
                    break;
                }
            }
            if (match) {
                return i;
            }
        }
        return -1;
    }
	
	// Devuelve el último índice de un caracter buscando a partir de una posición o '-1' si no la encontró
    public int lastIndexOf(char c) {	
		int index = -1;       
        for (int i = 0; i < value.length; i++) {
            if (charAt(i) == c) {
                index = i;
            }
        }
        return index;
    }

    // comprobar si una cadena coincide con otra dada
    public boolean regionMatches(boolean condition, int index, String type, int arg, int length){
        // type = "+Nan", "NaN", "-Nan", "+Infinity", "Infinity", y "-Infinity"
        boolean result = false;
        if(arg == 0 && value.length == length){
            result = true;
        }
        return result;
    }

    // Elimina los espacios en blanco y caracteres de control al principio y al final
    public String trim() {
        int len = length();
        int st = 0;

        // Avanzar el inicio mientras haya espacios o caracteres de control (<= ' ')
        while ((st < len) && (charAt(st) <= ' ')) {
            st++;
        }
        // Retroceder el final mientras haya espacios
        while ((st < len) && (charAt(len - 1) <= ' ')) {
            len--;
        }
        
        return ((st > 0) || (len < length())) ? substring(st, len) : this;
    }

    // Requerido por Boolean.java
    public boolean equalsIgnoreCase(String anotherString) {
        if (this == anotherString) return true;
        if (anotherString == null || anotherString.length() != length()) return false;
        
        for (int i = 0; i < length(); i++) {
            char c1 = charAt(i);
            char c2 = anotherString.charAt(i);
            if (c1 != c2) {
                // Validación ASCII básica para coincidencia de mayúsculas/minúsculas
                if (Character.toUpperCase(c1) != Character.toUpperCase(c2) &&
                    Character.toLowerCase(c1) != Character.toLowerCase(c2)) {
                    return false;
                }
            }
        }
        return true;
    }

    @Override
	public boolean equals(Object anObject) {
		if (this == anObject) return true;
		if (anObject instanceof String) {
			String aString = (String)anObject;
			if (this.length() != aString.length()) return false;

			byte[] otherBytes = aString.getBytes(); // Extracción previa
			for (int i = 0; i < this.length(); i++) {
				if (this.value[i] != otherBytes[i]) return false;
			}
			return true;
		}
		return false;
	}

    public String replace(char character_find, char character_replace) {
        StringBuilder sb = new StringBuilder();
        for(int i = 0; i < length(); i++) {
            if (value[i] == character_find) {
                sb.append(character_replace);
            } else {
                sb.append((char)(value[i] & 0xFF));
            }
        }
        return sb.toString();
    }

    // Reemplazo literal para evitar el uso del motor Regex
    public String replaceAll(String target, String replacement) {
        if (target == null || target.length() == 0) {
            return this; // Evitar ciclos infinitos si la búsqueda está vacía
        }
        if (replacement == null) {
            replacement = "null";
        }

        int index = indexOf(target, 0);
        if (index == -1) {
            return this; // No hay coincidencias, devolvemos el texto original
        }

        StringBuilder sb = new StringBuilder();
        int currentPos = 0;
        int targetLen = target.length();

        while (index != -1) {
            // Añadir el texto previo a la coincidencia
            sb.append(substring(currentPos, index));
            
            // Añadir el nuevo texto de reemplazo
            sb.append(replacement);
            
            // Avanzar el cursor saltando la palabra encontrada
            currentPos = index + targetLen;
            
            // Buscar la siguiente coincidencia a partir de la nueva posición
            index = indexOf(target, currentPos);
        }

        // Añadir el fragmento final de la cadena
        sb.append(substring(currentPos, length()));

        return sb.toString();
    }

    // Reemplazo de la primera coincidencia 
    public String replaceFirst(String regex, String replacement) {
        if (regex == null || regex.length() == 0) {
            return this;
        }
        if (replacement == null) {
            replacement = "null";
        }

        // Soporte específico para el regex "^0+" (quitar ceros a la izquierda)
        if (regex.equals("^0+")) {
            int i = 0;
            // Contar cuántos ceros hay al principio
            while (i < length() && charAt(i) == '0') {
                i++;
            }
            // Si no hay ceros, devolvemos la cadena intacta
            if (i == 0) {
                return this; 
            }
            
            // Construir la nueva cadena reemplazando los ceros
            StringBuilder sb = new StringBuilder();
            sb.append(replacement);
            sb.append(substring(i, length()));
            return sb.toString();
        }

        // Para cualquier otro texto, hacemos una búsqueda literal de la primera aparición
        int index = indexOf(regex, 0);
        if (index == -1) {
            return this; // No se encontró
        }

        StringBuilder sb = new StringBuilder();
        // Texto antes de la coincidencia
        sb.append(substring(0, index));
        // El reemplazo
        sb.append(replacement);
        // Texto después de la coincidencia
        sb.append(substring(index + regex.length(), length()));

        return sb.toString();
    }
    
    public static String valueOf(int i) {
        return new StringBuilder().append(i).toString();
    }
    
    public static String valueOf(Object obj) {
        return (obj == null) ? new String("null".getBytes()) : obj.toString();
    }

    public byte[] getBytes() {
        return value;
    }

    public char[] toCharArray() {
		char[] chars = new char[value.length];

		for (int i = 0; i < value.length; i++) {
			chars[i] = (char) (value[i] & 0xFF);
		}

		return chars;
	}

    public static String valueOf(long l) { return Long.toString(l); }
    public static String valueOf(float f) { return Float.toString(f); }
    public static String valueOf(double d) { return Double.toString(d); }
    public static String valueOf(boolean b) { return b ? "true" : "false"; }
    public static String valueOf(char c) { 
        byte[] charByte = new byte[1];
        charByte[0] = (byte) c;
        return new String(charByte); 
    }
    
    @Override
    public int hashCode() {
        int h = 0;
        for (int i = 0; i < length(); i++) {
            h = 31 * h + value[i];
        }
        return h;
    }

    @Override
    public String toString() {
        return this;
    }
}
