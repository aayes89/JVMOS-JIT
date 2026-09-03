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

package java.awt;

public abstract class Graphics {
    protected Graphics() {
        // para la invocación invokespecial desde Graphics2D
    }
    public abstract void setColor(Color c);
	public abstract void setColor(int c);
    public abstract Color getColor();
    public abstract void drawLine(int x1, int y1, int x2, int y2);
    public abstract int drawChar(char c, int x, int y);
    public abstract void drawString(String str, int x, int y);
    public abstract void fillRect(int x, int y, int width, int height);
    public abstract void drawRect(int x, int y, int width, int height);
    public abstract int drawInt(int value, int x, int y);   
    public abstract int getPixel(int x, int y);
    
}
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

package java.awt;

public abstract class Graphics {
    protected Graphics() {
        // para la invocación invokespecial desde Graphics2D
    }
    public abstract void setColor(Color c);
    public abstract Color getColor();
    public abstract void drawLine(int x1, int y1, int x2, int y2);
    public abstract int drawChar(char c, int x, int y);
    public abstract void drawString(String str, int x, int y);
    public abstract void fillRect(int x, int y, int width, int height);
    public abstract void drawRect(int x, int y, int width, int height);
    public abstract int drawInt(int value, int x, int y);   
    public abstract int getPixel(int x, int y);
    
}
