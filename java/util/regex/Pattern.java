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

package java.util.regex;

import java.io.IOException;
//import java.io.ObjectInputStream;
import java.io.Serializable;

public final class Pattern implements Serializable {
	
	// tomado de la implementación original
    private static final long serialVersionUID = 5073258162644648461L;
    
	 // constante equivalente en UNIX para '\n'
    public static final int UNIX_LINES = 0x01;
    
	// constante para indicar el modo de comparación
    public static final int CASE_INSENSITIVE = 0x02;
    
	// constante para indicar comentario o espacio vacio
    public static final int COMMENTS = 0x04;
    
	// constante para indicar '^' y '$' (inicio y fin de una cadena respectivamente)
    public static final int MULTILINE = 0x08;
    
	// constante para indicar que la cadena será tomada tal cual 
    public static final int LITERAL = 0x10;
    
	// constante '.' para indicar que el metacarácter coincide con caracteres arbitrarios, incluidos los saltos de línea, lo cual normalmente no sucede.
    public static final int DOTALL = 0x20;

    // constante para indicar que se realiza coincidencias sin distinguir entre mayúsculas y minúsculas
    public static final int UNICODE_CASE = 0x40;

    // Esta constante especifica que un carácter en un Pattern y un carácter en la cadena de entrada solo coinciden si son canónicamente equivalentes.
    public static final int CANON_EQ = 0x80;

    private final String pattern;
    private final int flags;

    transient long address;

    /**
     * Devuelve un Matcher para este patrón aplicado a la 'input' dada. 
	 * El Matcher puede utilizarse para comparar el Pattern con toda la entrada, encontrar apariciones del Pattern en la entrada o reemplazar partes de la entrada.
     */
    public Matcher matcher(CharSequence input) {
        return new Matcher(this, input);
    }

	// Dividir en partes una cadena según límite indicado 
    public String[] split(CharSequence input, int limit) {
        return Splitter.split(this, pattern, input.toString(), limit);
    }

    // Divide en partes toda la cadena, dejando en el primer grupo la cadena completa (String[0]) 
    public String[] split(CharSequence input) {
        return split(input, 0);
    }
    
	// Devuelve la expresión regular para compilar 
    public String pattern() {
        return pattern;
    }

    @Override
    public String toString() {
        return pattern;
    }

    // Devuelve la bandera establecida
    public int flags() {
        return flags;
    }

    /**
     * Devuelve una forma compilada de la {@code regularExpression} dada, modificada por las banderas especificadas.
	 * lanzar PatternSyntaxException si la expresión regular es sintácticamente incorrecta.
     */
    public static Pattern compile(String regularExpression, int flags) throws PatternSyntaxException {
        return new Pattern(regularExpression, flags);
    }
    
    public static Pattern compile(String pattern) {
        return new Pattern(pattern, 0);
    }

    private Pattern(String pattern, int flags) throws PatternSyntaxException {
        if ((flags & CANON_EQ) != 0) {
            throw new UnsupportedOperationException("CANON_EQ flag not supported");
        }
        int supportedFlags = CASE_INSENSITIVE | COMMENTS | DOTALL | LITERAL | MULTILINE | UNICODE_CASE | UNIX_LINES;
        if ((flags & ~supportedFlags) != 0) {
            throw new IllegalArgumentException("Unsupported flags: " + (flags & ~supportedFlags));
        }
        this.pattern = pattern;
        this.flags = flags;
        compile();
    }

    private void compile() throws PatternSyntaxException {
        if (pattern == null) {
            throw new NullPointerException("pattern == null");
        }

        String icuPattern = pattern;
        if ((flags & LITERAL) != 0) {
            icuPattern = quote(pattern);
        }

        // Banderas nativas soportadas por ICU.        
        int icuFlags = flags & (CASE_INSENSITIVE | COMMENTS | MULTILINE | DOTALL | UNIX_LINES);

        address = compileImpl(icuPattern, icuFlags);
    }

    /**
     * Comprueba si la 'regularExpression' dada coincide con la 'input' dada. 
	 * Equivale a Pattern.compile(regularExpression).matcher(input).matches(). 
	 * Si se va a utilizar la misma expresión regular para varias operaciones, puede resultar más
	 * eficiente reutilizar un Pattern compilado.
     */
    public static boolean matches(String regularExpression, CharSequence input) {
        return new Matcher(new Pattern(regularExpression, 0), input).matches();
    }

    /**
     * Encierra la 'string' dada entre "\Q" y "\E" para que todos
	 * los metacaracteres pierdan su significado especial. Este método
	 * escapa correctamente las apariciones de "\Q" o "\E" contenidas en la cadena. 
	 * Si el resultado completo se va a pasar literalmente a compile,
	 * suele ser más claro utilizar la bandera 'LITERAL'.
	 */
    public static String quote(String string) {
        StringBuilder sb = new StringBuilder();
        sb.append("\\Q");
        int apos = 0;
        int k;
        while ((k = string.indexOf("\\E", apos)) >= 0) {
            sb.append(string.substring(apos, k + 2)).append("\\\\E\\Q");
            apos = k + 2;
        }
        return sb.append(string.substring(apos)).append("\\E").toString();
    }

    @Override 
	protected void finalize() throws Throwable {
        try {
            closeImpl(address);
        } finally {
            super.finalize();
        }
    }

	// NO usar aún hasta tener ObjectInputStream implementado
    /*private void readObject(ObjectInputStream s) throws IOException, ClassNotFoundException {
        s.defaultReadObject();
        compile();
    }*/

	// NATIVOS - TODO
    private static void closeImpl(long addr);
    private static long compileImpl(String regex, int flags);
}
