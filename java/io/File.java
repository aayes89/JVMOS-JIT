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

package java.io;

import java.io.PrintStream;

public class File {
	public static final byte FLAG_EMPTY = 0;
    public static final byte FLAG_FILE  = 1;
    public static final byte FLAG_DIR 	= 2;
	public static final int INVALID_LBA = -1;

	private byte type;
    private String path;
    private String name;    
    private int startLBA;
    private int size;
	private int parentLBA;

    // Nota: File no almacena datos en un sistema real solo las referencias    
	// Constructor completo
    public File(String path, String name, byte type, int startLBA, int size, int parentLBA) {
		if (type != FLAG_EMPTY && type != FLAG_FILE && type != FLAG_DIR) {
			//throw new IllegalArgumentException("Tipo de archivo invalido");
			java.lang.System.out.println("Tipo de archivo invalido");
			return;
		}
        this.path = path;
        this.name = name;
        this.type = type;        
        this.size = size;
        this.startLBA = startLBA;
        this.parentLBA = parentLBA;
    }
	
	public File(String pathname) {
		this.path = pathname;
		this.name = extractName(pathname);
		this.type = FLAG_EMPTY;		
		this.size = 0;		
		this.startLBA = INVALID_LBA;
		this.parentLBA = INVALID_LBA;
	}

    private String extractName(String path) {
        if (path == null || path.length() == 0) {
            return "";
        }

        int lastSlash = path.lastIndexOf('/');

        if (lastSlash < 0) {
            return path;
        }

        if (lastSlash == path.length() - 1) {
            return "";
        }

        return path.substring(lastSlash + 1);
    }
	
	public boolean exists(){		
		return type != FLAG_EMPTY && startLBA >= 0;
	}
	public String getAbsolutePath(){
		return path;
	}
	
	public String getTypeName() {
		switch (type) {
			case FLAG_EMPTY:
				return "Vacio";

			case FLAG_FILE:
				return "Archivo binario";

			case FLAG_DIR:
				return "Directorio";

			default:
				return "Desconocido";
		}
	}
		
	public String getExtension() {
		if (name == null) {
			return "FILE";
		}
		int dotIdx = name.lastIndexOf('.');
		if (dotIdx <= 0 || dotIdx == name.length() - 1) {
			return "FILE";
		}
		return name.substring(dotIdx + 1);
	}

    public String getName() { return name; }
    public String getPath() { return path; }
    public int length() { return size; }
	public boolean isEmpty(){ return type==FLAG_EMPTY; }
    public boolean isDirectory() { return type==FLAG_DIR; }
    public boolean isFile() { return type==FLAG_FILE; }
	public byte getType(){ return type; }
    
    // getter
    public int getStartLBA() { return startLBA; }
	public int getParentLBA() { return parentLBA; }
}
