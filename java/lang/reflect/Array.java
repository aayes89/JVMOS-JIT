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

package java.lang.reflect;

public final class Array {
    private Array() {
    }

    public static Object get(Object array, int index) {
        if (array instanceof Object[]) {
            return ((Object[]) array)[index];
        }
        if (array instanceof boolean[]) {
            return ((boolean[]) array)[index] ? Boolean.TRUE : Boolean.FALSE;
        }
        if (array instanceof byte[]) {
            return Byte.valueOf(((byte[]) array)[index]);
        }
        if (array instanceof char[]) {
            return Character.valueOf(((char[]) array)[index]);
        }
        if (array instanceof short[]) {
            return Short.valueOf(((short[]) array)[index]);
        }
        if (array instanceof int[]) {
            return Integer.valueOf(((int[]) array)[index]);
        }
        if (array instanceof long[]) {
            return Long.valueOf(((long[]) array)[index]);
        }
        if (array instanceof float[]) {
            return Float.valueOf(((float[]) array)[index]);
        }
        if (array instanceof double[]) {
            return Double.valueOf(((double[]) array)[index]);
        }
		// Evito lanzar IllegalArgumentException
        return null; 
    }

    public static boolean getBoolean(Object array, int index) {
        if (array instanceof boolean[]) {
            return ((boolean[]) array)[index];
        }
        return false;
    }

    public static byte getByte(Object array, int index) {
        if (array instanceof byte[]) {
            return ((byte[]) array)[index];
        }
        return 0;
    }

    public static char getChar(Object array, int index) {
        if (array instanceof char[]) {
            return ((char[]) array)[index];
        }
        return 0;
    }

    public static double getDouble(Object array, int index) {
        if (array instanceof double[]) {
            return ((double[]) array)[index];
        } else if (array instanceof byte[]) {
            return ((byte[]) array)[index];
        } else if (array instanceof char[]) {
            return ((char[]) array)[index];
        } else if (array instanceof float[]) {
            return ((float[]) array)[index];
        } else if (array instanceof int[]) {
            return ((int[]) array)[index];
        } else if (array instanceof long[]) {
            return ((long[]) array)[index];
        } else if (array instanceof short[]) {
            return ((short[]) array)[index];
        }
        return 0.0;
    }

    public static float getFloat(Object array, int index) {
        if (array instanceof float[]) {
            return ((float[]) array)[index];
        } else if (array instanceof byte[]) {
            return ((byte[]) array)[index];
        } else if (array instanceof char[]) {
            return ((char[]) array)[index];
        } else if (array instanceof int[]) {
            return ((int[]) array)[index];
        } else if (array instanceof long[]) {
            return ((long[]) array)[index];
        } else if (array instanceof short[]) {
            return ((short[]) array)[index];
        }
        return 0.0f;
    }

    public static int getInt(Object array, int index) {
        if (array instanceof int[]) {
            return ((int[]) array)[index];
        } else if (array instanceof byte[]) {
            return ((byte[]) array)[index];
        } else if (array instanceof char[]) {
            return ((char[]) array)[index];
        } else if (array instanceof short[]) {
            return ((short[]) array)[index];
        }
        return 0;
    }

    public static int getLength(Object array) {
        if (array instanceof Object[]) {
            return ((Object[]) array).length;
        } else if (array instanceof boolean[]) {
            return ((boolean[]) array).length;
        } else if (array instanceof byte[]) {
            return ((byte[]) array).length;
        } else if (array instanceof char[]) {
            return ((char[]) array).length;
        } else if (array instanceof double[]) {
            return ((double[]) array).length;
        } else if (array instanceof float[]) {
            return ((float[]) array).length;
        } else if (array instanceof int[]) {
            return ((int[]) array).length;
        } else if (array instanceof long[]) {
            return ((long[]) array).length;
        } else if (array instanceof short[]) {
            return ((short[]) array).length;
        }
        return 0;
    }

    public static long getLong(Object array, int index) {
        if (array instanceof long[]) {
            return ((long[]) array)[index];
        } else if (array instanceof byte[]) {
            return ((byte[]) array)[index];
        } else if (array instanceof char[]) {
            return ((char[]) array)[index];
        } else if (array instanceof int[]) {
            return ((int[]) array)[index];
        } else if (array instanceof short[]) {
            return ((short[]) array)[index];
        }
        return 0L;
    }

