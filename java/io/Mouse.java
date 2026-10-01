/*
MIT License

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

import kernel.Native;
import java.lang.System;
import java.awt.Graphics2D;
import java.awt.Color;

// Helper para manejar el mouse
public class Mouse {
	private int posx;
	private int posy;
	private int btn;
	private Graphics2D g;
	private static int[] mPolyX;
	private static int[] mPolyY;

	// Eventos vinculados a operaciones con el mouse (botones, movimiento, presionado, etc.)
	/*
	private static final int BTN_IZQ_PRESS  = 100;  // boton izquierdo presionado
	private static final int BTN_CENT_PRESS = 010;  // boton central presionado
	private static final int BTN_DER_PRESS  = 001;  // boton derecho presionado
	private static final int MOUSE_MOVED 	= 111;	// el mouse se está moviendo
	private static final int MOUSE_STOPPED  = 000;  // el mouse está detenido
	private static final int BTN_IZQ_KPRESS = 110;  // boton izquierdo no soltado
	private static final int BTN_DER_KPRESS = 011;  // boton derecho no soltado
	*/

	// Constructor
	public Mouse(Graphics2D g){
		updateMouseStatus();
		this.g = g;
		mPolyX  = new int[7];
		mPolyY = new int[7];
	}

	public int getMouseButtonPressed(){
		return btn;
	}
	public int getPosX(){
		return posx;
	}
	public int getPosY(){
		return posy;
	}
	public void updateMouseStatus(){
		posx = System.readMouseEvent(0);
		posy = System.readMouseEvent(1);
		btn  = System.readMouseEvent(2);
	}

	public boolean mouseMoved(int x, int y){
		return (posx != x || posy != y);
	}

	public void drawMouse(int x, int y) {			
		mPolyX[0] = x;
		mPolyX[1] = x+10;
		mPolyX[2] = x+5;
		mPolyX[3] = x+8;
		mPolyX[4] = x+5;
		mPolyX[5] = x+3;
		mPolyX[6] = x;
		mPolyY[0] = y;
		mPolyY[1] = y + 10;
		mPolyY[2] = y + 10;
		mPolyY[3] = y + 15;
		mPolyY[4] = y + 16;
		mPolyY[5] = y + 10;
		mPolyY[6] = y + 15;
		g.setColor(Color.WHITE);
		g.fillPolygon(mPolyX,mPolyY,7);	
    }
}
