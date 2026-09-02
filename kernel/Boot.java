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

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Toolkit;
import java.util.Calendar;
import java.io.PrintStream;
import java.lang.Thread;
import java.lang.Math;
import java.math.BigInteger;

public class Boot {
    
    // ESTRUCTURA DEL SISTEMA DE ARCHIVOS (VFS)    
    public static class Node {
        public String name;
        public int[] customName;
        public int customLen;
        public boolean isDir;
        public Node[] children;
        public int childCount;
        public Node parent;
    }

    private static Node root, currentDir, selectedNode, clipboardNode, fileMenuTarget;

    // MICRO-RT: Gráficos y Paleta Cacheada
    private static Graphics2D g;
    private static Color C_BLACK, C_WHITE, C_RED, C_GREEN, C_BLUE, C_YELLOW;
    private static Color C_GRAY, C_LIGHT_GRAY, C_DARK_GRAY, C_TRANSPARENT, C_CYAN;
    private static Color C_WIN_BG, C_WIN_TITLE, C_SEL, C_FOLDER, C_FILE;

    // ESTADO GLOBAL DEL ESCRITORIO
    private static int winX, winY, winW, winH;
    private static boolean windowOpen, windowMinimized, isDragging, isNaming;
    private static int dragOffsetX, dragOffsetY, lastClickTick, lastUIKey;
    private static boolean showStartMenu, showContextMenu, showAbout, showFileMenu, namingIsDir;
    private static int contextX, contextY, fileMenuX, fileMenuY, backgroundMode, newNameLen;
    private static int[] newNameBuf = new int[16];

    public static void main(String[] args) {
        java.lang.System.out = new PrintStream();
        java.lang.System.out.println("[Boot] Inicializando subsistemas Micro-rt...");
        
        g = new Graphics2D();
        initColors();

        initKeyboard();
        dramaticBIOS();
        shell();

        int cursorX = 85, cursorY = 80, lastKey = 0, cmdLen = 0;
        int[] cmdBuffer = new int[16];
        showCursor(cursorY);

        while (true) {
            int asciiChar = Native.sys(Native.SYS_READ_KEYBOARD, 0, 0, 0, 0);

            if (asciiChar != 0 && asciiChar != lastKey) {
                if (asciiChar == 13) {
                    cursorY += 25;
                    
                    if (cmdLen == 6 && cmdBuffer[0] == 's' && cmdBuffer[1] == 't' && cmdBuffer[2] == 'a' && cmdBuffer[3] == 'r' && cmdBuffer[4] == 't' && cmdBuffer[5] == 'x') {
                        java.lang.System.out.println("[GUI] Arrancando entorno de escritorio...");
                        runStartX();
                        cursorY = 40;
                    } else if (cmdLen == 4 && cmdBuffer[0] == 'c' && cmdBuffer[1] == 'u' && cmdBuffer[2] == 'b' && cmdBuffer[3] == 'e') {
                        java.lang.System.out.println("[3D Engine] Arrancando renderizador JIT-Baremetal...");
                        runCube3D();
                        clearScreen();
                        cursorY = 40;
                    } else if (cmdLen == 3 && cmdBuffer[0] == 'v' && cmdBuffer[1] == 'e' && cmdBuffer[2] == 'r') {
                        g.setColor(C_GREEN);
                        g.drawString("JVMOS Kernel v2.5 (Baremetal Java x86)", 20, cursorY); cursorY += 25;
                        g.drawString("Micro-rt Integrado - Slam 2026", 20, cursorY); cursorY += 25;
                    } else if (cmdLen == 4 && cmdBuffer[0] == 't' && cmdBuffer[1] == 'i' && cmdBuffer[2] == 'm' && cmdBuffer[3] == 'e') {
                        showTime(cursorY); cursorY += 25;
                    } else if (cmdLen == 4 && cmdBuffer[0] == 'd' && cmdBuffer[1] == 'a' && cmdBuffer[2] == 't' && cmdBuffer[3] == 'e') {
                        showDate(cursorY); cursorY += 25;
                    } else if ((cmdLen == 5 && cmdBuffer[0] == 'c' && cmdBuffer[1] == 'l' && cmdBuffer[2] == 'e' && cmdBuffer[3] == 'a' && cmdBuffer[4] == 'r') ||
                               (cmdLen == 3 && cmdBuffer[0] == 'c' && cmdBuffer[1] == 'l' && cmdBuffer[2] == 's')) {
                        clearScreen();
                        cursorY = 40;
                    } else if (cmdLen == 4 && cmdBuffer[0] == 'h' && cmdBuffer[1] == 'e' && cmdBuffer[2] == 'l' && cmdBuffer[3] == 'p') {
                        g.setColor(C_GREEN);
                        g.drawString("COMANDOS: help | startx | cube | clear | cls | ver | time | date | test | exit", 20, cursorY);
                        cursorY += 25;
                    } else if(cmdLen == 4 && cmdBuffer[0] == 't' && cmdBuffer[1] == 'e' && cmdBuffer[2] == 's' && cmdBuffer[3] == 't'){
                        runSystemDiagnostics();
                    }else if (cmdLen == 4 && cmdBuffer[0] == 'e' && cmdBuffer[1] == 'x' && cmdBuffer[2] == 'i' && cmdBuffer[3] == 't') {
                        shutdown();
                    } else if (cmdLen > 0) {
                        g.setColor(C_RED);
                        g.drawString("Error: Comando no reconocido.", 20, cursorY);
                        Toolkit.getDefaultToolkit().beep(800); Thread.sleep(150); Toolkit.getDefaultToolkit().beep(0);
                        cursorY += 25;
                    }

                    for (int i = 0; i < cmdLen; i++) cmdBuffer[i] = 0;
                    cmdLen = 0;

                    if (cursorY > 700) { clearScreen(); cursorY = 40; }
                    showCursor(cursorY);
                    cursorX = 85;
                } else if (asciiChar == 8) {
                    if (cmdLen > 0 && cursorX > 85) {
                        cmdLen--; cmdBuffer[cmdLen] = 0; cursorX -= 10;
                        g.setColor(C_BLACK); g.fillRect(cursorX, cursorY, 12, 20);
                    }
                } else if (asciiChar >= 32 && asciiChar <= 165) {
                    if (cmdLen < 15) {
                        cmdBuffer[cmdLen] = asciiChar; cmdLen++;
                        g.setColor(C_WHITE); g.drawChar((char)asciiChar, cursorX, cursorY);
                        cursorX += 10;
                    }
                }
                lastKey = asciiChar;
            } else if (asciiChar == 0) {
                lastKey = 0;
            }
            Thread.sleep(1);
        }
    }