    public static short getShort(Object array, int index) {
        if (array instanceof short[]) {
            return ((short[]) array)[index];
        } else if (array instanceof byte[]) {
            return ((byte[]) array)[index];
        }
        return 0;
    }

    // La creación dinámica requiere soporte en JIT para metadatos (RTTI)
	// TODO
    public static Object newInstance(Class<?> componentType, int... dimensions) {
        return null;
    }

    public static Object newInstance(Class<?> componentType, int size) {
        return null;
    }

    public static void set(Object array, int index, Object value) {
        if (array instanceof Object[]) {
            ((Object[]) array)[index] = value;
        } else if (value != null) {
            // Unboxing seguro hacia arreglos primitivos
            if (value instanceof Boolean) {
                setBoolean(array, index, ((Boolean) value).booleanValue());
            } else if (value instanceof Byte) {
                setByte(array, index, ((Byte) value).byteValue());
            } else if (value instanceof Character) {
                setChar(array, index, ((Character) value).charValue());
            } else if (value instanceof Short) {
                setShort(array, index, ((Short) value).shortValue());
            } else if (value instanceof Integer) {
                setInt(array, index, ((Integer) value).intValue());
            } else if (value instanceof Long) {
                setLong(array, index, ((Long) value).longValue());
            } else if (value instanceof Float) {
                setFloat(array, index, ((Float) value).floatValue());
            } else if (value instanceof Double) {
                setDouble(array, index, ((Double) value).doubleValue());
            }
        }
    }

    public static void setBoolean(Object array, int index, boolean value) {
        if (array instanceof boolean[]) ((boolean[]) array)[index] = value;
    }

    public static void setByte(Object array, int index, byte value) {
        if (array instanceof byte[]) {
            ((byte[]) array)[index] = value;
        } else if (array instanceof double[]) {
            ((double[]) array)[index] = value;
        } else if (array instanceof float[]) {
            ((float[]) array)[index] = value;
        } else if (array instanceof int[]) {
            ((int[]) array)[index] = value;
        } else if (array instanceof long[]) {
            ((long[]) array)[index] = value;
        } else if (array instanceof short[]) {
            ((short[]) array)[index] = value;
        }
    }
	
	// son casi un copia y pega, que tedioso!!

    public static void setChar(Object array, int index, char value) {
        if (array instanceof char[]) {
            ((char[]) array)[index] = value;
        } else if (array instanceof double[]) {
            ((double[]) array)[index] = value;
        } else if (array instanceof float[]) {
            ((float[]) array)[index] = value;
        } else if (array instanceof int[]) {
            ((int[]) array)[index] = value;
        } else if (array instanceof long[]) {
            ((long[]) array)[index] = value;
        }
    }

    public static void setDouble(Object array, int index, double value) {
        if (array instanceof double[]) ((double[]) array)[index] = value;
    }

    public static void setFloat(Object array, int index, float value) {
        if (array instanceof float[]) {
            ((float[]) array)[index] = value;
        } else if (array instanceof double[]) {
            ((double[]) array)[index] = value;
        }
    }

    public static void setInt(Object array, int index, int value) {
        if (array instanceof int[]) {
            ((int[]) array)[index] = value;
        } else if (array instanceof double[]) {
            ((double[]) array)[index] = value;
        } else if (array instanceof float[]) {
            ((float[]) array)[index] = value;
        } else if (array instanceof long[]) {
            ((long[]) array)[index] = value;
        }
    }

    public static void setLong(Object array, int index, long value) {
        if (array instanceof long[]) {
            ((long[]) array)[index] = value;
        } else if (array instanceof double[]) {
            ((double[]) array)[index] = value;
        } else if (array instanceof float[]) {
            ((float[]) array)[index] = value;
        }
    }

    public static void setShort(Object array, int index, short value) {
        if (array instanceof short[]) {
            ((short[]) array)[index] = value;
        } else if (array instanceof double[]) {
            ((double[]) array)[index] = value;
        } else if (array instanceof float[]) {
            ((float[]) array)[index] = value;
        } else if (array instanceof int[]) {
            ((int[]) array)[index] = value;
        } else if (array instanceof long[]) {
            ((long[]) array)[index] = value;
        }
    }
}
