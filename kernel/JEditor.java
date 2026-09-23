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

// Funcionalidades extraídas de la implementación de JPad y shell en proyectos previos
public class JEditor {
    private Graphics2D g;
    private FileSystem fs;
    
    public int winX, winY, winW, winH;
    public boolean windowOpen, isDragging;
    public int dragOffsetX, dragOffsetY;

    private File currentFile;
    private int currentDirLba;
    private char[] textBuffer;
    private int textLen;

    public JEditor(Graphics2D g, FileSystem fs) {
        this.g = g;
        this.fs = fs;
        this.winX = 200; this.winY = 100;
        this.winW = 600; this.winH = 400;
        this.textBuffer = new char[4096]; // Buffer seguro de 4KB
        this.textLen = 0;
        this.windowOpen = false;
    }

    public void openFile(File file, int dirLba) {
        this.currentFile = file;
        this.currentDirLba = dirLba;
        this.textLen = 0;
        
        if (file != null && file.exists()) {
            byte[] data = fs.readFile(file);
            if (data != null) {
                for (int i = 0; i < data.length && i < 4096; i++) {
                    textBuffer[i] = (char) data[i];
                }
                textLen = data.length;
            }
        }
        this.windowOpen = true;
    }

    public void saveFile() {
        if (currentFile != null) {
            byte[] data = new byte[textLen];
            for (int i = 0; i < textLen; i++) {
                data[i] = (byte) textBuffer[i];
            }
            fs.delete(currentFile, currentDirLba); // Limpiar versión antigua
            fs.writeFile(currentFile.getName(), data, currentDirLba);
        }
    }

    public void draw() {
        if (!windowOpen) return;
        
        // Interfaz base
        g.setColor(0x00E0E0E0); g.fillRect(winX, winY, winW, winH);
        g.setColor(0x001F4E5B); g.fillRect(winX + 3, winY + 3, winW - 6, 24);
        g.setColor(0x00FFFFFF); 
        String title = "JEditor - " + (currentFile != null ? currentFile.getName() : "Nuevo");
        g.drawString(title, winX + 10, winY + 20);

        // Botones Superiores
        int btnX = winX + winW - 23, btnY = winY + 5;
        g.setColor(0x00FF0000); g.fillRect(btnX, btnY, 18, 18);
        g.setColor(0x00FFFFFF); g.drawString("X", btnX + 5, btnY + 14);

        int saveX = winX + 10, saveY = winY + 35;
        g.setColor(0x00008000); g.fillRect(saveX, saveY, 80, 24);
        g.setColor(0x00FFFFFF); g.drawString("Guardar", saveX + 10, saveY + 16);

        // Renderizado del texto
        int textX = winX + 10, textY = winY + 65;
        g.setColor(0x00FFFFFF); g.fillRect(textX, textY, winW - 20, winH - 75);
        
        g.setColor(0x00000000);
        int cx = textX + 5, cy = textY + 20;
        for (int i = 0; i < textLen; i++) {
            char c = textBuffer[i];
            if (c == '\n') {
                cy += 20; cx = textX + 5;
            } else {
                g.drawChar(c, cx, cy);
                cx += 10;
                if (cx > winX + winW - 20) { cy += 20; cx = textX + 5; }
            }
        }
        g.fillRect(cx, cy - 12, 8, 14); // Puntero de escritura
    }

    public boolean handleMouse(int mx, int my, boolean isClick) {
        if (!windowOpen) return false;
        if (isClick) {
            int btnX = winX + winW - 23, btnY = winY + 5;
            int saveX = winX + 10, saveY = winY + 35;
            
            if (mx >= btnX && mx <= btnX + 18 && my >= btnY && my <= btnY + 18) {
                windowOpen = false; return true;
            } else if (mx >= saveX && mx <= saveX + 80 && my >= saveY && my <= saveY + 24) {
                saveFile(); return true;
            } else if (mx >= winX && mx <= winX + winW - 30 && my >= winY && my <= winY + 27) {
                isDragging = true; dragOffsetX = mx - winX; dragOffsetY = my - winY;
                return true;
            }
        }
        return false;
    }

    public boolean handleKey(int ascii) {
        if (!windowOpen) return false;
        if (ascii == 8) { 
            if (textLen > 0) textLen--; 
        } else if (ascii == 13) { 
            if (textLen < textBuffer.length) textBuffer[textLen++] = '\n'; 
        } else if (ascii >= 32 && ascii <= 255) {
            if (textLen < textBuffer.length) textBuffer[textLen++] = (char) ascii;
        }
        return true;
    }
}
