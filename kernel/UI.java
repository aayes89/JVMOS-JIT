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

package kernel;

import java.awt.Graphics2D;
import java.lang.Thread;
import java.lang.System;
import java.io.FileSystem;
import java.awt.ui.JDesktop;

public class UI {
    private static Graphics2D g;
    private static FileSystem fs;
    private static JExplorer explorer;
    private static JEditor editor;
    private static JDesktop desktop;

    public UI(FileSystem fileSys, Portapapeles clip){
        g = new Graphics2D();
        fs = fileSys;    
        editor = new JEditor(fs);
        explorer = new JExplorer(fs, clip, editor);
        desktop = new JDesktop(g, explorer, editor);
    }
    	
    public static int drawChar(int x, int y, int c){ 
		return g.drawChar((char)c,x,y);
	}
    public static void drawString(int x, int y, String text){ 
		g.drawString(text, x, y);
	}    
	public static void shutdown() {
        g.setColor(0x00FF5555); // Rojo
        drawString(380, 360, "SISTEMA APAGADO. CERRANDO EN 2s...");
        Thread.sleep(2000); 
        System.exit(0);
    }

    public void runStartX() {
        int oldMx = 512, oldMy = 384;
        int lastBtn = 0;

        while (true) {
            Native.sys(Native.SYS_MARK_FRAME, 0, 0, 0, 0);

            int mx = System.readMouseEvent(0);
            int my = System.readMouseEvent(1);
            int btn = System.readMouseEvent(2);

            if (mx < 0) mx = 0; if (mx > 1010) mx = 1010;
            if (my < 0) my = 0; if (my > 750) my = 750;

            if (mx != oldMx || my != oldMy) {
                // Iterar sobre TODOS los JFrames para ver si alguno se está arrastrando
                for (int i = 0; i < desktop.getWindowCount(); i++) {
                    java.awt.ui.JFrame win = desktop.getWindows()[i];
                    if (win.getIsDragging()) {
                        win.setX(mx - win.getDragOffsetX());
                        win.setY(my - win.getDragOffsetY());
                    }
                }
            }

            if (btn != lastBtn) {
                desktop.handleMouse(mx, my, btn);
                
                if (btn == 0) {
                    for (int i = 0; i < desktop.getWindowCount(); i++) {
                        desktop.getWindows()[i].setIsDragging(false);
                    }
                }
            }

            desktop.paint();
            showDateTime();
            drawMouse(mx, my);

            oldMx = mx; oldMy = my; lastBtn = btn;

            int ascii = Native.sys(Native.SYS_READ_KEYBOARD, 0, 0, 0, 0);
            if (ascii != 0) {
                if (ascii == 27) break;
                // Enviar teclado a la ventana activa
                if (editor.isVisible()) editor.handleKey(ascii);
            }

            Native.sys(Native.SYS_RESET_FRAME, 0, 0, 0, 0);
            Thread.sleep(1);
        }
        g.clearScreen();
    }
 
    private static void showDateTime(){
        int hour = System.readTime(2), min = System.readTime(1), sec = System.readTime(0);
        int day = System.readTime(3), month = System.readTime(4), year = System.readTime(5);
        g.setColor(0x00C0C0C0); 
		g.fillRect(880, 728, 144, 38);
        g.setColor(0x00000000);

        // Hora Superior
        drawChar(910, 733, (hour / 10) + '0');
		drawChar(920, 733, (hour % 10) + '0'); 
		drawChar(930, 733, ':');
        drawChar(940, 733, (min / 10) + '0'); 
		drawChar(950, 733, (min % 10) + '0'); 
		drawChar(960, 733, ':');
        drawChar(970, 733, (sec / 10) + '0'); 
		drawChar(980, 733, (sec % 10) + '0');
        // Fecha Inferior
        drawChar(890, 750, (day / 10) + '0');
		drawChar(900, 750, (day % 10) + '0');
		drawChar(910, 750, '/');
        drawChar(920, 750, (month / 10) + '0');
		drawChar(930, 750, (month % 10) + '0');
		drawString(940, 750, "/20");
        drawChar(970, 750, (year / 10) + '0');
		drawChar(980, 750, (year % 10) + '0');
    }

    public static void drawMouse(int x, int y) {
        g.setColor(0x00000000);
        for (int i = 0; i < 12; i++) {
			g.fillRect(x, y + i, i + 2, 1);
		}
        g.fillRect(x + 2, y + 12, 4, 5);
        g.setColor(0x00FFFFFF);
        for (int i = 1; i < 10; i++) {
			g.fillRect(x + 1, y + i, i, 1);
		}
        g.fillRect(x + 3, y + 10, 2, 6);
    }
}
