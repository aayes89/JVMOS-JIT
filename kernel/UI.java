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
package kernel;

import java.awt.Graphics2D;
import java.awt.Color;
import java.lang.Thread;
import java.lang.System;
import kernel.vfs.Node;


// Versión antigua del UI, la reemplazaré cuando acabe con los driver de red y el sistema de archivo
public class UI {

	private static Graphics2D g;
	private static int winX, winY, winW, winH, contextX, contextY, backgroundMode, dragOffsetX, dragOffsetY;
	private static boolean windowOpen, windowMinimized, isDragging, showStartMenu, showContextMenu, showAbout;
	private static Node root, currentDir;

	// Constructor
	public UI(){
		g = new Graphics2D();
	}

	// Me daba flojera cambiar cada método, así que hice unos cambios para que se ajustara el código antiguo
	public static int readTime(int p) { return Native.sys(Native.SYS_GET_TIME, p, 0, 0, 0); }
	public static int drawChar(int x, int y, int c){ return g.drawChar((char)c,x,y);}
	public static void drawString(int x, int y, String text){ g.drawString(text, x, y);}
	public static int readMouseEvent(int e){ return Native.sys(Native.SYS_READ_MOUSE,e,0,0,0);}
	public static int readKeyboardKey(int p) { return Native.sys(Native.SYS_READ_KEYBOARD, p, 0, 0, 0); }
	public static void shutdown() {
        g.setColor(0x00FF5555); // Rojo
        drawString(380, 360, "SISTEMA APAGADO. CERRANDO EN 2s...");
        Thread.sleep(2000); 
        System.exit(0);
    }

