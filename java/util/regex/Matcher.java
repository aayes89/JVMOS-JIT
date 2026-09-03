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

    private Pattern pattern;
    private String input;
    
    private int regionStart;
    private int regionEnd;
    private int appendPos;
    
    private boolean matchFound;
    private int[] matchOffsets;
    
    private boolean anchoringBounds = true;
    private boolean transparentBounds;

    // Estado interno para el motor de búsqueda en Java
    private int cursor; 

    Matcher(Pattern pattern, CharSequence input) {
        if (pattern == null) {
            throw new IllegalArgumentException("pattern == null");
        }
        this.pattern = pattern;
        reset(input);
    }

    public Matcher appendReplacement(StringBuffer buffer, String replacement) {
        buffer.append(input.substring(appendPos, start()));
        appendEvaluated(buffer, replacement);
        appendPos = end();
        return this;
    }

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

        if (escape) {
            throw new ArrayIndexOutOfBoundsException(s.length());
        }
    }

    public Matcher reset() {
        return reset(input, 0, input.length());
    }

    public Matcher reset(CharSequence input) {
        return reset(input, 0, input.length());
    }

    private Matcher reset(CharSequence input, int start, int end) {
        if (input == null) {
            throw new IllegalArgumentException("input == null");
        }
        if (start < 0 || end < 0 || start > input.length() || end > input.length() || start > end) {
            throw new IndexOutOfBoundsException("start=" + start + "; end=" + end);
        }

        this.input = input.toString();
        this.regionStart = start;
        this.regionEnd = end;
        this.cursor = start;

        this.matchFound = false;
        this.appendPos = 0;
        
        // El array debe tener espacio para todo el texto más los grupos definidos en Pattern
        this.matchOffsets = new int[(pattern.groupCount() + 1) * 2];

        return this;
    }

    public Matcher usePattern(Pattern pattern) {
        if (pattern == null) {
            throw new IllegalArgumentException("pattern == null");
        }
        this.pattern = pattern;
        if (input != null) {
            reset(input, regionStart, regionEnd);
        }
        return this;
    }

    public Matcher region(int start, int end) {
        return reset(input, start, end);
    }

    public StringBuffer appendTail(StringBuffer buffer) {
        if (appendPos < regionEnd) {
            buffer.append(input.substring(appendPos, regionEnd));
        }
        return buffer;
    }

    public String replaceFirst(String replacement) {
        reset();
        StringBuffer buffer = new StringBuffer(input.length());
        if (find()) {
            appendReplacement(buffer, replacement);
        }
        return appendTail(buffer).toString();
    }

    public String replaceAll(String replacement) {
        reset();
        StringBuffer buffer = new StringBuffer(input.length());
        while (find()) {
            appendReplacement(buffer, replacement);
        }
        return appendTail(buffer).toString();
    }

    public Pattern pattern() {
        return pattern;
    }

    public boolean find(int start) {
        if (start < 0 || start > input.length()) {
            throw new IndexOutOfBoundsException("start=" + start + "; length=" + input.length());
        }
        reset();
        this.cursor = start;
        return search(this.cursor);
    }

    public boolean find() {
        int nextSearchIndex = matchFound ? matchOffsets[1] : cursor;
        if (matchFound && matchOffsets[0] == matchOffsets[1]) {
            // Evitar bucles infinitos en coincidencias de longitud cero (ej. "^" o "")
            nextSearchIndex++;
        }
        return search(nextSearchIndex);
    }

    private boolean search(int from) {
        matchFound = false;
        if (from > regionEnd) {
            return false;
        }

        // Delegación directa a la implementación 100% Java del Pattern.
        // El Pattern debe intentar coincidir desde 'from' hasta 'regionEnd'
        // y rellenar 'matchOffsets' con los índices si tiene éxito.
        matchFound = pattern.match(input, from, regionEnd, matchOffsets, anchoringBounds, transparentBounds);
        
        if (matchFound) {
            this.cursor = matchOffsets[1];
        }
        return matchFound;
    }

    public boolean lookingAt() {
        matchFound = pattern.matchAt(input, regionStart, regionEnd, matchOffsets, anchoringBounds, transparentBounds);
        return matchFound;
    }

    public boolean matches() {
        matchFound = pattern.matchEntire(input, regionStart, regionEnd, matchOffsets, anchoringBounds, transparentBounds);
        return matchFound;
    }

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

    public MatchResult toMatchResult() {
        ensureMatch();
        return new MatchResultImpl(input, matchOffsets);
    }

    public Matcher useAnchoringBounds(boolean value) {
        anchoringBounds = value;
        return this;
    }

    public boolean hasAnchoringBounds() {
        return anchoringBounds;
    }

    public Matcher useTransparentBounds(boolean value) {
        transparentBounds = value;
        return this;
    }

    public boolean hasTransparentBounds() {
        return transparentBounds;
    }

    private void ensureMatch() {
        if (!matchFound) {
            throw new IllegalStateException("No successful match so far");
        }
    }

    public int regionStart() {
        return regionStart;
    }

    public int regionEnd() {
        return regionEnd;
    }

    public boolean requireEnd() {
        return pattern.requiresEnd();
    }

    public boolean hitEnd() {
        return pattern.hitEnd();
    }

    public String toString() {
        return getClass().getName() + "[pattern=" + pattern() +
            " region=" + regionStart() + "," + regionEnd() +
            " lastmatch=" + (matchFound ? group() : "") + "]";
    }

    public int end() {
        return end(0);
    }

    public int end(int group) {
        ensureMatch();
        if (group < 0 || group > groupCount()) {
            throw new IndexOutOfBoundsException("No group " + group);
        }
        return matchOffsets[(group * 2) + 1];
    }

    public String group() {
        return group(0);
    }

    public String group(int group) {
        ensureMatch();
        if (group < 0 || group > groupCount()) {
            throw new IndexOutOfBoundsException("No group " + group);
        }
        int from = matchOffsets[group * 2];
        int to = matchOffsets[(group * 2) + 1];
        if (from == -1 || to == -1) {
            return null;
        } else {
            return input.substring(from, to);
        }
    }

    public int groupCount() {
        return pattern.groupCount();
    }

    public int start() {
        return start(0);
    }

    public int start(int group) {
        ensureMatch();
        if (group < 0 || group > groupCount()) {
            throw new IndexOutOfBoundsException("No group " + group);
        }
        return matchOffsets[group * 2];
    }
}
