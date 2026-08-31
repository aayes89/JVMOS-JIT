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

import java.util.ArrayList;
import java.util.List;

public class Splitter {
	// Permitir expresiones regulares iniciando con ']' o '}'
    private static final String METACHARACTERS = "\\?*+[](){}^$.|";
	
	// constructor
    private Splitter() {
    }

    // Equivalente a un split pero más rápido
    public static String[] fastSplit(String re, String input, int limit) {
        // Comprobar si se puede dividir rápidamente
        int len = re.length();
        if (len == 0) {
            return null;
        }
        char ch = re.charAt(0);
        if (len == 1 && METACHARACTERS.indexOf(ch) == -1) {
            // buscar un no meta caracter
        } else if (len == 2 && ch == '\\') {
            // buscar un caracter entre comillas.
            ch = re.charAt(1);
            if (METACHARACTERS.indexOf(ch) == -1) {
                return null;
            }
        } else {
            return null;
        }

        // Java devuelve un arreglo con un texto vacio
        if (input.isEmpty()) {
            return new String[] { "" };
        }

        // Contar separadores
        int separatorCount = 0;
        int begin = 0;
        int end;
        while (separatorCount + 1 != limit && (end = input.indexOf(ch, begin)) != -1) {
            ++separatorCount;
            begin = end + 1;
        }
        int lastPartEnd = input.length();
        if (limit == 0 && begin == lastPartEnd) {
            // eliminamos todas las coincidencias vacías finales.
            if (separatorCount == lastPartEnd) {
                // sólo separadores en la entrada
                return EmptyArray.STRING;
            }
            // Encuentrar el inicio de los separadores finales.
            do {
                --begin;
            } while (input.charAt(begin - 1) == ch);
            // Reducimos separatorCount y corrijo lastPartEnd.
            separatorCount -= input.length() - begin;
            lastPartEnd = begin;
        }

        // Recopilar las partes del resultado.
        String[] result = new String[separatorCount + 1];
        begin = 0;
        for (int i = 0; i != separatorCount; ++i) {
            end = input.indexOf(ch, begin);
            result[i] = input.substring(begin, end);
            begin = end + 1;
        }
        // añado la última parte
        result[separatorCount] = input.substring(begin, lastPartEnd);
        return result;
    }

    public static String[] split(Pattern pattern, String re, String input, int limit) {
        String[] fastResult = fastSplit(re, input, limit);
        if (fastResult != null) {
            return fastResult;
        }

        // Java devuelve un arreglo con un texto vacio
        if (input.isEmpty()) {
            return new String[] { "" };
        }

        // Recopilo el texto que precede a cada aparición del separador, siempre que haya espacio suficiente.
        ArrayList<String> list = new ArrayList<String>();
        Matcher matcher = new Matcher(pattern, input);
        int begin = 0;
        while (list.size() + 1 != limit && matcher.find()) {
            list.add(input.substring(begin, matcher.start()));
            begin = matcher.end();
        }
        return finishSplit(list, input, begin, limit);
    }

    private static String[] finishSplit(List<String> list, String input, int begin, int limit) {
        // Añadir texto al final.
        if (begin < input.length()) {
            list.add(input.substring(begin));
        } else if (limit != 0) {
            list.add("");
        } else {
            // Elimina todas las coincidencias vacías finales en el caso de `limit == 0`.
            int i = list.size() - 1;
            while (i >= 0 && list.get(i).isEmpty()) {
                list.remove(i);
                i--;
            }
        }
        // convertir a arreglo
        return list.toArray(new String[list.size()]);
    }
}