    // Aquí inicia el módo gráfico
	public static void runStartX() {
        // Inicialización Explicita (Vital en Baremetal)
        winX = 150; winY = 60; winW = 720; winH = 460;
        windowOpen = true; windowMinimized = false;
        isDragging = false; dragOffsetX = 0; dragOffsetY = 0;
        showStartMenu = false; showContextMenu = false; showAbout = false;
        contextX = 0; contextY = 0; backgroundMode = 0;

        redrawScreen();

        int oldMx = 512, oldMy = 384, lastBtn = 0;
        drawMouse(oldMx, oldMy);

        // Ciclo principal del escritorio
        while (true) {
            // Cargar fecha y hora dinámicamente
            showDateTime();

            // Capturar eventos de mouse
            int mx = readMouseEvent(0), my = readMouseEvent(1), btn = readMouseEvent(2);

            // Vigilar colisión con inicio y fin de la pantalla (evitar desbordamiento)
            if (mx < 0) mx = 0; if (mx > 1010) mx = 1010;
            if (my < 0) my = 0; if (my > 750) my = 750;

            if (mx != oldMx || my != oldMy) {
                if (isDragging && windowOpen && !windowMinimized && !showAbout) {
                    winX = mx - dragOffsetX; winY = my - dragOffsetY;
                    if (winX < 0) winX = 0; if (winY < 0) winY = 0;
                    if (winX + winW > 1024) winX = 1024 - winW;
                    if (winY + winH > 726) winY = 726 - winH;
                    redrawScreen();
                } else {
                    clearMouse(oldMx, oldMy);
                }
                drawMouse(mx, my);
                oldMx = mx; oldMy = my;
            }

            // Eventos con Clic Derecho
            if (btn == 2 && lastBtn != 2) { 
                showContextMenu = true; showStartMenu = false;
                contextX = mx; contextY = my;
                if (contextX > 820) contextX = 820;
                if (contextY > 600) contextY = 600;
                redrawScreen(); drawMouse(mx, my);
                lastBtn = btn;
            } // Eventos con Clic Izquierdo
            else if (btn == 1 && lastBtn != 1) { 
            	if (showContextMenu) {
                    if (mx >= contextX && mx <= contextX + 190) {
                        if (my >= contextY + 5 && my <= contextY + 25) backgroundMode = 0;
                        else if (my >= contextY + 25 && my <= contextY + 45) backgroundMode = 1;
                        else if (my >= contextY + 45 && my <= contextY + 65) backgroundMode = 2;
                        else if (my >= contextY + 65 && my <= contextY + 85) { windowOpen = true; windowMinimized = false; }
                        else if (my >= contextY + 85 && my <= contextY + 105) showAbout = true;
                    }
                    showContextMenu = false; redrawScreen(); drawMouse(mx, my);
                } else if (showStartMenu) {
                    int menuY = 726 - 95;
                    if (mx >= 5 && mx <= 185 && my >= menuY && my <= 726) {
                        if (my >= menuY && my < menuY + 30) { windowOpen = true; windowMinimized = false; }
                        else if (my >= menuY + 30 && my < menuY + 60) { windowOpen = false; showAbout = false; }
                        else if (my >= menuY + 60 && my <= 726) { g.clearScreen(); shutdown(); }
                    }
                    showStartMenu = false; redrawScreen(); drawMouse(mx, my);
                } else if (showAbout) {
                    int ax = 262, ay = 250, aw = 500;
                    int btnX = ax + aw - 23, btnY = ay + 5;
                    if (mx >= btnX && mx <= btnX + 18 && my >= btnY && my <= btnY + 18) {
                        showAbout = false; redrawScreen(); drawMouse(mx, my);
                    }
                } else if (my >= 726) { // Taskbar
                    if (mx >= 5 && mx <= 85) { showStartMenu = !showStartMenu; redrawScreen(); drawMouse(mx, my); }
                    else if (windowOpen && mx >= 95 && mx <= 235) { windowMinimized = !windowMinimized; redrawScreen(); drawMouse(mx, my); }
                } else if (windowOpen && !windowMinimized) {
                    int btnX = winX + winW - 23, btnY = winY + 5;
                    if (mx >= btnX && mx <= btnX + 18 && my >= btnY && my <= btnY + 18) {
                        windowOpen = false; redrawScreen(); drawMouse(mx, my);
                    } else if (mx >= winX && mx <= winX + winW - 30 && my >= winY && my <= winY + 27) {
                        isDragging = true; dragOffsetX = mx - winX; dragOffsetY = my - winY;
                    } else if (mx >= winX + 10 && mx <= winX + 190 && my >= winY + 35 && my <= winY + winH - 45) { // Desplazamiento en el explorador
                        int nodeY = winY + 55;
                        if (my >= nodeY - 10 && my <= nodeY + 10) { currentDir = root; redrawScreen(); drawMouse(mx, my); }
                        nodeY += 25;
                        for (int i = 0; i < root.childCount; i++) {
                            Node child = root.children[i];
                            if (child.isDir) {
                                if (my >= nodeY - 10 && my <= nodeY + 10) { currentDir = child; redrawScreen(); drawMouse(mx, my); }
                                nodeY += 20;
                            }
                        }
                    } else if (mx >= winX + 195 && mx <= winX + winW - 10 && my >= winY + 35 && my <= winY + winH - 45) { // Desplazamiento en el explorador
                        int iconX = winX + 215, iconY = winY + 55;
                        if (currentDir.parent != null) {
                            if (mx >= iconX && mx <= iconX + 60 && my >= iconY && my <= iconY + 50) { currentDir = currentDir.parent; redrawScreen(); drawMouse(mx, my); }
                            iconX += 90;
                        }
                        for (int i = 0; i < currentDir.childCount; i++) {
                            Node child = currentDir.children[i];
                            if (child != null && child.isDir) {
                                if (mx >= iconX && mx <= iconX + 60 && my >= iconY && my <= iconY + 50) { currentDir = child; redrawScreen(); drawMouse(mx, my); }
                                iconX += 90;
                                if (iconX > winX + winW - 80) { iconX = winX + 215; iconY += 60; }
                            }
                        }
                    }
                }
                lastBtn = 1;
            } else if (btn == 0) {
                isDragging = false; lastBtn = 0;
            }

            if (readKeyboardKey(0) == 27) break;
            Thread.sleep(1);
        }
        g.clearScreen();
    }

