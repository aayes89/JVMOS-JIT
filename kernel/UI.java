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
import java.awt.Color;
import java.lang.Thread;
import java.lang.System;
import java.io.FileSystem;
import java.awt.ui.JDesktop;
import java.awt.ui.JFrame;
import java.awt.ui.JPopupMenu;

public class UI {
    private static Graphics2D g;
    private static FileSystem fs;
    private static JExplorer explorer;
    private static JEditor editor;
    private static JDesktop desktop;
	private static int[] mPolyX;
	private static int[] mPolyY;

    public UI(FileSystem fileSys, Portapapeles clip, Graphics2D g){
        this.g = g;
        fs = fileSys;    
        editor = new JEditor(fs);
        explorer = new JExplorer(fs, clip, editor);
        desktop = new JDesktop(g, explorer, editor);
		mPolyX  = new int[7];
		mPolyY = new int[7];
    }
        
    public static int drawChar(int x, int y, int c){ return g.drawChar((char)c,x,y); }    
    
    public static void shutdown() {
        g.setColor(0x00FF5555);
        g.drawString("SISTEMA APAGADO. CERRANDO EN 2s...",380, 360);
        try { Thread.sleep(2000); } catch(Exception e){}
        System.exit(0);
    }

    public void runStartX() {
        int oldMx = 512, oldMy = 384;
        int lastBtn = 0;
        int lastSec = -1;

        // Render inicial
        desktop.paint();
        showDateTime();
        drawMouse(oldMx, oldMy);

        while (true) {
            Native.sys(Native.SYS_MARK_FRAME, 0, 0, 0, 0);

            int mx = System.readMouseEvent(0);
            int my = System.readMouseEvent(1);
            int btn = System.readMouseEvent(2);
			g.drawString("PosX: " + mx,900,80);
			g.drawString("PosY: " + my,900,90);

            if (mx < 0) mx = 0; if (mx > 1024) mx = 1024;
            if (my < 0) my = 0; if (my > 768) my = 768;

            boolean stateChanged = false;
			boolean hoverChanged = false;
            boolean mouseMoved = (mx != oldMx || my != oldMy);

            // Reloj dinámico
            int currentSec = System.readTime(0);
            if (currentSec != lastSec) {
                showDateTime();
                lastSec = currentSec;
            }

            // Arrastre de ventanas
            if (mouseMoved) {
                for (int i = 0; i < desktop.getWindowCount(); i++) {
                    JFrame win = desktop.getWindows()[i];
                    if (win.getIsDragging()) {
                        win.setX(mx - win.getDragOffsetX());
                        win.setY(my - win.getDragOffsetY());
                        stateChanged = true;
                    }
                }
            }

            // Clics y botones
            if (btn != lastBtn) {
                if (desktop.handleMouse(mx, my, btn)) {
                    stateChanged = true; 
                }
                if (btn == 0) {
                    for (int i = 0; i < desktop.getWindowCount(); i++) {
                        desktop.getWindows()[i].setIsDragging(false);
                    }
                }
            } else if(mouseMoved){
				// Si solo se mueve el ratón, evaluar Hover en los menús para resaltarlos
                if (desktop.getStartMenu().isVisible() && desktop.getStartMenu().handleMouse(mx, my, -1)){
					hoverChanged = true;
				}
                if (desktop.getContextMenu().isVisible() && desktop.getContextMenu().handleMouse(mx, my, -1)) {
					hoverChanged = true;
				}
            }

            // Teclado
            int ascii = Native.sys(Native.SYS_READ_KEYBOARD, 0, 0, 0, 0);
            if (ascii != 0) {
                if (ascii == 27) break;
                if (editor.isVisible() && editor.handleKey(ascii)) {
                    stateChanged = true;
                }
            }

            // MÁQUINA DE ESTADO DE RENDERIZADO
            if (stateChanged) {
                // Solo repintar toda la pantalla si se abrió un menú o se arrastró una ventana
                desktop.paint();
                showDateTime();
                drawMouse(mx, my);
            } else if(hoverChanged){
				// Solo repintar el menú afectado si el mouse se posa sobre un ítem
                if (desktop.getStartMenu().isVisible()){
					desktop.getStartMenu().paint(g);
				}
                if (desktop.getContextMenu().isVisible()){
					desktop.getContextMenu().paint(g);
				}
                drawMouse(mx, my);				
			} else if (mouseMoved) {
                // Si solo se movió el mouse, limpiamos anterior y lo dibujamos en nueva posición
                clearMouse(oldMx, oldMy);
                drawMouse(mx, my);
            }

            oldMx = mx; oldMy = my; lastBtn = btn;
            
            Native.sys(Native.SYS_RESET_FRAME, 0, 0, 0, 0);
            try { Thread.sleep(1); } catch(Exception e){}
        }
        g.clearScreen();
    }