    public static void initColors() {
        C_BLACK = new Color(0x00000000); C_WHITE = new Color(0x00FFFFFF);
        C_RED = new Color(0x00FF0000); C_GREEN = new Color(0x0000FF00);
        C_BLUE = new Color(0x000000FF); C_YELLOW = new Color(0x00FFFF00);
        C_CYAN = new Color(0x0000FFFF);
        C_GRAY = new Color(0x00808080); C_LIGHT_GRAY = new Color(0x00C0C0C0);
        C_DARK_GRAY = new Color(0x00404040); C_TRANSPARENT = new Color(0x00000055);
        C_WIN_BG = new Color(0x00E0E0E0); C_WIN_TITLE = new Color(0x00000080);
        C_SEL = new Color(0x00D0D0FF); C_FOLDER = new Color(0x00F0C000); C_FILE = new Color(0x00A0A0A0);
    }

    // =========================================================================
    // MOTOR 3D BAREMETAL (Cubo Giratorio - Matemática Entera)
    // =========================================================================
    public static void runCube3D() {
        clearScreen();
        g.setColor(C_CYAN);
        g.drawString("Baremetal 3D Engine (JIT Integer Math) - Presiona ESC para salir", 20, 20);

        // Geometría del cubo (Escala local +- 50)
        int[] cubeX = {-50, 50, 50, -50, -50, 50, 50, -50};
        int[] cubeY = {-50, -50, 50, 50, -50, -50, 50, 50};
        int[] cubeZ = {-50, -50, -50, -50, 50, 50, 50, 50};

        // Conexiones de las líneas (Aristas)
        int[] edges = {
            0,1, 1,2, 2,3, 3,0, // Cara Frontal
            4,5, 5,6, 6,7, 7,4, // Cara Trasera
            0,4, 1,5, 2,6, 3,7  // Conectores de profundidad
        };

        // Arrays de proyección (Evitan GC)
        int[] projX = new int[8];
        int[] projY = new int[8];
        int[] oldProjX = new int[8];
        int[] oldProjY = new int[8];

        int angleX = 0, angleY = 0, angleZ = 0;

        while (true) {
            // 1. Borrar solo las líneas viejas (Hack Zero-Flicker)
            g.setColor(C_BLACK);
            for (int i = 0; i < 12; i++) {
                int p1 = edges[i * 2], p2 = edges[i * 2 + 1];
                if (oldProjX[p1] != 0) {
                    g.drawLine(oldProjX[p1], oldProjY[p1], oldProjX[p2], oldProjY[p2]);
                }
            }

            // 2. Pre-calcular trigonometría entera (Escalada a 256)
            int sinX = Math.sin(angleX), cosX = Math.cos(angleX);
            int sinY = Math.sin(angleY), cosY = Math.cos(angleY);
            int sinZ = Math.sin(angleZ), cosZ = Math.cos(angleZ);

            // 3. Proyección 3D Pipeline
            for (int i = 0; i < 8; i++) {
                int x = cubeX[i], y = cubeY[i], z = cubeZ[i];

                // Rotación X
                int xy = (y * cosX - z * sinX) / 256;
                int xz = (y * sinX + z * cosX) / 256;
                y = xy; z = xz;

                // Rotación Y
                int yx = (x * cosY + z * sinY) / 256;
                int yz = (-x * sinY + z * cosY) / 256;
                x = yx; z = yz;

                // Rotación Z
                int zx = (x * cosZ - y * sinZ) / 256;
                int zy = (x * sinZ + y * cosZ) / 256;
                x = zx; y = zy;

                // Proyección Perspectiva al Centro de Pantalla (1024x768)
                int z_shifted = z + 150; // Alejar cámara
                projX[i] = (x * 400) / z_shifted + 512;
                projY[i] = (y * 400) / z_shifted + 384;
            }

            // 4. Dibujar nuevo frame en verde hacker
            g.setColor(C_GREEN);
            for (int i = 0; i < 12; i++) {
                int p1 = edges[i * 2], p2 = edges[i * 2 + 1];
                g.drawLine(projX[p1], projY[p1], projX[p2], projY[p2]);
            }

            // 5. Guardar estado para el borrado del próximo frame
            for (int i = 0; i < 8; i++) {
                oldProjX[i] = projX[i];
                oldProjY[i] = projY[i];
            }

            // 6. Actualizar rotación y esperar 16ms (~60 FPS)
            angleX = (angleX + 2) % 360;
            angleY = (angleY + 3) % 360;
            angleZ = (angleZ + 1) % 360;
            
            Thread.sleep(16); 

            // 7. Salir con ESC
            if (Native.sys(Native.SYS_READ_KEYBOARD, 0, 0, 0, 0) == 27) break;
        }
    }