    // MÉTODOS DE DIBUJADO DE UI    
    // Pintar todo el UI (repintar)
    public static void redrawScreen() {
        drawBackground();
        drawWindow();
        drawTaskbar();
        drawStartMenu();
        drawContextMenu();
        drawAboutWindow();
    }

    // Fondos de pantalla (color azul entero, gradiente y mandelbrot)
    public static void drawBackground() {
        if (backgroundMode == 0) {
            g.setColor(0x00000055); g.fillRect(0, 0, 1024, 726);
        } else if (backgroundMode == 1) {
            for (int y = 0; y < 726; y += 8) {
                int red = (y * 255) / 726;
                g.setColor(((red / 2) << 16) | ((255 - red) / 2));
                g.fillRect(0, y, 1024, 8);
            }
        } else if (backgroundMode == 2) {
            for (int px = 0; px < 1024; px += 4) {
                for (int py = 0; py < 726; py += 4) {
                    int x0 = ((px - 600) * 4096) / 300; 
                    int y0 = ((py - 364) * 4096) / 300;
                    int cx = 0, cy = 0, iter = 0;
                    while (iter < 24) {
                        int nx2 = (cx * cx) >> 12;
                        int ny2 = (cy * cy) >> 12;
                        if (nx2 + ny2 > 16384) break;
                        int xtemp = nx2 - ny2 + x0;
                        cy = ((2 * cx * cy) >> 12) + y0;
                        cx = xtemp;
                        iter++;
                    }
                    if (iter < 24) g.setColor(0x000000FF | (iter * 10 << 8) | (iter * 5));
                    else g.setColor(0x00000000);
                    g.fillRect(px, py, 4, 4);
                }
            }
        }
    }

    // Pintar ventana de explorador de archivos (versión antigua - no funciona, sólo es para mostrar algo)
    public static void drawWindow() {
        if (!windowOpen || windowMinimized) return;

        g.setColor(0x00C0C0C0); g.fillRect(winX, winY, winW, winH);
        g.setColor(0x00000080); g.fillRect(winX + 3, winY + 3, winW - 6, 24);
        g.setColor(0x00FFFFFF); drawString(winX + 10, winY + 20, "JExplorer - "); drawString(winX + 130, winY + 20, currentDir.name);
        g.setColor(0x00FFFFFF); drawString(winX + 10, winY + 10, "JExplorer - "); drawString(winX + 130, winY + 10, currentDir.name); // Root (raiz)

        int btnX = winX + winW - 23, btnY = winY + 5;
        g.setColor(0x00FF0000); g.fillRect(btnX, btnY, 18, 18);
        g.setColor(0x00FFFFFF); drawString(btnX + 5, btnY + 14, "X");
        g.setColor(0x00FFFFFF); drawString(btnX + 5, btnY + 5, "X");

        int treeX = winX + 10, treeY = winY + 35, treeW = 180, treeH = winH - 45;
        int viewX = winX + 195, viewY = winY + 35, viewW = winW - 205, viewH = winH - 45;

        g.setColor(0x00E0E0E0); g.fillRect(treeX, treeY, treeW, treeH);
        g.setColor(0x00FFFFFF); g.fillRect(viewX, viewY, viewW, viewH);

        g.setColor(0x00000000); int nodeY = treeY + 20;
        drawString(treeX + 10, nodeY, "[-] / (Root)"); nodeY += 25;

        for (int i = 0; i < root.childCount; i++) {
            Node child = root.children[i];
            if (child.isDir) {
                if (child == currentDir) {
                    g.setColor(0x00000080); g.fillRect(treeX + 20, nodeY - 14, 150, 18); g.setColor(0x00FFFFFF);
                } else g.setColor(0x00000000);
                drawString(treeX + 25, nodeY, "+-- "); drawString(treeX + 55, nodeY, child.name);
                nodeY += 20;
            }
        }

        int iconX = viewX + 20, iconY = viewY + 20;
        if (currentDir.parent != null) {
            g.setColor(0x00808080); g.fillRect(iconX, iconY, 32, 22);
            g.setColor(0x00000000); drawString(iconX, iconY + 38, ".. (Atras)");
            iconX += 90;
        }

        for (int i = 0; i < currentDir.childCount; i++) {
            Node child = currentDir.children[i];
            if (child != null) {
                if (child.isDir) {
                    g.setColor(0x00F0C000); g.fillRect(iconX, iconY, 32, 22); g.fillRect(iconX, iconY - 4, 12, 4);
                } else {
                    g.setColor(0x00A0A0A0); g.fillRect(iconX, iconY, 20, 26);
                }
                g.setColor(0x00000000); drawString(iconX, iconY + 38, child.name);

                iconX += 90;
                if (iconX > viewX + viewW - 80) { iconX = viewX + 20; iconY += 60; }
            }
        }
    }

