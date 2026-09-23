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
import java.io.FileSystem;
import java.io.File;
import kernel.Portapapeles;

// Funcionalidades extraídas de la implementación de Filesystem y shell previos
public class JExplorer {
    private Graphics2D g;
    private FileSystem fs;
    private Portapapeles portapapeles;
	private JEditor editor;
    
    public int winX, winY, winW, winH;
    public boolean windowOpen, windowMinimized, isDragging;
    public int dragOffsetX, dragOffsetY;

    // Navegación
    private int currentDirLba;
    private String currentDirPath;
    private int parentDirLba;
    
    // Caché de archivos de la vista actual
    private File[] viewFiles;
    private int viewFileCount;

    // Menú Contextual
    private boolean showMenu;
    private int menuX, menuY;
    private File selectedFile;

    public JExplorer(Graphics2D g, FileSystem fs, Portapapeles clip, JEditor ed) {
        this.g = g;
        this.fs = fs;
        this.portapapeles = clip;
		this.editor = ed;
        this.winX = 150; this.winY = 60; 
        this.winW = 720; this.winH = 460;
        this.windowOpen = false; this.windowMinimized = false;
        this.isDragging = false;
        
        this.currentDirLba = FileSystem.ROOT_DIR_LBA;
        this.parentDirLba = FileSystem.ROOT_DIR_LBA;
        this.currentDirPath = "/";
        this.viewFiles = new File[128];
        this.viewFileCount = 0;
		this.refreshView();
    }

    public void refreshView() {
        File[] tempFiles = fs.listFiles(currentDirLba, currentDirPath);
        this.viewFileCount = fs.getCantFiles();
        for (int i = 0; i < this.viewFileCount; i++) {
            this.viewFiles[i] = tempFiles[i];
        }
    }

    public void draw() {
        if (!windowOpen || windowMinimized) return;

        // Fondo y bordes
        g.setColor(0x00C0C0C0); g.fillRect(winX, winY, winW, winH);
        g.setColor(0x001F4E5B); g.fillRect(winX + 3, winY + 3, winW - 6, 24);
        g.setColor(0x00FFFFFF); 
        g.drawString("JExplorer - " + currentDirPath, winX + 10, winY + 20);

        int btnX = winX + winW - 23, btnY = winY + 5;
        g.setColor(0x00FF0000); g.fillRect(btnX, btnY, 18, 18);
        g.setColor(0x00FFFFFF); g.drawString("X", btnX + 5, btnY + 14);

        int treeX = winX + 10, treeY = winY + 35, treeW = 180, treeH = winH - 45;
        int viewX = winX + 195, viewY = winY + 35, viewW = winW - 205, viewH = winH - 45;

        // Panel Izquierdo
        g.setColor(0x00E0E0E0); g.fillRect(treeX, treeY, treeW, treeH);
        g.setColor(0x00000000);
        g.drawString("[-] / (Raiz)", treeX + 10, treeY + 20);
        
        int nodeY = treeY + 45;
        if (!currentDirPath.equals("/")) {
            g.setColor(0x00000080); g.fillRect(treeX + 15, nodeY - 14, treeW - 20, 18);
            g.setColor(0x00FFFFFF);
            g.drawString("+-- " + currentDirPath, treeX + 20, nodeY);
        }

        // Panel Derecho
        g.setColor(0x00FFFFFF); g.fillRect(viewX, viewY, viewW, viewH);
        int iconX = viewX + 20, iconY = viewY + 20;
        
        if (!currentDirPath.equals("/")) {
            g.setColor(0x00808080); g.fillRect(iconX, iconY, 32, 22);
            g.setColor(0x00000000); g.drawString(".. (Atras)", iconX, iconY + 38);
            iconX += 90;
        }

        for (int i = 0; i < viewFileCount; i++) {
            File child = viewFiles[i];
            if (child != null && child.exists()) {
                if (child.isDirectory()) {
                    g.setColor(0x00F0C000); 
                    g.fillRect(iconX, iconY, 32, 22); 
                    g.fillRect(iconX, iconY - 4, 12, 4);
                } else {
                    String name = child.getName();
                    if (name.indexOf(".class") != -1) g.setColor(0x003366FF);
                    else if (name.indexOf(".txt") != -1) g.setColor(0x00999999);
                    else g.setColor(0x00A0A0A0);
                    
                    g.fillRect(iconX, iconY, 20, 26);
                }
                g.setColor(0x00000000); 
                g.drawString(child.getName(), iconX, iconY + 38);

                iconX += 90;
                if (iconX > viewX + viewW - 80) { 
                    iconX = viewX + 20; iconY += 60; 
                }
            }
        }

        if (showMenu) drawContextMenu();
    }

