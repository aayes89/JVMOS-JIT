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

import kernel.Native;

public class Graphics2D {//extends Graphics{
    private Color currentColor = Color.BLACK;

    public Graphics2D(){
        super();
		// para sortear la ausencia de execution automática del <clinit>
        /*if (Color.WHITE == null) {
            Color.WHITE = new Color(0xFFFFFF);
            Color.BLACK = new Color(0x000000);
            Color.RED   = new Color(0xFF0000);
            Color.GREEN = new Color(0x008000);
            Color.BLUE  = new Color(0x0000FF);
        }
        this.currentColor = Color.BLACK;*/
    }

    //@Override
    public void setColor(Color c) {
        if (c != null) {
            this.currentColor = c;
			// Syscall 1: Color activo VRAM
            Native.sys(Native.SYS_SET_COLOR, c.getRGB(), 0, 0, 0);
        }
    }
	//@Override
    public void setColor(int c) {
        this.currentColor = new Color(c);
		// Syscall 1: Color activo VRAM
        Native.sys(Native.SYS_SET_COLOR, 0, 0, c, 0);        
    }
	
    //@Override
    public Color getColor() {
        return currentColor;
    }
    //@Override
    public void fillRect(int x, int y, int width, int height) {
		//Syscall 2: Rellenar rectángulo
        Native.sys(Native.SYS_FILL_RECT, x, y, width, height);
    }
    //@Override
    public void drawRect(int x, int y, int width, int height) {
		// Syscall 3: Dibujar borde rectángulo
        Native.sys(Native.SYS_DRAW_RECT, x, y, width, height);
    }
    //@Override
    public void drawLine(int x1, int y1, int x2, int y2) {
		// Syscall 4: Dibujar línea
        Native.sys(Native.SYS_DRAW_LINE, x1, y1, x2, y2);
    }
    //@Override
    public void drawString(String text, int x, int y) {
        if (text != null) {
			// Syscall 5: Imprimir cadena texto en modo gráfico
            Native.sys(Native.SYS_DRAW_STRING, x, y, text, 0);
        }
    }    
	//@Override
	public int drawChar(char c, int x, int y) {
        // Syscall 15: Renderizar carácter en VRAM
        Native.sys(Native.SYS_DRAW_CHAR, x, y, (int) c, 0);
        return x + 10; // Devuelve la siguiente posición X
    }
	//@Override
	public int drawInt(int value, int x, int y) {
        if (value == 0) {
            drawChar('0', x, y);
        }
        int temp = value;
        int len = 0;
        boolean isNegative = false;
        
        if (temp < 0) {
            isNegative = true;
            temp = -temp;
            value = temp;
            len++;
        }
        
        int t2 = temp;
        while (t2 > 0) {
            len++;
            t2 /= 10;
        }
        
        int currX = x + (len - 1) * 10;
        int endX = x + len * 10;
        
        temp = value;
        while (temp > 0) {
			// Syscall 15: Renderizar carácter en VRAM
            Native.sys(Native.SYS_DRAW_CHAR, currX, y, '0' + (temp % 10), 0);
            currX -= 10;
            temp /= 10;
        }
        
        if (isNegative) {
			// Syscall 15: Renderizar carácter en VRAM
            Native.sys(Native.SYS_DRAW_CHAR, currX, y, '-', 0);
        }
        
        return endX;
    }
    //@Override
    public int getPixel(int x, int y) {
		// Syscall 14: Leer pixel de VRAM
        return Native.sys(Native.SYS_GET_PIXEL, x, y, 0, 0);
    }
	
	// Faltan por añadir drawCircle, fillCircle, drawTriangle, fillTriangle, clipText, etc.
	
}