    // Pintar barra de tareas 
    public static void drawTaskbar() {
        int taskbarY = 726;
        g.setColor(0x00C0C0C0); g.fillRect(0, taskbarY, 1024, 42);
        g.setColor(0x00FFFFFF); g.fillRect(0, taskbarY, 1024, 2);

        g.setColor(showStartMenu ? 0x00808080 : 0x00008000);
        g.fillRect(5, taskbarY + 4, 80, 32);
        g.setColor(0x00FFFFFF); drawString(22, taskbarY + 24, "INICIO");

        if (windowOpen) {
            g.setColor(windowMinimized ? 0x00A0A0A0 : 0x00E0E0E0);
            g.fillRect(95, taskbarY + 4, 140, 32);
            g.setColor(0x00000000); drawString(110, taskbarY + 24, "JExplorer");
        }
    }

    // Pintar elementos de la barra de menú
    public static void drawStartMenu() {
        if (!showStartMenu) return;
        int menuH = 95, menuY = 726 - menuH;
        g.setColor(0x00C0C0C0); g.fillRect(5, menuY, 180, menuH);
        g.setColor(0x00000080); g.fillRect(5, menuY, 25, menuH);
        g.setColor(0x00000000);
        drawString(35, menuY + 25, "Abrir JExplorer");
        drawString(35, menuY + 55, "Cerrar Ventanas");
        drawString(35, menuY + 82, "Apagar Equipo");
    }

    // Pintar elementos del menú de clic derecho
    public static void drawContextMenu() {
        if (!showContextMenu) return;
        g.setColor(0x00F0F0F0); g.fillRect(contextX, contextY, 190, 115);
        g.setColor(0x00000000); g.drawRect(contextX, contextY, 190, 115);
        drawString(contextX + 15, contextY + 20, "Fondo Solido");
        drawString(contextX + 15, contextY + 40, "Fondo Gradiente");
        drawString(contextX + 15, contextY + 60, "Fondo Fractal");
        drawString(contextX + 15, contextY + 80, "Abrir Explorador");
        drawString(contextX + 15, contextY + 100, "Acerca de JVMOS");
    }
    // Pintar Acerca de JVMOS-JIT
    public static void drawAboutWindow() {
        if (!showAbout) return;
        int ax = 262, ay = 250, aw = 500, ah = 220;

        g.setColor(0x00E0E0E0); g.fillRect(ax, ay, aw, ah);
        g.setColor(0x001F4E5B); g.fillRect(ax + 3, ay + 3, aw - 6, 24);
        g.setColor(0x00FFFFFF); drawString(ax + 10, ay + 10, "Acerca de JVMOS");

        int btnX = ax + aw - 23, btnY = ay + 5;
        g.setColor(0x00FF0000); g.fillRect(btnX, btnY, 18, 18);
        g.setColor(0x00FFFFFF); drawString(btnX + 5, btnY + 5, "X");

        g.setColor(0x00000000);
        drawString(ax + 170, ay + 60, "JVMOS - Version 1.0");
        drawString(ax + 20, ay + 80, "Sistema operativo escrito en ASM y Java");
        drawString(ax + 20, ay + 110, "Hecho por: Allan Ayes Ramirez (30/08/2026)");
        drawString(ax + 20, ay + 130, "GitHub: aayes89");
        drawString(ax + 20, ay + 160, "Memoria RAM: 128 MB (Estatica BIOS)");
        drawString(ax + 20, ay + 180, "Video: VBE VESA 1024x768 @ 32bpp");
    }
    private static void showDateTime(){
    	// Actualizar reloj dinámico y fecha en la Barra de Tareas
        int hour = readTime(2), min = readTime(1), sec = readTime(0);
		int day = readTime(3), month = readTime(4), year = readTime(5);
        g.setColor(0x00C0C0C0); g.fillRect(880, 728, 144, 38);
        g.setColor(0x00000000);

        // Hora Superior
        drawChar(910, 733, (hour / 10) + '0'); drawChar(920, 733, (hour % 10) + '0'); drawChar(930, 733, ':');
        drawChar(940, 733, (min / 10) + '0');  drawChar(950, 733, (min % 10) + '0');  drawChar(960, 733, ':');
        drawChar(970, 733, (sec / 10) + '0');  drawChar(980, 733, (sec % 10) + '0');
        // Fecha Inferior
        drawChar(890, 750, (day / 10) + '0'); drawChar(900, 750, (day % 10) + '0'); drawChar(910, 750, '/');
        drawChar(920, 750, (month / 10) + '0'); drawChar(930, 750, (month % 10) + '0'); drawString(940, 750, "/20");
        drawChar(970, 750, (year / 10) + '0'); drawChar(980, 750, (year % 10) + '0');
    }

