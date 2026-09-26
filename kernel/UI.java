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

public class UI {

    private static Graphics2D g;
    private static FileSystem fs;
    private static JExplorer explorer;
	private static JEditor editor;

    private static int contextX, contextY, backgroundMode;
    private static boolean showStartMenu, showContextMenu, showAbout;

    // Constructor
    public UI(FileSystem fileSys, Portapapeles clip){
        g = new Graphics2D();
        fs = fileSys;	
		editor = new JEditor(g, fs);
        explorer = new JExplorer(g, fs, clip, editor);
    }

    public static int readTime(int p) { 
		return Native.sys(Native.SYS_GET_TIME, p, 0, 0, 0); 
	}
    public static int drawChar(int x, int y, int c){ 
		return g.drawChar((char)c,x,y);
	}
    public static void drawString(int x, int y, String text){ 
		g.drawString(text, x, y);
	}
    public static int readMouseEvent(int e){ 
		return Native.sys(Native.SYS_READ_MOUSE,e,0,0,0);
	}
    public static int readKeyboardKey(int p) { 
		return Native.sys(Native.SYS_READ_KEYBOARD, p, 0, 0, 0); 
	}
    
    public static void shutdown() {
        g.setColor(0x00FF5555); // Rojo
        drawString(380, 360, "SISTEMA APAGADO. CERRANDO EN 2s...");
        Thread.sleep(2000); 
        System.exit(0);
    }

