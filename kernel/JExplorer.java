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

import java.awt.ui.JFrame;
import java.awt.ui.JPanel;
import java.awt.ui.JLabel;
import java.io.FileSystem;
import java.io.File;
import java.awt.Graphics2D;

// Funcionalidades extraídas de la implementación de Filesystem y shell previospackage kernel;
public class JExplorer extends JFrame {
    private FileSystem fs;
    private Portapapeles portapapeles;
    private JEditor editor;
    
    // Navegación
    private int currentDirLba;
    private String currentDirPath;
    private int parentDirLba;
    
    // Componentes Visuales
    private JPanel leftPanel;
    private JPanel rightPanel;
    private JLabel pathLabel;
    
    // Caché (Será migrado a un JList/JFileView en el futuro)
    private File[] viewFiles;
    private int viewFileCount;

    public JExplorer(FileSystem fs, Portapapeles clip, JEditor ed) {
        super("JExplorer - /", 150, 60, 720, 460);
        this.fs = fs;
        this.portapapeles = clip;
        this.editor = ed;
        
        this.currentDirLba = FileSystem.ROOT_DIR_LBA;
        this.parentDirLba = FileSystem.ROOT_DIR_LBA;
        this.currentDirPath = "/";
        this.viewFiles = new File[128];
        
        // Configurar Paneles Internos
        pathLabel = new JLabel("Ruta: /", 10, 30, 0x00000000);
        
        // Panel izquierdo (Árbol, gris oscuro)
        leftPanel = new JPanel(10, 50, 180, 400, 0x00E0E0E0);
        leftPanel.add(new JLabel("[-] / (Raiz)", 10, 10, 0x00000000));
        
        // Panel derecho (Vista de archivos, fondo blanco)
        rightPanel = new JPanel(200, 50, 510, 400, 0x00FFFFFF) {
            // Sobrescribimos temporalmente el paint del panel para dibujar los archivos
            @Override
            public void paint(Graphics2D g) {
                super.paint(g); // Dibuja el fondo blanco
                drawFiles(g, this.x, this.y, this.width);
            }
        };

        this.add(pathLabel);
        this.add(leftPanel);
        this.add(rightPanel);
        
        this.refreshView();
    }

    public void refreshView() {
        File[] tempFiles = fs.listFiles(currentDirLba, currentDirPath);
        this.viewFileCount = fs.getCantFiles();
        for (int i = 0; i < this.viewFileCount; i++) {
            this.viewFiles[i] = tempFiles[i];
        }
        this.setTitle("JExplorer - " + currentDirPath);
        this.pathLabel.setText("Ruta: " + currentDirPath);
    }
    
    // Método temporal incrustado en el rightPanel
    private void drawFiles(Graphics2D g, int panelX, int panelY, int panelW) {
        int iconX = panelX + 20;
        int iconY = panelY + 20;
        
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
                if (iconX > panelX + panelW - 80) { 
                    iconX = panelX + 20; iconY += 60; 
                }
            }
        }
    }
}
