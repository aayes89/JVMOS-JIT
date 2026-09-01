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

public final class RealToString {

    // Patrón Singleton requerido por Double.java y Float.java
    private static final RealToString INSTANCE = new RealToString();

    private RealToString() {
    }

    public static RealToString getInstance() {
        return INSTANCE;
    }

    public String doubleToString(double d) {
        // TODO - La conversión precisa requiere operaciones matemáticas complejas. 
        // Truncamos a la parte entera y añadimos ".0" de forma segura.
        long intPart = (long) d;
        return String.valueOf(intPart) + ".0";
    }

    public String floatToString(float f) {
        long intPart = (long) f;
        return String.valueOf(intPart) + ".0";
    }
    
    public void appendDouble(StringBuilder sb, double d) {
        long intPart = (long) d;
        sb.append(intPart).append(".0");
    }

    public void appendFloat(StringBuilder sb, float f) {
        long intPart = (long) f;
        sb.append(intPart).append(".0");
    }
}
