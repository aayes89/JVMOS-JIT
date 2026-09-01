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

import java.io.Serializable;

public final class Boolean implements Serializable, Comparable<Boolean> {

    private static final long serialVersionUID = -3665804199014368530L;

    private final boolean value;

    public static final Class<Boolean> TYPE = (Class<Boolean>) boolean[].class.getComponentType();    
    
    public static final Boolean TRUE = new Boolean(true);
    public static final Boolean FALSE = new Boolean(false);
    public Boolean(String string) {
        this(parseBoolean(string));
    }
    public Boolean(boolean value) {
        this.value = value;
    }
    public boolean booleanValue() {
        return value;
    }
    public boolean equals(Object o) {
        return (o == this) || ((o instanceof Boolean) && (((Boolean) o).value == value));
    }

    public int compareTo(Boolean that) {
        return compare(value, that.value);
    }

    public static int compare(boolean lhs, boolean rhs) {
        return lhs == rhs ? 0 : lhs ? 1 : -1;
    }
    
    public int hashCode() {
        return value ? 1231 : 1237;
    }
    
    public String toString() {
        return value ? "true" : "false";
    }
    
    public static boolean getBoolean(String string) {
        if (string == null || string.length() == 0) {
            return false;
        }
        return (parseBoolean(System.getProperty(string)));
    }
    
    public static boolean parseBoolean(String s) {
        return "true".equalsIgnoreCase(s);
    }
    
    public static String toString(boolean value) {
        return value ? "true" : "false";
    }
    
    public static Boolean valueOf(String string) {
        return parseBoolean(string) ? Boolean.TRUE : Boolean.FALSE;
    }
    
    public static Boolean valueOf(boolean b) {
        return b ? Boolean.TRUE : Boolean.FALSE;
    }
}