    // Aquí inicia el modo gráfico
    public void runStartX() {
        showStartMenu = false; 
		showContextMenu = false; 
		showAbout = false;
        contextX = 0; 
		contextY = 0; 
		backgroundMode = 0;

        redrawScreen();

        int oldMx = 512; 
		int oldMy = 384;
		int lastBtn = 0;
        drawMouse(oldMx, oldMy);

        // Ciclo principal del escritorio
        while (true) {
			drawBackground();
			
            // Cargar fecha y hora dinámicamente
            showDateTime();

            // Capturar eventos de mouse
            int mx = readMouseEvent(0);
			int my = readMouseEvent(1);
			int btn = readMouseEvent(2);

            // Vigilar colisión con inicio y fin de la pantalla
            if (mx < 0) mx = 0; if (mx > 1010) {
				mx = 1010;
			}
            if (my < 0) my = 0; if (my > 750) { 
				my = 750;
			}

            if (mx != oldMx || my != oldMy) {
                if (explorer.isDragging && explorer.windowOpen && !explorer.windowMinimized && !showAbout) {
                    explorer.winX = mx - explorer.dragOffsetX; 
                    explorer.winY = my - explorer.dragOffsetY;
                    if (explorer.winX < 0) {
						explorer.winX = 0; 
					}
                    if (explorer.winY < 0) { 
						explorer.winY = 0;
					}
                    if (explorer.winX + explorer.winW > 1024) {
						explorer.winX = 1024 - explorer.winW;
					}
                    if (explorer.winY + explorer.winH > 726) {
						explorer.winY = 726 - explorer.winH;
					}
                    redrawScreen();
                } else if (editor.isDragging && editor.windowOpen && !showAbout) {
                    editor.winX = mx - editor.dragOffsetX; 
                    editor.winY = my - editor.dragOffsetY;
                    if (editor.winX < 0) {
						editor.winX = 0; 
					}
                    if (editor.winY < 0) {
						editor.winY = 0;
					}
                    if (editor.winX + editor.winW > 1024){
						editor.winX = 1024 - editor.winW;
					}
                    if (editor.winY + editor.winH > 726) {
						editor.winY = 726 - editor.winH;
					}
                    redrawScreen();
                } else {
                    clearMouse(oldMx, oldMy);
                }
                drawMouse(mx, my);
                oldMx = mx;
				oldMy = my;
            }

            // Eventos con Clic Derecho
            if (btn == 2 && lastBtn != 2) { 
                showContextMenu = true; 
				showStartMenu = false;
                contextX = mx; 
				contextY = my;
                if (contextX > 820) {
					contextX = 820;
				}				
				if (contextY > 600) {
					contextY = 600;
				}
                redrawScreen(); 
				drawMouse(mx, my);
                lastBtn = btn;
            } 
            // Eventos con Clic Izquierdo
            else if (btn == 1 && lastBtn != 1) { 
                if (showContextMenu) {
                    if (mx >= contextX && mx <= contextX + 190) {
                        if (my >= contextY + 5 && my <= contextY + 25) {
							backgroundMode = 0;
						}
                        else if (my >= contextY + 25 && my <= contextY + 45) {
							backgroundMode = 1;
						}
						else if (my >= contextY + 45 && my <= contextY + 65){
							backgroundMode = 2;
						}
                        else if (my >= contextY + 65 && my <= contextY + 85) { 
							explorer.refreshView();
							explorer.windowOpen = true; 
							explorer.windowMinimized = false; 
						}
                        else if (my >= contextY + 85 && my <= contextY + 105){
							showAbout = true;
						}
                    }
                    showContextMenu = false; redrawScreen(); drawMouse(mx, my);
                } else if (showStartMenu) {
                    int menuY = 726 - 95;
                    if (mx >= 5 && mx <= 185 && my >= menuY && my <= 726) {
                        if (my >= menuY && my < menuY + 30) { 
							explorer.refreshView();
							explorer.windowOpen = true; 
							explorer.windowMinimized = false; 
						}
                        else if (my >= menuY + 30 && my < menuY + 60) { 
							explorer.windowOpen = false; 
							showAbout = false; 
						}
                        else if (my >= menuY + 60 && my <= 726) { 
							g.clearScreen(); 
							shutdown(); 
						}
                    }
                    showStartMenu = false; redrawScreen(); drawMouse(mx, my);
                } else if (showAbout) {
                    int ax = 262, ay = 250, aw = 500;
                    int btnX = ax + aw - 23, btnY = ay + 5;
                    if (mx >= btnX && mx <= btnX + 18 && my >= btnY && my <= btnY + 18) {
                        showAbout = false; redrawScreen(); drawMouse(mx, my);
                    }
                } else if (my >= 726) { // Taskbar
                    if (mx >= 5 && mx <= 85) { 
						showStartMenu = !showStartMenu;
						redrawScreen(); 
						drawMouse(mx, my); 
					}
                    else if (explorer.windowOpen && mx >= 95 && mx <= 235) { 
                        explorer.windowMinimized = !explorer.windowMinimized;
						redrawScreen(); 
						drawMouse(mx, my); 
                    }
                } else if (editor.windowOpen && mx >= editor.winX && mx <= editor.winX + editor.winW && my >= editor.winY && my <= editor.winY + editor.winH) {
                    if (editor.handleMouse(mx, my, true)) {
                        redrawScreen(); drawMouse(mx, my);
                    }
                } else if (explorer.windowOpen && !explorer.windowMinimized) {
					// Evitar que el Clic Derecho de UI sobrescriba el de JExplorer si estamos dentro de su ventana
					if (mx >= explorer.winX && mx <= explorer.winX + explorer.winW && 
						my >= explorer.winY && my <= explorer.winY + explorer.winH) {
						
						if (explorer.handleMouse(mx, my, true, btn)) {
							redrawScreen(); 
							drawMouse(mx, my);
							if (btn == 2) {
								showContextMenu = false; // Suprimir el menú del escritorio
							}
						}
					}
				}
                lastBtn = 1;
            } else if (btn == 0) {
                explorer.isDragging = false; 
				editor.isDragging = false;
				lastBtn = 0;
            }

            int ascii = readKeyboardKey(0);
            if (ascii != 0) {
                if (ascii == 27) { break;}
                if (editor.windowOpen) {
                    editor.handleKey(ascii);
                    editor.draw(); 
                    drawMouse(mx, my);
                }
            }
            Thread.sleep(1);
        }
        g.clearScreen();
    }

    public static void redrawScreen() {
        drawBackground();
        explorer.draw();
		editor.draw();
        drawTaskbar();
        drawStartMenu();
        drawContextMenu();
        drawAboutWindow();
    }

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

    public static void drawTaskbar() {
        int taskbarY = 726;
        g.setColor(0x00C0C0C0); g.fillRect(0, taskbarY, 1024, 42);
        g.setColor(0x00FFFFFF); g.fillRect(0, taskbarY, 1024, 2);

        g.setColor(showStartMenu ? 0x00808080 : 0x00008000);
        g.fillRect(5, taskbarY + 4, 80, 32);
        g.setColor(0x00FFFFFF); drawString(22, taskbarY + 24, "INICIO");

        if (explorer.windowOpen) {
            g.setColor(explorer.windowMinimized ? 0x00A0A0A0 : 0x00E0E0E0);
            g.fillRect(95, taskbarY + 4, 140, 32);
            g.setColor(0x00000000); drawString(110, taskbarY + 24, "JExplorer");
        }
    }

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