    // =========================================================================
    // MOTOR GRÁFICO (JExplorer)
    // =========================================================================
    public static void runStartX() {
        winX = 150; winY = 60; winW = 720; winH = 460;
        windowOpen = true; windowMinimized = false; isDragging = false;
        showStartMenu = false; showContextMenu = false; showAbout = false; showFileMenu = false;
        backgroundMode = 0; selectedNode = null; clipboardNode = null; isNaming = false;

        initFS();
        redrawScreen();
        Toolkit.getDefaultToolkit().beep(1200); Thread.sleep(100); Toolkit.getDefaultToolkit().beep(0);

        int oldMx = 512, oldMy = 384, lastBtn = 0;
        drawMouse(oldMx, oldMy);

        while (true) {
            int hour = Calendar.get(Calendar.HOUR), min = Calendar.get(Calendar.MINUTE), sec = Calendar.get(Calendar.SECOND);
            int day = Calendar.get(Calendar.DAY), month = Calendar.get(Calendar.MONTH), year = Calendar.get(Calendar.YEAR);
            g.setColor(C_LIGHT_GRAY); g.fillRect(880, 728, 144, 38);
            g.setColor(C_BLACK);
            
            g.drawChar((char)((hour / 10) + '0'), 910, 733); g.drawChar((char)((hour % 10) + '0'), 920, 733); g.drawChar(':', 930, 733);
            g.drawChar((char)((min / 10) + '0'), 940, 733);  g.drawChar((char)((min % 10) + '0'), 950, 733);  g.drawChar(':', 960, 733);
            g.drawChar((char)((sec / 10) + '0'), 970, 733);  g.drawChar((char)((sec % 10) + '0'), 980, 733);
            
            g.drawChar((char)((day / 10) + '0'), 890, 750); g.drawChar((char)((day % 10) + '0'), 900, 750); g.drawChar('/', 910, 750);
            g.drawChar((char)((month / 10) + '0'), 920, 750); g.drawChar((char)((month % 10) + '0'), 930, 750); g.drawString("/20", 940, 750);
            g.drawChar((char)((year / 10) + '0'), 970, 750); g.drawChar((char)((year % 10) + '0'), 980, 750);

            int mx = Native.sys(Native.SYS_READ_MOUSE, 0, 0, 0, 0);
            int my = Native.sys(Native.SYS_READ_MOUSE, 1, 0, 0, 0);
            int btn = Native.sys(Native.SYS_READ_MOUSE, 2, 0, 0, 0);

            if (mx < 0) mx = 0; if (mx > 1010) mx = 1010;
            if (my < 0) my = 0; if (my > 750) my = 750;

            if (mx != oldMx || my != oldMy) {
                if (isDragging && windowOpen && !windowMinimized && !showAbout && !isNaming) {
                    winX = mx - dragOffsetX; winY = my - dragOffsetY;
                    if (winX < 0) winX = 0; if (winY < 0) winY = 0;
                    if (winX + winW > 1024) winX = 1024 - winW;
                    if (winY + winH > 726) winY = 726 - winH;
                    redrawScreen();
                } else { clearMouse(oldMx, oldMy); }
                drawMouse(mx, my);
                oldMx = mx; oldMy = my;
            }

            int key = Native.sys(Native.SYS_READ_KEYBOARD, 0, 0, 0, 0);
            if (isNaming) {
                if (key != 0 && key != lastUIKey) {
                    if (key == 13 && newNameLen > 0) {
                        int[] custom = new int[newNameLen];
                        for(int i = 0; i < newNameLen; i++) custom[i] = newNameBuf[i];
                        addNodeCustom(currentDir, custom, newNameLen, namingIsDir);
                        isNaming = false; redrawScreen(); drawMouse(mx, my);
                    } else if (key == 27) { isNaming = false; redrawScreen(); drawMouse(mx, my);
                    } else if (key == 8 && newNameLen > 0) { newNameLen--; redrawScreen(); drawMouse(mx, my);
                    } else if (key >= 32 && key <= 126 && newNameLen < 15) { newNameBuf[newNameLen] = key; newNameLen++; redrawScreen(); drawMouse(mx, my); }
                }
                if (key == 0) lastUIKey = 0; else lastUIKey = key;
            } else { if (key == 27) break; }

            if (!isNaming) {
                if (btn == 2 && lastBtn != 2) { 
                    if (showContextMenu || showStartMenu || showFileMenu) { showContextMenu = false; showStartMenu = false; showFileMenu = false; redrawScreen(); }
                    int viewX = winX + 195, viewY = winY + 35, viewW = winW - 205, viewH = winH - 45;
                    if (windowOpen && !windowMinimized && mx >= viewX && mx <= viewX + viewW && my >= viewY && my <= viewY + viewH) {
                        fileMenuTarget = getClickedNode(mx, my);
                        if(fileMenuTarget != null) selectedNode = fileMenuTarget;
                        showFileMenu = true; fileMenuX = mx; fileMenuY = my;
                        if (fileMenuX > 860) fileMenuX = 860; if (fileMenuY > 640) fileMenuY = 640;
                    } else {
                        showContextMenu = true; showStartMenu = false; contextX = mx; contextY = my;
                        if (contextX > 820) contextX = 820; if (contextY > 600) contextY = 600;
                    }
                    redrawScreen(); drawMouse(mx, my); lastBtn = btn;
                } else if (btn == 1 && lastBtn != 1) { 
                    int ticks = (int) java.lang.System.currentTimeMillis();
                    boolean isDoubleClick = (lastClickTick > 0 && (ticks - lastClickTick) < 500);
                    lastClickTick = ticks;

                    if (showFileMenu) {
                        if (mx >= fileMenuX && mx <= fileMenuX + 160) {
                            if (fileMenuTarget != null) {
                                if (my >= fileMenuY + 5 && my <= fileMenuY + 25) { if(fileMenuTarget.isDir) currentDir = fileMenuTarget; selectedNode = null; }
                                else if (my >= fileMenuY + 25 && my <= fileMenuY + 45) clipboardNode = fileMenuTarget;
                                else if (my >= fileMenuY + 45 && my <= fileMenuY + 65) removeNode(fileMenuTarget);
                            } else {
                                if (my >= fileMenuY + 5 && my <= fileMenuY + 25) { isNaming = true; namingIsDir = false; newNameLen = 0; }
                                else if (my >= fileMenuY + 25 && my <= fileMenuY + 45) { isNaming = true; namingIsDir = true; newNameLen = 0; }
                                else if (my >= fileMenuY + 45 && my <= fileMenuY + 65) {
                                    if (clipboardNode != null) {
                                        if (clipboardNode.name != null) addNode(currentDir, clipboardNode.name, clipboardNode.isDir);
                                        else addNodeCustom(currentDir, clipboardNode.customName, clipboardNode.customLen, clipboardNode.isDir);
                                    }
                                }
                            }
                        }
                        showFileMenu = false; redrawScreen(); drawMouse(mx, my);
                    } else if (showContextMenu) {
                        if (mx >= contextX && mx <= contextX + 190) {
                            if (my >= contextY + 15 && my <= contextY + 35) backgroundMode = 0;
                            else if (my >= contextY + 35 && my <= contextY + 55) backgroundMode = 1;
                            else if (my >= contextY + 55 && my <= contextY + 75) backgroundMode = 2;
                            else if (my >= contextY + 75 && my <= contextY + 95) { windowOpen = true; windowMinimized = false; }
                            else if (my >= contextY + 95 && my <= contextY + 115) showAbout = true;
                        }
                        showContextMenu = false; redrawScreen(); drawMouse(mx, my);
                    } else if (showStartMenu) {
                        int menuY = 726 - 105;
                        if (mx >= 5 && mx <= 185 && my >= menuY && my <= 726) {
                            if (my >= menuY + 20 && my < menuY + 50) { windowOpen = true; windowMinimized = false; }
                            else if (my >= menuY + 50 && my < menuY + 80) { windowOpen = false; showAbout = false; }
                            else if (my >= menuY + 80 && my <= 726) { clearScreen(); shutdown(); }
                        }
                        showStartMenu = false; redrawScreen(); drawMouse(mx, my);
                    } else if (showAbout) {
                        int ax = 262, ay = 250, aw = 500;
                        int btnX = ax + aw - 23, btnY = ay + 5;
                        if (mx >= btnX && mx <= btnX + 18 && my >= btnY && my <= btnY + 18) { showAbout = false; redrawScreen(); drawMouse(mx, my); }
                    } else if (my >= 726) { 
                        if (mx >= 5 && mx <= 85) { showStartMenu = !showStartMenu; redrawScreen(); drawMouse(mx, my); }
                        else if (windowOpen && mx >= 95 && mx <= 235) { windowMinimized = !windowMinimized; redrawScreen(); drawMouse(mx, my); }
                    } else if (windowOpen && !windowMinimized) {
                        int btnX = winX + winW - 23, btnY = winY + 5;
                        if (mx >= btnX && mx <= btnX + 18 && my >= btnY && my <= btnY + 18) {
                            windowOpen = false; redrawScreen(); drawMouse(mx, my);
                        } else if (mx >= winX && mx <= winX + winW - 30 && my >= winY && my <= winY + 27) {
                            isDragging = true; dragOffsetX = mx - winX; dragOffsetY = my - winY;
                        } else if (mx >= winX + 10 && mx <= winX + 190 && my >= winY + 35 && my <= winY + winH - 45) {
                            int nodeY = winY + 55;
                            if (my >= nodeY - 5 && my <= nodeY + 15) { currentDir = root; selectedNode = null; redrawScreen(); drawMouse(mx, my); }
                            nodeY += 25;
                            for (int i = 0; i < root.childCount; i++) {
                                Node child = root.children[i];
                                if (child.isDir) {
                                    if (my >= nodeY - 5 && my <= nodeY + 15) { currentDir = child; selectedNode = null; redrawScreen(); drawMouse(mx, my); }
                                    nodeY += 20;
                                }
                            }
                        } else if (mx >= winX + 195 && mx <= winX + winW - 10 && my >= winY + 35 && my <= winY + winH - 45) {
                            Node clicked = getClickedNode(mx, my);
                            if (clicked == null) {
                                int iconX = winX + 215, iconY = winY + 55;
                                if (currentDir.parent != null && mx >= iconX && mx <= iconX + 60 && my >= iconY && my <= iconY + 50) {
                                    currentDir = currentDir.parent; selectedNode = null; redrawScreen(); drawMouse(mx, my);
                                } else { selectedNode = null; redrawScreen(); drawMouse(mx, my); }
                            } else {
                                if (selectedNode == clicked && isDoubleClick) { if(clicked.isDir) { currentDir = clicked; selectedNode = null; } } 
                                else { selectedNode = clicked; }
                                redrawScreen(); drawMouse(mx, my);
                            }
                        }
                    }
                    lastBtn = 1;
                } else if (btn == 0) { isDragging = false; lastBtn = 0; }
            }
            Thread.sleep(1);
        }
        clearScreen();
    }