    // Limpiar el rastro del cursor en la pantalla (versión antigua)
    public static void clearMouse(int x, int y) {
        if (backgroundMode == 0) {
            g.setColor(0x00000055); g.fillRect(x, y, 14, 18);
        } else if (backgroundMode == 1) {
            for (int iy = y; iy < y + 18; iy += 2) {
                if (iy >= 726) break;
                int red = (iy * 255) / 726;
                g.setColor(((red / 2) << 16) | ((255 - red) / 2));
                g.fillRect(x, iy, 14, 2);
            }
        } else if (backgroundMode == 2) {
            for (int px = (x / 4) * 4; px < x + 14; px += 4) {
                for (int py = (y / 4) * 4; py < y + 18; py += 4) {
                    if (py >= 726) continue;
                    int x0 = ((px - 600) * 4096) / 300; 
                    int y0 = ((py - 364) * 4096) / 300;
                    int cx = 0, cy = 0, iter = 0;
                    while (iter < 24) {
                        int nx2 = (cx * cx) >> 12;
                        int ny2 = (cy * cy) >> 12;
                        if (nx2 + ny2 > 16384) break;
                        int xtemp = nx2 - ny2 + x0;
                        cy = ((2 * cx * cy) >> 12) + y0;
                        cx = xtemp; iter++;
                    }
                    if (iter < 24) g.setColor(0x000000FF | (iter * 10 << 8) | (iter * 5));
                    else g.setColor(0x00000000);
                    g.fillRect(px, py, 4, 4);
                }
            }
        }

        if (windowOpen && !windowMinimized && x < winX + winW && x + 14 > winX && y < winY + winH && y + 18 > winY) drawWindow();
        if (showAbout && x < 262 + 500 && x + 14 > 262 && y < 250 + 220 && y + 18 > 250) drawAboutWindow();
        if (y + 18 >= 726) drawTaskbar();
        if (showStartMenu && x < 185 && y + 18 > 631) drawStartMenu();
        if (showContextMenu && x < contextX + 190 && x + 14 > contextX && y < contextY + 115 && y + 18 > contextY) drawContextMenu();
    }

    // Pintar el cursor en pantalla (versión antigua - actualmente un rectángulo simple)
    public static void drawMouse(int x, int y) {
        g.setColor(0x00000000);
        for (int i = 0; i < 12; i++) g.fillRect(x, y + i, i + 2, 1);
        g.fillRect(x + 2, y + 12, 4, 5);
        g.setColor(0x00FFFFFF);
        for (int i = 1; i < 10; i++) g.fillRect(x + 1, y + i, i, 1);
        g.fillRect(x + 3, y + 10, 2, 6);
    }
}
