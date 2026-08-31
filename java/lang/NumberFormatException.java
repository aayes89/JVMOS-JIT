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

public class NumberFormatException extends IllegalArgumentException {
    // tomado de las implementaciones originales
    static final long serialVersionUID = -2848938806368998894L;    
    // Constructores
    public NumberFormatException () {
        super();
    }   
    public NumberFormatException (String s) {
        super (s);
    }

    // Manejo de las excepciones
    public static NumberFormatException forInputString(String s, int radix) {
        return new NumberFormatException("For input string: \"" + s + "\"" + (radix == 10 ? "" : " under radix " + radix));
    }
    
    public static NumberFormatException forCharSequence(CharSequence s, int beginIndex, int endIndex, int errorIndex) {
        return new NumberFormatException("Error at index " + (errorIndex - beginIndex) + " in: \"" + s.subSequence(beginIndex, endIndex) + "\"");
    }
}