    public static void clearMouse(int x, int y) {
        // Restaurar el fondo en el parche de 14x18 píxeles
        if (desktop.getBackgroundMode() == 0) {
            g.setColor(Color.TRANSPARENT); g.fillRect(x, y, 14, 18);
        } else if (desktop.getBackgroundMode() == 1) {
            for (int iy = y; iy < y + 18; iy += 2) {
                if (iy >= 726) break;
                int red = (iy * 255) / 726;
                g.setColor(((red / 2) << 16) | ((255 - red) / 2));
                g.fillRect(x, iy, 14, 2);
            }
        } else {
            g.setColor(Color.BLACK); g.fillRect(x, y, 14, 18);
        }

        // Restaurar las ventanas intersecadas
        for (int i = 0; i < desktop.getWindowCount(); i++) {
            JFrame win = desktop.getWindows()[i];
            if (win.isVisible() && !win.isMinimized()) {
                if (x < win.getX() + win.getWidth() && x + 14 > win.getX() && y < win.getY() + win.getHeight() && y + 18 > win.getY()) {
                    win.paint(g); 
                }
            }
        }
        
        // Restaurar Taskbar y reloj si el mouse estaba abajo
        if (y + 18 >= 726) {
            desktop.drawTaskbar();
			showDateTime();
        }
        
        // Restaurar Menú de Inicio con límites exactos dinámicos
		if (desktop.getStartMenu().isVisible()) {
			JPopupMenu st = desktop.getStartMenu();
			if (x < st.getX() + st.getWidth() && x + 14 > st.getX() && y < st.getY() + st.getHeight() && y + 18 > st.getY()) {
				st.paint(g);
			}
		}
		
		// Restaurar Menú Contextual
		if (desktop.getContextMenu().isVisible()) {
			JPopupMenu ctx = desktop.getContextMenu();
			if (x < ctx.getX() + ctx.getWidth() && x + 14 > ctx.getX() && y < ctx.getY() + ctx.getHeight() && y + 18 > ctx.getY()) {
				ctx.paint(g);
			}
		}
	}

    private static void showDateTime(){
        int hour = System.readTime(2), min = System.readTime(1), sec = System.readTime(0);
        int day = System.readTime(3), month = System.readTime(4), year = System.readTime(5);
        g.setColor(Color.LIGHT_GRAY); g.fillRect(880, 728, 144, 38);
        g.setColor(Color.BLACK);

        drawChar(910, 733, (hour / 10) + '0'); drawChar(920, 733, (hour % 10) + '0'); 
		drawChar(930, 733, ':');
        drawChar(940, 733, (min / 10) + '0'); drawChar(950, 733, (min % 10) + '0'); 
		drawChar(960, 733, ':');
        drawChar(970, 733, (sec / 10) + '0'); drawChar(980, 733, (sec % 10) + '0');
        
        drawChar(910, 750, (day / 10) + '0'); drawChar(920, 750, (day % 10) + '0');
		drawChar(930, 750, '/');
        drawChar(940, 750, (month / 10) + '0'); drawChar(950, 750, (month % 10) + '0');
		drawChar(960, 750, '/');
        drawChar(970, 750, (year / 10) + '0'); drawChar(980, 750, (year % 10) + '0');
    }

    public static void drawMouse(int x, int y) {			
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
