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

public final class Matcher implements MatchResult {

    // La expresión regular compilada
    private Pattern pattern;

    // La dirección del par nativo. Los usos de este elemento deben sincronizarse manualmente para evitar fallos a nivel nativo.
    private long address;

    // El texto de entrada
    private String input;

    // El inicio de la region o '0' si coincide con el inicio del texto
    private int regionStart;

    // Contiene el final de la región, o input.length() si la coincidencia debe extenderse hasta el final de la entrada.     
    private int regionEnd;

    // mantiene la posición donde tendrá lugar la siguiente operación de anexión.
    private int appendPos;

    // Indica si se ha encontrado una coincidencia durante la operación de búsqueda más reciente.
    private boolean matchFound;

    // Almacena los desplazamientos de la coincidencia más reciente.     
    private int[] matchOffsets;

    // Indica si los límites de la región actúan como anclaje.
    private boolean anchoringBounds = true;

    // Indica si los límites de la región son transparentes.
    private boolean transparentBounds;

    // Crea un buscador de coincidencias para una combinación dada de patrón y entrada. (Ambos elementos pueden modificarse posteriormente.)
    Matcher(Pattern pattern, CharSequence input) {
        usePattern(pattern);
        reset(input);
    }

    // Añadir un fragmento de texto a otro
    public Matcher appendReplacement(StringBuffer buffer, String replacement) {
        buffer.append(input.substring(appendPos, start()));
        appendEvaluated(buffer, replacement);
        appendPos = end();

        return this;
    }