    public static Node getClickedNode(int mx, int my) {
        int iconX = winX + 215, iconY = winY + 55;
        if (currentDir.parent != null) iconX += 90; 
        for (int i = 0; i < currentDir.childCount; i++) {
            Node child = currentDir.children[i];
            if (child != null) {
                if (mx >= iconX - 5 && mx <= iconX + 65 && my >= iconY - 5 && my <= iconY + 55) return child;
                iconX += 90;
                if (iconX > winX + winW - 80) { iconX = winX + 215; iconY += 60; }
            }
        }
        return null;
    }

    public static void addNode(Node parent, String staticName, boolean isDir) {
        if (parent.childCount >= 8) return; 
        Node n = new Node(); n.name = staticName; n.isDir = isDir; n.parent = parent; n.childCount = 0; 
        if (isDir) n.children = new Node[8]; 
        parent.children[parent.childCount] = n; parent.childCount++;
    }

    public static void addNodeCustom(Node parent, int[] customName, int customLen, boolean isDir) {
        if (parent.childCount >= 8) return; 
        Node n = new Node(); n.customName = customName; n.customLen = customLen; n.isDir = isDir; n.parent = parent; n.childCount = 0; 
        if (isDir) n.children = new Node[8]; 
        parent.children[parent.childCount] = n; parent.childCount++;
    }

