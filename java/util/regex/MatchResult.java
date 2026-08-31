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

public interface MatchResult {

    // Devuelve el índice del primer carácter que sigue al texto que coincidió con toda la expresión regular.     
    int end();

    // Devuelve el índice del primer carácter que sigue al texto que coincidió con un grupo determinado
    int end(int group);

    // Devuelve el texto que coincidió con toda la expresión regular.
    String group();

    //Devuelve el texto que coincidió con un grupo determinado de la expresión regular.
    String group(int group);

    // Devuelve el número de grupos en los resultados, que siempre es igual a el número de grupos en la expresión regular original.
    int groupCount();

    // Devuelve el índice del primer carácter del texto que coincidió con toda la expresión regular.
    int start();

    // Devuelve el índice del primer carácter del texto que coincidió con un grupo determinado.
    int start(int group);
}