    /**
     * Método auxiliar interno para anexar una cadena dada a un búfer de cadenas. 
	 * Si la cadena contiene referencias a grupos, estas se sustituyen por
	 * el contenido del grupo correspondiente.
     */
    private void appendEvaluated(StringBuffer buffer, String s) {
        boolean escape = false;
        boolean dollar = false;

        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '\\' && !escape) {
                escape = true;
            } else if (c == '$' && !escape) {
                dollar = true;
            } else if (c >= '0' && c <= '9' && dollar) {
                buffer.append(group(c - '0'));
                dollar = false;
            } else {
                buffer.append(c);
                dollar = false;
                escape = false;
            }
        }

        // Este fragmento de código, reproduce un error del JDK.
        if (escape) {
            throw new ArrayIndexOutOfBoundsException(s.length());
        }
    }

    /**
     * Restablece el Matcher.
	 * Esto hace que la región se establezca en toda la entrada. 
	 * Se pierden los resultados de una búsqueda anterior. 
	 * El siguiente intento de encontrar una coincidencia del Pattern en la cadena comenzará al principio de la entrada.
     */
    public Matcher reset() {
        return reset(input, 0, input.length());
    }

    /**
     * Proporciona una nueva entrada y restablece el Matcher. 
	 * Esto hace que la región se establezca en toda la entrada. Se pierden los resultados de una búsqueda anterior. 
	 * El siguiente intento de encontrar una coincidencia del Pattern en la cadena comenzará al principio de la entrada.
     */
    public Matcher reset(CharSequence input) {
        return reset(input, 0, input.length());
    }

    /**
     * Restablece el Matcher.
	 * Se pueden especificar una nueva secuencia de entrada y una nueva región. 
	 * Los resultados de una búsqueda anterior se pierden. 
	 * El siguiente intento de encontrar una aparición del Pattern en la cadena comenzará al inicio de la región. 
	 * Esta es la versión interna de reset() a la que delegan las diversas versiones públicas.     
     */
    private Matcher reset(CharSequence input, int start, int end) {
        if (input == null) {
            throw new IllegalArgumentException("input == null");
        }

        if (start < 0 || end < 0 || start > input.length() || end > input.length() || start > end) {
            throw new IndexOutOfBoundsException();
        }

        this.input = input.toString();
        this.regionStart = start;
        this.regionEnd = end;
        resetForInput();

        matchFound = false;
        appendPos = 0;

        return this;
    }

    /**
     * Establece un nuevo patrón para el Matcher.
	 * Se pierden los resultados de una búsqueda anterior.
	 * El siguiente intento de encontrar una coincidencia del Pattern en la cadena comenzará al principio de la entrada.
     */
    public Matcher usePattern(Pattern pattern) {
        if (pattern == null) {
            throw new IllegalArgumentException("pattern == null");
        }

        this.pattern = pattern;

        synchronized (this) {
            if (address != 0) {
                closeImpl(address);
                address = 0; // In case openImpl throws.
            }
            address = openImpl(pattern.address);
        }

        if (input != null) {
            resetForInput();
        }

        matchOffsets = new int[(groupCount() + 1) * 2];
        matchFound = false;
        return this;
    }

    private void resetForInput() {
        synchronized (this) {
            setInputImpl(address, input, regionStart, regionEnd);
            useAnchoringBoundsImpl(address, anchoringBounds);
            useTransparentBoundsImpl(address, transparentBounds);
        }
    }

    /**
     * Restablece este buscador de coincidencias y establece una región. 
	 * Solo se tienen en cuenta para una coincidencia los caracteres situados dentro de la región.
     */
    public Matcher region(int start, int end) {
        return reset(input, start, end);
    }

    /**
     * Anexa el resto (no coincidente) de la entrada al StringBuffer especificado. 
	 * Este método puede utilizarse junto con find() y appendReplacement(StringBuffer, String)
	 * para recorrer la entrada y reemplazar todas las coincidencias del Pattern por otro contenido.
     */
    public StringBuffer appendTail(StringBuffer buffer) {
        if (appendPos < regionEnd) {
            buffer.append(input.substring(appendPos, regionEnd));
        }
        return buffer;
    }

    // Reemplaza la primera aparición del patrón de este buscador en la entrada por una cadena dada.
    public String replaceFirst(String replacement) {
        reset();
        StringBuffer buffer = new StringBuffer(input.length());
        if (find()) {
            appendReplacement(buffer, replacement);
        }
        return appendTail(buffer).toString();
    }

    // Reemplaza todas las apariciones del patrón de este buscador en la entrada por una cadena dada.
	public String replaceAll(String replacement) {
        reset();
        StringBuffer buffer = new StringBuffer(input.length());
        while (find()) {
            appendReplacement(buffer, replacement);
        }
        return appendTail(buffer).toString();
    }

    // Devuelve la instancia de Pattern utilizada dentro de este matcher.    
    public Pattern pattern() {
        return pattern;
    }

    // Devuelve verdadero si hay otra coincidencia en la entrada, comenzando desde la posición indicada. (Se ignora la región)
    public boolean find(int start) {
        if (start < 0 || start > input.length()) {
            throw new IndexOutOfBoundsException("start=" + start + "; length=" + input.length());
        }

        synchronized (this) {
            matchFound = findImpl(address, input, start, matchOffsets);
        }
        return matchFound;
    }

    /**
     * Avanza a la siguiente aparición del patrón en la entrada. 
	 * Si hubo una coincidencia previa exitosa, el método continúa la búsqueda a partir del primer carácter posterior a dicha coincidencia en la entrada.
	 * De lo contrario, busca ya sea desde el inicio de la región (si se ha establecido una) o desde la posición 0.
     */
    public boolean find() {
        synchronized (this) {
            matchFound = findNextImpl(address, input, matchOffsets);
        }
        return matchFound;
    }

    /**
     * Intenta hacer coincidir el Pattern. 
	 * Comenzando desde el inicio de la región o desde el inicio de la entrada, si no se ha establecido ninguna región.
	 * No requiere que el Pattern coincida con toda la región.
     */
    public boolean lookingAt() {
        synchronized (this) {
            matchFound = lookingAtImpl(address, input, matchOffsets);
        }
        return matchFound;
    }

    // Intenta ajustar el {@link Pattern} a toda la región (o a toda la entrada, si no se ha establecido ninguna región).
    public boolean matches() {
        synchronized (this) {
            matchFound = matchesImpl(address, input, matchOffsets);
        }
        return matchFound;
    }

    // Devuelve una cadena de reemplazo para la cadena dada, con todas las barras invertidas y los signos de dólar escapados.
    public static String quoteReplacement(String s) {
        StringBuilder result = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '\\' || c == '$') {
                result.append('\\');
            }
            result.append(c);
        }
        return result.toString();
    }

    /*
	 * Convierte la coincidencia actual en una instancia independiente de MatchResult
	 * que no depende de este buscador de coincidencias (*matcher*). 
	 * El nuevo objeto no se ve afectado cuando cambia el estado de este buscador.
     */
    public MatchResult toMatchResult() {
        ensureMatch();
        return new MatchResultImpl(input, matchOffsets);
    }

    /**
     * Determina si este buscador tiene habilitados los límites de anclaje. 
	 * Cuando los límites de anclaje están habilitados, el inicio y el final de la entrada coinciden con los metacaracteres '^' y '$'; de lo contrario, no.
	 * Los límites de anclaje están habilitados de forma predeterminada.
     */
    public Matcher useAnchoringBounds(boolean value) {
        synchronized (this) {
            anchoringBounds = value;
            useAnchoringBoundsImpl(address, value);
        }
        return this;
    }

    /**
     * Devuelve `true` si este buscador tiene habilitados los límites de anclaje. 
	 * Cuando los límites de anclaje están habilitados, el inicio y el final de la entrada coinciden con los metacaracteres '^' y '$'; de lo contrario, no.
	 * Los límites de anclaje están habilitados de forma predeterminada.
     */
    public boolean hasAnchoringBounds() {
        return anchoringBounds;
    }

    /**
     * Determina si este buscador tiene habilitados los límites transparentes. 
	 * Cuando los límites transparentes están habilitados, las partes de la entrada situadas fuera de la
	 * región están sujetas a operaciones de *lookahead* y *lookbehind*; de lo contrario, no lo están. 
	 * Los límites transparentes están deshabilitados de forma predeterminada.
     */
    public Matcher useTransparentBounds(boolean value) {
        synchronized (this) {
            transparentBounds = value;
            useTransparentBoundsImpl(address, value);
        }
        return this;
    }

    // Asegura que se haya producido una coincidencia exitosa. Se invoca internamente desde varios puntos de la clase.
    private void ensureMatch() {
        if (!matchFound) {
            throw new IllegalStateException("No successful match so far");
        }
    }

    /**
     * Devuelve `true` si este buscador tiene habilitados los límites transparentes. 
	 * Cuando los límites transparentes están habilitados, las partes de la entrada situadas fuera de la región
	 * están sujetas a *lookahead* y *lookbehind*; de lo contrario, no lo están. 
	 * Los límites transparentes están deshabilitados de forma predeterminada.
     */
    public boolean hasTransparentBounds() {
        return transparentBounds;
    }

    // Devuelve el índice del primer carácter
    public int regionStart() {
        return regionStart;
    }

    /// Devuelve el índice del último carácter 
    public int regionEnd() {
        return regionEnd;
    }

    /**
     * Devuelve `true` si la coincidencia más reciente tuvo éxito y una entrada adicional podría hacer que fallara.
	 * Si este método devuelve `false` y se encontró una coincidencia, entonces 
	 * más datos de entrada podrían modificar dicha coincidencia, pero esta no se perdería. 
	 * Si no se encontró ninguna coincidencia, `requireEnd` carece de sentido.
     */
    public boolean requireEnd() {
        synchronized (this) {
            return requireEndImpl(address);
        }
    }

    /**
     * Devuelve verdadero si la operación de coincidencia más reciente intentó acceder a
	 * texto adicional más allá de la entrada disponible, lo que significa que una entrada adicional
	 * podría cambiar los resultados de la coincidencia.
     */
    public boolean hitEnd() {
        synchronized (this) {
            return hitEndImpl(address);
        }
    }

    @Override protected void finalize() throws Throwable {
        try {
            synchronized (this) {
                closeImpl(address);
            }
        } finally {
            super.finalize();
        }
    }

    // Devuelve una representación del patron en texto
	// (formato en orden que me cargó el IDE Netbeans)
    @Override public String toString() {
        return getClass().getName() + "[pattern=" + pattern() +
            " region=" + regionStart() + "," + regionEnd() +
            " lastmatch=" + (matchFound ? group() : "") + "]";
    }

    
	// lanzar IllegalStateException si no hubo ningún match
    public int end() {
        return end(0);
    }

    // lanzar IllegalStateException si no hubo ningún match
    public int end(int group) {
        ensureMatch();
        return matchOffsets[(group * 2) + 1];
    }

    // lanzar IllegalStateException si no hubo ningún match
    public String group() {
        return group(0);
    }

    // lanzar IllegalStateException si no hubo ningún match
    public String group(int group) {
        ensureMatch();
        int from = matchOffsets[group * 2];
        int to = matchOffsets[(group * 2) + 1];
        if (from == -1 || to == -1) {
            return null;
        } else {
            return input.substring(from, to);
        }
    }

    // lanzar IllegalStateException si no hubo ningún match
	// contar grupos de cadenas
    public int groupCount() {
        synchronized (this) {
            return groupCountImpl(address);
        }
    }

    // lanzar IllegalStateException si no hubo ningún match
    public int start() {
        return start(0);
    }

    // lanzar IllegalStateException si no hubo ningún match
    public int start(int group) throws IllegalStateException {
        ensureMatch();
        return matchOffsets[group * 2];
    }

	// NATIVOS - TODO
    private static void closeImpl(long addr);
    private static boolean findImpl(long addr, String s, int startIndex, int[] offsets);
    private static boolean findNextImpl(long addr, String s, int[] offsets);
    private static int groupCountImpl(long addr);
    private static boolean hitEndImpl(long addr);
    private static boolean lookingAtImpl(long addr, String s, int[] offsets);
    private static boolean matchesImpl(long addr, String s, int[] offsets);
    private static long openImpl(long patternAddr);
    private static boolean requireEndImpl(long addr);
    private static void setInputImpl(long addr, String s, int start, int end);
    private static void useAnchoringBoundsImpl(long addr, boolean value);
    private static void useTransparentBoundsImpl(long addr, boolean value);
}