    public static void removeNode(Node target) {
        if (target == null || target.parent == null) return;
        Node p = target.parent; int idx = -1;
        for (int i = 0; i < p.childCount; i++) if (p.children[i] == target) { idx = i; break; }
        if (idx != -1) {
            for (int i = idx; i < p.childCount - 1; i++) p.children[i] = p.children[i + 1];
            p.childCount--; p.children[p.childCount] = null;
        }
        if (selectedNode == target) selectedNode = null;
    }

    public static void redrawScreen() {
        drawBackground(); drawWindow(); drawTaskbar(); drawStartMenu(); drawContextMenu(); drawFileMenu(); drawNamingWindow(); drawAboutWindow();
    }

    public static void drawBackground() {
        if (backgroundMode == 0) {
            g.setColor(C_TRANSPARENT); g.fillRect(0, 0, 1024, 726);
        } else if (backgroundMode == 1) {
            for (int y = 0; y < 726; y += 8) {
                int red = (y * 255) / 726;
                g.setColor(new Color(((red / 2) << 16) | ((255 - red) / 2)));
                g.fillRect(0, y, 1024, 8);
            }
        } else if (backgroundMode == 2) {
            for (int px = 0; px < 1024; px += 4) {
                for (int py = 0; py < 726; py += 4) {
                    int x0 = ((px - 600) * 4096) / 300, y0 = ((py - 364) * 4096) / 300;
                    int cx = 0, cy = 0, iter = 0;
                    while (iter < 24) {
                        int nx2 = (cx * cx) >> 12, ny2 = (cy * cy) >> 12;
                        if (nx2 + ny2 > 16384) break;
                        int xtemp = nx2 - ny2 + x0;
                        cy = ((2 * cx * cy) >> 12) + y0; cx = xtemp; iter++;
                    }
                    if (iter < 24) g.setColor(new Color(0x000000FF | (iter * 10 << 8) | (iter * 5)));
                    else g.setColor(C_BLACK);
                    g.fillRect(px, py, 4, 4);
                }
            }
        }
    }