    private void drawContextMenu() {
        int menuW = 160;
        boolean canExecute = selectedFile != null && !selectedFile.isDirectory() && 
                            (selectedFile.getName().indexOf(".class") != -1 || selectedFile.getName().indexOf(".txt") != -1);
        
        int menuH = canExecute ? 125 : 105;

        g.setColor(0x00F0F0F0); g.fillRect(menuX, menuY, menuW, menuH);
        g.setColor(0x00000000); g.drawRect(menuX, menuY, menuW, menuH);

        int textY = menuY + 20;
        
        if (canExecute) {
            g.setColor(0x00000000); g.drawString("Ejecutar", menuX + 15, textY);
            textY += 20;
            g.drawLine(menuX + 5, textY - 10, menuX + menuW - 5, textY - 10);
        }
        
        g.setColor(0x00000000);
        if (selectedFile != null) {
            g.drawString("Copiar", menuX + 15, textY); textY += 20;
            g.drawString("Cortar", menuX + 15, textY); textY += 20;
            g.drawString("Eliminar", menuX + 15, textY); textY += 20;
            g.drawString("Propiedades", menuX + 15, textY); textY += 20;
            
            g.setColor(0x00A0A0A0); 
            g.drawString("Enviar (Deshabilitado)", menuX + 15, textY);
        } else {
            g.drawString("Pegar", menuX + 15, textY); textY += 20;
            g.drawString("Actualizar", menuX + 15, textY);
        }
    }

    public boolean handleMouse(int mx, int my, boolean isClick, int btn) {
        if (!windowOpen || windowMinimized) return false;

        if (showMenu && isClick && btn == 1) {
            handleMenuClick(mx, my);
            showMenu = false;
            return true;
        }
        if (isClick && showMenu) { showMenu = false; return true; }

        int btnX = winX + winW - 23, btnY = winY + 5;
        
        if (isClick) {
            if (mx >= btnX && mx <= btnX + 18 && my >= btnY && my <= btnY + 18) {
                windowOpen = false;
                return true;
            } 
            else if (mx >= winX && mx <= winX + winW - 30 && my >= winY && my <= winY + 27) {
                isDragging = true; dragOffsetX = mx - winX; dragOffsetY = my - winY;
                return true;
            }

            int viewX = winX + 195, viewY = winY + 35, viewW = winW - 205, viewH = winH - 45;
            if (mx >= viewX && mx <= viewX + viewW && my >= viewY && my <= viewY + viewH) {
                File clickedFile = detectFileClick(mx, my, viewX, viewY, viewW);
                
                if (btn == 2) { 
                    selectedFile = clickedFile;
                    showMenu = true;
                    menuX = mx; menuY = my;
                } else if (btn == 1 && clickedFile != null) { 
                    if (clickedFile.isDirectory()) {
                        parentDirLba = currentDirLba;
                        currentDirLba = clickedFile.getStartLBA();
                        currentDirPath = currentDirPath.equals("/") ? "/" + clickedFile.getName() : currentDirPath + "/" + clickedFile.getName();
                        refreshView();
                    }
                } else if (btn == 1 && clickedFile == null) {
                    if (!currentDirPath.equals("/") && mx >= viewX + 20 && mx <= viewX + 52 && my >= viewY + 20 && my <= viewY + 42) {
                        currentDirLba = FileSystem.ROOT_DIR_LBA;
                        currentDirPath = "/";
                        refreshView();
                    }
                }
                return true;
            }
        }
        return false;
    }

    private File detectFileClick(int mx, int my, int viewX, int viewY, int viewW) {
        int iconX = viewX + 20, iconY = viewY + 20;
        
        if (!currentDirPath.equals("/")) iconX += 90;

        for (int i = 0; i < viewFileCount; i++) {
            File child = viewFiles[i];
            if (child != null && child.exists()) {
                if (mx >= iconX && mx <= iconX + 40 && my >= iconY && my <= iconY + 40) {
                    return child;
                }
                iconX += 90;
                if (iconX > viewX + viewW - 80) { iconX = viewX + 20; iconY += 60; }
            }
        }
        return null;
    }

    private void handleMenuClick(int mx, int my) {
        boolean canExecute = selectedFile != null && !selectedFile.isDirectory() && 
                            (selectedFile.getName().indexOf(".class") != -1 || selectedFile.getName().indexOf(".txt") != -1);
        
        int textY = menuY + 5; 
        
        if (selectedFile != null) {
            if (canExecute && my >= textY && my <= textY + 20) {
                if (selectedFile.getName().indexOf(".class") != -1) {
					fs.execute(selectedFile.getName(), currentDirLba, currentDirPath);
				} else if (selectedFile.getName().indexOf(".txt") != -1) {
					editor.openFile(selectedFile, currentDirLba);
				}
				return;
            }
            if (canExecute) textY += 25;
            
            if (my >= textY && my <= textY + 20) { 
                portapapeles.capturarArchivo(selectedFile, currentDirLba, false); //[cite: 4]
            } else if (my >= textY + 20 && my <= textY + 40) { 
                portapapeles.capturarArchivo(selectedFile, currentDirLba, true); //[cite: 4]
            } else if (my >= textY + 40 && my <= textY + 60) { 
                fs.delete(selectedFile, currentDirLba);
                refreshView();
            }
        } 
        else {
            if (my >= textY && my <= textY + 20 && portapapeles.tieneArchivo()) { //[cite: 4]
                File clipFile = portapapeles.obtenerArchivo(); //[cite: 4]
                int clipSourceLba = portapapeles.obtenerLba(); //[cite: 4]
                File src = fs.lookup(clipFile.getName(), clipSourceLba, "");
                
                if (src != null) {
                    byte[] data = fs.readFile(src);
                    fs.writeFile(src.getName(), data, currentDirLba);
                    if (portapapeles.esCortar()) { //[cite: 4]
                        fs.delete(src, clipSourceLba);
                        portapapeles.limpiarArchivo(); //[cite: 4]
                    }
                    refreshView();
                }
            } else if (my >= textY + 20 && my <= textY + 40) { 
                refreshView();
            }
        }
    }
}
