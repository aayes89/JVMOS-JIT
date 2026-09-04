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

public class File {
    public static final byte FLAG_FILE = 1;
    public static final byte FLAG_DIR = 2;

    private String path;
    private String name;
    private boolean isDirectory;
    private int startLBA;
    private int size;

    // File no almacena datos en un sistema real solo las referencias
    
    public File(String pathname) {
        this.path = pathname;
        this.name = extractName(pathname);
        // El administrador de archivos poblará estos datos
        this.startLBA = -1; 
        this.size = 0;
        this.isDirectory = false;
    }

    // Constructor
    public File(String name, boolean isDirectory, int startLBA, int size) {
        this.name = name;
        this.isDirectory = isDirectory;
        this.startLBA = startLBA;
        this.size = size;
    }

    private String extractName(String path) {
        int lastSlash = path.lastIndexOf('/');
        if (lastSlash == -1) return path;
        return path.substring(lastSlash + 1);
    }

    public String getName() { return name; }
    public String getPath() { return path; }
    public int length() { return size; }
    public boolean isDirectory() { return isDirectory; }
    public boolean isFile() { return !isDirectory; }
    
    // getter
    public int getStartLBA() { return startLBA; }
}