    public static void drawWindow() {
        if (!windowOpen || windowMinimized) return;
        g.setColor(C_LIGHT_GRAY); g.fillRect(winX, winY, winW, winH);
        g.setColor(C_WIN_TITLE); g.fillRect(winX + 3, winY + 3, winW - 6, 24);
        g.setColor(C_WHITE); g.drawString("JExplorer - ", winX + 10, winY + 10); 
        
        if (currentDir.name != null) g.drawString(currentDir.name, winX + 130, winY + 10);
        else for(int c = 0; c < currentDir.customLen; c++) g.drawChar((char)currentDir.customName[c], winX + 130 + c*10, winY + 10);

        int btnX = winX + winW - 23, btnY = winY + 5;
        g.setColor(C_RED); g.fillRect(btnX, btnY, 18, 18);
        g.setColor(C_WHITE); g.drawString("X", btnX + 5, btnY + 5);

        int treeX = winX + 10, treeY = winY + 35, treeW = 180, treeH = winH - 45;
        int viewX = winX + 195, viewY = winY + 35, viewW = winW - 205, viewH = winH - 45;

        g.setColor(C_WIN_BG); g.fillRect(treeX, treeY, treeW, treeH);
        g.setColor(C_WHITE); g.fillRect(viewX, viewY, viewW, viewH);

        int nodeY = treeY + 20;
        if (currentDir == root) { g.setColor(C_WIN_TITLE); g.fillRect(treeX + 5, nodeY - 5, 170, 20); g.setColor(C_WHITE); } 
        else g.setColor(C_BLACK);
        g.drawString("[-] / (Root)", treeX + 10, nodeY); nodeY += 25;

        for (int i = 0; i < root.childCount; i++) {
            Node child = root.children[i];
            if (child.isDir) {
                if (child == currentDir) { g.setColor(C_WIN_TITLE); g.fillRect(treeX + 15, nodeY - 5, 160, 20); g.setColor(C_WHITE); } 
                else g.setColor(C_BLACK);
                g.drawString("+-- ", treeX + 25, nodeY); 
                if (child.name != null) g.drawString(child.name, treeX + 55, nodeY);
                else for(int c = 0; c < child.customLen; c++) g.drawChar((char)child.customName[c], treeX + 55 + c*10, nodeY);
                nodeY += 20;
            }
        }

        int iconX = viewX + 20, iconY = viewY + 20;
        if (currentDir.parent != null) {
            g.setColor(C_GRAY); g.fillRect(iconX, iconY, 32, 22);
            g.setColor(C_BLACK); g.drawString(".. (Atras)", iconX, iconY + 38);
            iconX += 90;
        }

        for (int i = 0; i < currentDir.childCount; i++) {
            Node child = currentDir.children[i];
            if (child != null) {
                if (child == selectedNode) { g.setColor(C_SEL); g.fillRect(iconX - 5, iconY - 5, 80, 60); }
                if (child.isDir) { g.setColor(C_FOLDER); g.fillRect(iconX, iconY, 32, 22); g.fillRect(iconX, iconY - 4, 12, 4); } 
                else { g.setColor(C_FILE); g.fillRect(iconX, iconY, 20, 26); }
                g.setColor(C_BLACK); 
                if (child.name != null) g.drawString(child.name, iconX, iconY + 38);
                else for(int c = 0; c < child.customLen; c++) g.drawChar((char)child.customName[c], iconX + c*10, iconY + 38);
                iconX += 90;
                if (iconX > viewX + viewW - 80) { iconX = viewX + 20; iconY += 60; }
            }
        }
    }

    public static void drawTaskbar() {
        int taskbarY = 726;
        g.setColor(C_LIGHT_GRAY); g.fillRect(0, taskbarY, 1024, 42);
        g.setColor(C_WHITE); g.fillRect(0, taskbarY, 1024, 2);

        g.setColor(showStartMenu ? C_GRAY : C_GREEN);
        g.fillRect(5, taskbarY + 4, 80, 32);
        g.setColor(C_WHITE); g.drawString("INICIO", 22, taskbarY + 14);

        if (windowOpen) {
            g.setColor(windowMinimized ? C_FILE : C_WIN_BG);
            g.fillRect(95, taskbarY + 4, 140, 32);
            g.setColor(C_BLACK); g.drawString("JExplorer", 110, taskbarY + 14);
        }
    }

    public static void drawStartMenu() {
        if (!showStartMenu) return;
        int menuH = 105, menuY = 726 - menuH;
        g.setColor(C_LIGHT_GRAY); g.fillRect(5, menuY, 180, menuH);
        g.setColor(C_WIN_TITLE); g.fillRect(5, menuY, 25, menuH);
        g.setColor(C_BLACK);
        g.drawString("Abrir JExplorer", 35, menuY + 25);
        g.drawString("Cerrar Ventanas", 35, menuY + 55);
        g.drawString("Apagar Equipo", 35, menuY + 85);
    }

    public static void drawContextMenu() {
        if (!showContextMenu) return;
        g.setColor(new Color(0x00F0F0F0)); g.fillRect(contextX, contextY, 190, 125);
        g.setColor(C_BLACK); g.drawRect(contextX, contextY, 190, 125);
        g.drawString("Fondo Solido", contextX + 15, contextY + 20);
        g.drawString("Fondo Gradiente", contextX + 15, contextY + 40);
        g.drawString("Fondo Fractal", contextX + 15, contextY + 60);
        g.drawString("Abrir Explorador", contextX + 15, contextY + 80);
        g.drawString("Acerca de JVMOS", contextX + 15, contextY + 100);
    }

    public static void drawFileMenu() {
        if (!showFileMenu) return;
        g.setColor(new Color(0x00F0F0F0)); g.fillRect(fileMenuX, fileMenuY, 160, 70);
        g.setColor(C_BLACK); g.drawRect(fileMenuX, fileMenuY, 160, 70);
        if (fileMenuTarget != null) {
            g.drawString("Abrir Elemento", fileMenuX + 10, fileMenuY + 20);
            g.drawString("Copiar Elemento", fileMenuX + 10, fileMenuY + 40);
            g.drawString("Eliminar", fileMenuX + 10, fileMenuY + 60);
        } else {
            g.drawString("Nuevo Archivo", fileMenuX + 10, fileMenuY + 20);
            g.drawString("Nueva Carpeta", fileMenuX + 10, fileMenuY + 40);
            g.drawString("Pegar Aqui", fileMenuX + 10, fileMenuY + 60);
        }
    }

    public static void drawNamingWindow() {
        if (!isNaming) return;
        int bx = winX + 250, by = winY + 150;
        g.setColor(C_WIN_BG); g.fillRect(bx, by, 220, 60);
        g.setColor(C_BLACK); g.drawRect(bx, by, 220, 60);
        g.drawString(namingIsDir ? "Nombre Carpeta:" : "Nombre Archivo:", bx + 10, by + 20);
        g.setColor(C_WHITE); g.fillRect(bx + 10, by + 30, 200, 20);
        g.setColor(C_BLACK);
        for(int i = 0; i < newNameLen; i++) g.drawChar((char)newNameBuf[i], bx + 12 + i*10, by + 45);
        g.fillRect(bx + 12 + newNameLen*10, by + 32, 8, 16); 
    }

