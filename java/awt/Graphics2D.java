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
    private Color currentColor;

    public Graphics2D(){
        super();		
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
        Native.sys(Native.SYS_SET_COLOR, c, 0, 0, 0);
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
            //Native.sys(Native.SYS_DRAW_STRING, x, y, text, 0);
			// Hasta corregir syscall 5, esta versión trabaja bien y mejor.
            drawText(text, x, y);
        }
    } 
	
	// Método especializado para imprimir Strings creados dinámicamente en RAM
    public void drawText(String text, int x, int y) {
        if (text == null) return;
        
        int currentX = x;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == 0) break; // Ignorar nulos para evitar colapsos
            currentX = this.drawChar(c, currentX, y);
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
	
	//@Override
	public void drawPixel(int x, int y){
		fillRect(x,y,1,1);
	}
	
	public void drawPixelAlpha(int x, int y) {		
		// syscall 39: SYS_DRAW_PIXEL_ALPHA
		Native.sys(Native.SYS_DRAW_PIXEL_ALPHA, x, y, 0, 0); 
	}

	public void fillRectAlpha(int x, int y, int w, int h) {		
		for (int i = 0; i < w; i++) {
			for (int j = 0; j < h; j++) {
				// syscall 39: SYS_DRAW_PIXEL_ALPHA
				Native.sys(Native.SYS_DRAW_PIXEL_ALPHA, x + i, y + j, 0, 0);
			}
		}
	}
	
	//@Override
	public void drawTriangle(int x0, int y0,int x1, int y1,	int x2, int y2) {
		drawLine(x0, y0, x1, y1);
		drawLine(x1, y1, x2, y2);
		drawLine(x2, y2, x0, y0);
	}
	
	//@Override
	public void fillTriangle(int x0, int y0,int x1, int y1,int x2, int y2) {
		// Ordenar los vértices por Y: y0 <= y1 <= y2
		if (y0 > y1) {
			int t = x0;
			x0 = x1;
			x1 = t;

			t = y0;
			y0 = y1;
			y1 = t;
		}

		if (y0 > y2) {
			int t = x0;
			x0 = x2;
			x2 = t;

			t = y0;
			y0 = y2;
			y2 = t;
		}

		if (y1 > y2) {
			int t = x1;
			x1 = x2;
			x2 = t;

			t = y1;
			y1 = y2;
			y2 = t;
		}


		// Triángulo plano. Sin área
		if (y0 == y2) {
			return;
		}
		// Parte superior
		if (y1 > y0) {
			for (int y = y0; y < y1; y++) {
				int xA = x0 + ((x1 - x0) * (y - y0)) / (y1 - y0);
				int xB = x0 + ((x2 - x0) * (y - y0)) / (y2 - y0);

				// xA <= xB
				if (xA > xB) {
					int t = xA;
					xA = xB;
					xB = t;
				}

				// Un scanline es simplemente un rectángulo de altura 1.
				fillRect(xA,y,xB - xA + 1,1);
			}
		}

		// Parte inferior
		if (y2 > y1) {
			for (int y = y1; y <= y2; y++) {
				int xA = x1 + ((x2 - x1) * (y - y1)) / (y2 - y1);
				int xB = x0 + ((x2 - x0) * (y - y0)) / (y2 - y0);
				if (xA > xB) {
					int t = xA;
					xA = xB;
					xB = t;
				}
				fillRect(xA,y,xB - xA + 1,1);
			}
		}
	}
	
	//@Override	
	public void drawCircle(int centerX, int centerY, int radius) {
		if (radius < 0) {
			return;
		}
		// Syscall 42: SYS_DRAW_OVAL
		Native.sys(Native.SYS_DRAW_OVAL, centerX -radius, centerY -radius, radius * 2, radius * 2);
	}

	//@Override
	public void fillCircle(int centerX, int centerY, int radius) {
		if (radius <= 0) {
			return;
		}
		// Syscall 43: SYS_FILL_OVAL
		Native.sys(Native.SYS_FILL_OVAL, centerX - radius, centerY - radius, radius * 2, radius * 2);
    }
	
	// Limpiar pantalla (sólo pintar de negro)
	public void clearScreen(){
		setColor(Color.BLACK);
		fillRect(0,0,1024,768);
	}
	
	//@Override
    public void drawPolygon(int[] xPoints, int[] yPoints, int nPoints) {
        if (xPoints == null || yPoints == null || nPoints < 2) return;
        // Syscall 40: drawPolygon
        // Pasamo la REFERENCIA a los arreglos directamente al HAL en ASM
        Native.sys(Native.SYS_DRAW_POLYGON, xPoints, yPoints, nPoints, 0);
    }

    //@Override
    public void fillPolygon(int[] xPoints, int[] yPoints, int nPoints) {
        if (xPoints == null || yPoints == null || nPoints < 3) return;
        // Syscall 41: fillPolygon
        Native.sys(Native.SYS_FILL_POLYGON, xPoints, yPoints, nPoints, 0);
    }

    //@Override
    public void drawOval(int x, int y, int width, int height) {
        if (width <= 0 || height <= 0) return;
        // Syscall 42: drawOval
        Native.sys(Native.SYS_DRAW_OVAL, x, y, width, height);
    }

    //@Override
    public void fillOval(int x, int y, int width, int height) {
        if (width <= 0 || height <= 0) return;
        // Syscall 43: fillOval
        Native.sys(Native.SYS_FILL_OVAL, x, y, width, height);
    }

    //@Override
    public void drawArc(int x, int y, int width, int height) {
        if (width <= 0 || height <= 0) return;
        // Syscall 45: drawArc
        Native.sys(Native.SYS_DRAW_ARC, x, y, width, height);
    }

    //@Override
    public void fillArc(int x, int y, int width, int height) {
        if (width <= 0 || height <= 0) return;
        // Syscall 46: fillArc
        Native.sys(Native.SYS_FILL_ARC, x, y, width, height);
    }

    //@Override
    public void swapBuffers() {
        // Syscall 44: Ejecutar blitting VSYNC
        Native.sys(Native.SYS_SWAP_BUFFERS, 0, 0, 0, 0);
    }
	
	// Faltan por añadir otros pero con los que hay, creo que es suficiente por ahora.
}