    public static void drawContextMenu() {
        if (!showContextMenu) {return;}
        g.setColor(0x00F0F0F0); 
		g.fillRect(contextX, contextY, 190, 115);
        g.setColor(0x00000000); 
		g.drawRect(contextX, contextY, 190, 115);
        drawString(contextX + 15, contextY + 20, "Fondo Solido");
        drawString(contextX + 15, contextY + 40, "Fondo Gradiente");
        drawString(contextX + 15, contextY + 60, "Fondo Fractal");
        drawString(contextX + 15, contextY + 80, "Abrir Explorador");			
        drawString(contextX + 15, contextY + 100, "Acerca de JVMOS");
    }

    public static void drawAboutWindow() {
        if (!showAbout) {return;}
        int ax = 262, ay = 250, aw = 500, ah = 220;

        g.setColor(0x00E0E0E0); 
		g.fillRect(ax, ay, aw, ah);
        g.setColor(0x001F4E5B); 
		g.fillRect(ax + 3, ay + 3, aw - 6, 24);
        g.setColor(0x00FFFFFF); 
		drawString(ax + 10, ay + 10, "Acerca de JVMOS");

        int btnX = ax + aw - 23, btnY = ay + 5;
        g.setColor(0x00FF0000); 
		g.fillRect(btnX, btnY, 18, 18);
        g.setColor(0x00FFFFFF); 
		drawString(btnX + 5, btnY + 5, "X");

        g.setColor(0x00000000);
        drawString(ax + 170, ay + 60, "JVMOS - Version 1.0");
        drawString(ax + 20, ay + 80, "Sistema operativo escrito en ASM y Java");
        drawString(ax + 20, ay + 110, "Hecho por: Allan Ayes Ramirez (30/08/2026)");
        drawString(ax + 20, ay + 130, "GitHub: aayes89");
        drawString(ax + 20, ay + 160, "Memoria RAM: 128 MB (Estatica BIOS)");
        drawString(ax + 20, ay + 180, "Video: VBE VESA 1024x768 @ 32bpp");
    }

    private static void showDateTime(){
        int hour = readTime(2), min = readTime(1), sec = readTime(0);
        int day = readTime(3), month = readTime(4), year = readTime(5);
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

    public static void clearMouse(int x, int y) {
        if (backgroundMode == 0) {
            g.setColor(0x00000055); 
			g.fillRect(x, y, 14, 18);
        } else if (backgroundMode == 1) {
            for (int iy = y; iy < y + 18; iy += 2) {
                if (iy >= 726) {break;}
                int red = (iy * 255) / 726;
                g.setColor(((red / 2) << 16) | ((255 - red) / 2));
                g.fillRect(x, iy, 14, 2);
            }
        } else if (backgroundMode == 2) {
            for (int px = (x / 4) * 4; px < x + 14; px += 4) {
                for (int py = (y / 4) * 4; py < y + 18; py += 4) {
					if (py >= 726) {continue;}
                    int x0 = ((px - 600) * 4096) / 300; 
                    int y0 = ((py - 364) * 4096) / 300;
                    int cx = 0, cy = 0, iter = 0;
                    while (iter < 24) {
                        int nx2 = (cx * cx) >> 12;
                        int ny2 = (cy * cy) >> 12;
                        if (nx2 + ny2 > 16384){ break;}
                        int xtemp = nx2 - ny2 + x0;
                        cy = ((2 * cx * cy) >> 12) + y0;
                        cx = xtemp; iter++;
                    }
                    if (iter < 24) {
						g.setColor(0x000000FF | (iter * 10 << 8) | (iter * 5));
					}
                    else {
						g.setColor(0x00000000);
					}
                    g.fillRect(px, py, 4, 4);
                }
            }
        }

        // Llamadas de repintado localizadas para evitar Flickering
        if (explorer.windowOpen && !explorer.windowMinimized && x < explorer.winX + explorer.winW && x + 14 > explorer.winX && y < explorer.winY + explorer.winH && y + 18 > explorer.winY) {
			explorer.draw();
		}
		if (editor.windowOpen && x < editor.winX + editor.winW && x + 14 > editor.winX && y < editor.winY + editor.winH && y + 18 > editor.winY) {
			editor.draw(); 
		}
        if (showAbout && x < 262 + 500 && x + 14 > 262 && y < 250 + 220 && y + 18 > 250) {
			drawAboutWindow();
		}
        if (y + 18 >= 726) {
			drawTaskbar();
		}
        if (showStartMenu && x < 185 && y + 18 > 631) {
			drawStartMenu();
		}
        if (showContextMenu && x < contextX + 190 && x + 14 > contextX && y < contextY + 115 && y + 18 > contextY) {
			drawContextMenu();
		}
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