    public static void drawAboutWindow() {
        if (!showAbout) return;
        int ax = 262, ay = 250, aw = 500, ah = 220;
        g.setColor(C_WIN_BG); g.fillRect(ax, ay, aw, ah);
        g.setColor(new Color(0x001F4E5B)); g.fillRect(ax + 3, ay + 3, aw - 6, 24);
        g.setColor(C_WHITE); g.drawString("Acerca de JVMOS", ax + 10, ay + 10);
        
        int btnX = ax + aw - 23, btnY = ay + 5;
        g.setColor(C_RED); g.fillRect(btnX, btnY, 18, 18);
        g.setColor(C_WHITE); g.drawString("X", btnX + 5, btnY + 5);

        g.setColor(C_BLACK);
        g.drawString("JVMOS - Version 1.0", ax + 170, ay + 60);
        g.drawString("Sistema operativo escrito en ASM y Java", ax + 20, ay + 80);
        g.drawString("Hecho por: Allan Ayes Ramirez (30/08/2026)", ax + 20, ay + 110);
        g.drawString("GitHub: aayes89", ax + 20, ay + 130);
        g.drawString("Memoria RAM: 128 MB (Estatica BIOS)", ax + 20, ay + 160);
        g.drawString("Video: VBE VESA 1024x768 @ 32bpp", ax + 20, ay + 180);
    }
    
    public static void clearMouse(int x, int y) {
        if (backgroundMode == 0) { g.setColor(C_TRANSPARENT); g.fillRect(x, y, 14, 18);
        } else if (backgroundMode == 1) {
            for (int iy = y; iy < y + 18; iy += 2) {
                if (iy >= 726) break;
                int red = (iy * 255) / 726; g.setColor(new Color(((red / 2) << 16) | ((255 - red) / 2))); g.fillRect(x, iy, 14, 2);
            }
        } else if (backgroundMode == 2) {
            for (int px = (x / 4) * 4; px < x + 14; px += 4) {
                for (int py = (y / 4) * 4; py < y + 18; py += 4) {
                    if (py >= 726) continue;
                    int x0 = ((px - 600) * 4096) / 300, y0 = ((py - 364) * 4096) / 300;
                    int cx = 0, cy = 0, iter = 0;
                    while (iter < 24) {
                        int nx2 = (cx * cx) >> 12, ny2 = (cy * cy) >> 12;
                        if (nx2 + ny2 > 16384) break;
                        int xtemp = nx2 - ny2 + x0; cy = ((2 * cx * cy) >> 12) + y0; cx = xtemp; iter++;
                    }
                    if (iter < 24) g.setColor(new Color(0x000000FF | (iter * 10 << 8) | (iter * 5))); else g.setColor(C_BLACK);
                    g.fillRect(px, py, 4, 4);
                }
            }
        }
        if (windowOpen && !windowMinimized && x < winX + winW && x + 14 > winX && y < winY + winH && y + 18 > winY) drawWindow();
        if (showAbout && x < 262 + 500 && x + 14 > 262 && y < 250 + 220 && y + 18 > 250) drawAboutWindow();
        if (isNaming && x < winX + 470 && x + 14 > winX + 250 && y < winY + 210 && y + 18 > winY + 150) drawNamingWindow();
        if (y + 18 >= 726) drawTaskbar();
        if (showStartMenu && x < 185 && y + 18 > 621) drawStartMenu();
        if (showContextMenu && x < contextX + 190 && x + 14 > contextX && y < contextY + 125 && y + 18 > contextY) drawContextMenu();
        if (showFileMenu && x < fileMenuX + 160 && x + 14 > fileMenuX && y < fileMenuY + 70 && y + 18 > fileMenuY) drawFileMenu();
    }

    public static void drawMouse(int x, int y) {
        g.setColor(C_BLACK); for (int i = 0; i < 12; i++) g.fillRect(x, y + i, i + 2, 1); g.fillRect(x + 2, y + 12, 4, 5);
        g.setColor(C_WHITE); for (int i = 1; i < 10; i++) g.fillRect(x + 1, y + i, i, 1); g.fillRect(x + 3, y + 10, 2, 6);
    }

    public static void initFS() {
        root = new Node(); root.name = "Root"; root.isDir = true; root.children = new Node[8]; root.childCount = 0; root.parent = null;
        Node appsDir = new Node(); appsDir.name = "APPS"; appsDir.isDir = true; appsDir.children = new Node[8]; appsDir.childCount = 0; appsDir.parent = root;
        Node paintApp = new Node(); paintApp.name = "Paint.class"; paintApp.isDir = false; paintApp.parent = appsDir;
        appsDir.children[0] = paintApp; appsDir.childCount = 1;
        Node docsDir = new Node(); docsDir.name = "DOCS"; docsDir.isDir = true; docsDir.children = new Node[8]; docsDir.childCount = 0; docsDir.parent = root;
        root.children[0] = appsDir; root.children[1] = docsDir; root.childCount = 2;
        currentDir = root;
    }

