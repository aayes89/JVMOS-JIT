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

import java.io.PrintStream;
import kernel.Native;

public final class System {
	// Declaración de PrintStream garantiza (System.out.)
    public static PrintStream out;

	// obtener tiempo en ms
    public static long currentTimeMillis() {
        // Syscall 18 = SYS_GET_TICKS
        return Native.sys(18, 0, 0, 0, 0); 
    }
	// equivalente de exit() en Java (probado en qemu)
    public static void exit(int status) {
        // Syscall 17 = SYS_EXIT
        Native.sys(17, status, 0, 0, 0);
    }

    // Copia manual para eludir punteros inseguros de C
    public static void arraycopy(Object src, int srcPos, Object dest, int destPos, int length) {
        if (src instanceof byte[] && dest instanceof byte[]) {
            byte[] s = (byte[]) src;
            byte[] d = (byte[]) dest;
            for (int i = 0; i < length; i++) {
                d[destPos + i] = s[srcPos + i];
            }
        } else if (src instanceof int[] && dest instanceof int[]) {
            int[] s = (int[]) src;
            int[] d = (int[]) dest;
            for (int i = 0; i < length; i++) {
                d[destPos + i] = s[srcPos + i];
            }
        } else if (src instanceof Object[] && dest instanceof Object[]) {
            Object[] s = (Object[]) src;
            Object[] d = (Object[]) dest;
            for (int i = 0; i < length; i++) {
                d[destPos + i] = s[srcPos + i];
            }
        }
    }
	
	// sobrecargas de método getProperty
	public static String getProperty(String key) {
        return getProperty(key, null);
    }
	// equivalente en Java, por ahora todo hardcodeado
    public static String getProperty(String key, String def) {
        if (key == null) return def;
        
        // Propiedades de JVMOS
        if (key.equals("os.name")) return "JVMOS";
        if (key.equals("os.arch")) return "x86";
        if (key.equals("os.version")) return "2.5";
        if (key.equals("java.version")) return "1.8-Baremetal";
        if (key.equals("file.separator")) return "/";
        if (key.equals("line.separator")) return "\n";
        
        return def;
    }
}