    public static void showTime(int y) {
        int hour = Calendar.get(Calendar.HOUR), min  = Calendar.get(Calendar.MINUTE), sec  = Calendar.get(Calendar.SECOND);
        g.setColor(C_GREEN); g.drawString(" HORA: ", 20, y);
        g.drawChar((char)((hour / 10) + '0'), 90, y); g.drawChar((char)((hour % 10) + '0'), 100, y); g.drawChar(':', 110, y);
        g.drawChar((char)((min / 10) + '0'), 120, y); g.drawChar((char)((min % 10) + '0'), 130, y);  g.drawChar(':', 140, y);
        g.drawChar((char)((sec / 10) + '0'), 150, y); g.drawChar((char)((sec % 10) + '0'), 160, y);
    }

    public static void showDate(int y) {
        int day = Calendar.get(Calendar.DAY), month = Calendar.get(Calendar.MONTH), year = Calendar.get(Calendar.YEAR);
        g.setColor(C_GREEN); g.drawString("FECHA: ", 20, y);
        g.drawChar((char)((day / 10) + '0'), 90, y); g.drawChar((char)((day % 10) + '0'), 100, y); g.drawChar('/', 110, y);
        g.drawChar((char)((month / 10) + '0'), 120, y); g.drawChar((char)((month % 10) + '0'), 130, y); g.drawString("/20", 140, y);
        g.drawChar((char)((year / 10) + '0'), 170, y); g.drawChar((char)((year % 10) + '0'), 180, y);
    }

    public static void initKeyboard() { Native.sys(Native.SYS_SET_KBD_LAYOUT, 1, 0, 0, 0); }
    public static void clearScreen() { g.setColor(C_BLACK); g.fillRect(0, 0, 1024, 768); }
    public static void showCursor(int y) { g.setColor(C_GREEN); g.drawString("JVMOS>", 20, y); }

    public static void dramaticBIOS() {
        clearScreen(); try { Thread.sleep(250); } catch(Exception e) {}
        g.setColor(C_GREEN); g.drawString("JVMOS BIOS [v2.5]", 20, 25); g.drawString("=============================================", 20, 45);
        g.drawString("[ OK ]", 20, 75); g.setColor(C_WHITE); g.drawString("Verificando CPU x86 [Protected Mode 32-Bit]...", 90, 75);
        g.setColor(C_GREEN); g.drawString("[ OK ]", 20, 95); g.setColor(C_WHITE); g.drawString("Memoria RAM Detectada: [128MB]", 90, 95);
        g.setColor(C_GREEN); g.drawString("[ OK ]", 20, 115); g.setColor(C_WHITE); g.drawString("Cargando Driver PS/2 Keyboard [LATAM ISO Map]", 90, 115);
        g.setColor(C_GREEN); g.drawString("[ OK ]", 20, 135); g.setColor(C_WHITE); g.drawString("Cargando Driver Mouse i8042 [240 DPI]", 90, 135);
        g.setColor(C_GREEN); g.drawString("[ OK ]", 20, 155); g.setColor(C_WHITE); g.drawString("Montando Sistema de Archivos JVMFS [Virtual Ramdisk]", 90, 155);
        g.setColor(C_GREEN); g.drawString("[ OK ]", 20, 175); g.setColor(C_WHITE); g.drawString("Modo de Video VBE VESA [1024x768 @ 32bpp]", 90, 175);
        g.setColor(C_GREEN); g.drawString("SISTEMA LISTO. Iniciando Shell interactivo...", 20, 205);
        try { Thread.sleep(1000); } catch(Exception e) {} clearScreen();
    }

    public static void runSystemDiagnostics() {
        System.out.println("=========================================");
        System.out.println("   Iniciando Diagnosticos del Kernel     ");
        System.out.println("=========================================");

        // 1. Prueba de Strings y StringBuilder
        String base = "  000Baremetal  ";
        String trim = base.trim();
        String replaced = trim.replaceFirst("^0+", "JVM-");
        System.out.println("[Test 1] Manipulacion de Cadenas:");
        System.out.println("Original: '" + base + "'");
        System.out.println("Limpia:   '" + replaced + "'");

        // 2. Prueba de la FPU (Math)
        System.out.println("\n[Test 2] Operaciones Matematicas:");
        double angle = Math.PI / 2.0;
        System.out.println("Seno de PI/2: " + Math.sin(angle));
        System.out.println("Raiz de 144:  " + Math.sqrt(144.0));
        System.out.println("Maximo (5, 9): " + Math.max(5, 9));

        // 3. Prueba de BigInteger Pure Java
        System.out.println("\n[Test 3] Precision Arbitraria (BigInteger):");
        BigInteger a = BigInteger.valueOf(5000);
        BigInteger b = BigInteger.valueOf(7000);
        BigInteger sum = a.add(b);
        BigInteger mul = a.multiply(b);
        System.out.println("Sum (5000+7000): " + sum.toString());
        System.out.println("Mul (5000*7000): " + mul.toString());

        System.out.println("=========================================");
        System.out.println("    Diagnosticos Completados con Exito   ");
        System.out.println("=========================================");
    }

    public static void shell() {
        g.setColor(C_GREEN);
        g.drawString("JVMOS TERMINAL INTERACTIVA - Escriba 'help' para ver comandos disponibles", 20, 30);
        g.drawString("----------------------------------------------------------", 20, 50);
    }

    public static void shutdown() {
        g.setColor(C_RED); g.drawString("SISTEMA APAGADO. CERRANDO EN 2s...", 380, 360);
        //Toolkit.getDefaultToolkit().beep(500); try{ Thread.sleep(500); } catch(Exception e){} Toolkit.getDefaultToolkit().beep(0);
        try { Thread.sleep(1500); } catch(Exception e){} java.lang.System.exit(0);
    }
}
