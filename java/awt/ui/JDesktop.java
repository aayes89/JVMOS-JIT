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
package java.awt.ui;

import java.awt.Graphics2D;
import java.lang.System;
import kernel.Native;
import kernel.JExplorer; 
import kernel.JEditor;

/*
 Fungirá como la UI del escritorio por ahora
 son los elementos de UI.java anterior usando las nuevas clases,
 para romper el esquema monolítico antiguo
 */
public class JDesktop {
    private Graphics2D g;
    
    // Jerarquía visual
    private JFrame[] windows;
    private int windowCount;
    private static final int MAX_WINDOWS = 8;
    
    public JPopupMenu contextMenu;
    public JPopupMenu startMenu;
    public JFrame aboutWindow;
    
    // Referencias a aplicaciones heredadas temporalmente (hasta que se refactoricen a JFrame)
    public JExplorer explorer;
    public JEditor editor;
    
    public int backgroundMode = 0;
    
    public JDesktop(Graphics2D g, JExplorer explorer, JEditor editor) {
        this.g = g;
        this.explorer = explorer;
        this.editor = editor;
        
        this.windows = new JFrame[MAX_WINDOWS];
        this.windowCount = 0;
        
        initMenus();
        initAboutWindow();
		
		addWindow(explorer);
		addWindow(editor);
		addWindow(aboutWindow);
    }
        
    private void initMenus() {
        contextMenu = new JPopupMenu();
        contextMenu.add(new JMenuItem("Fondo Solido", () -> backgroundMode = 0));
        contextMenu.add(new JMenuItem("Fondo Gradiente", () -> backgroundMode = 1));
        contextMenu.add(new JMenuItem("Fondo Fractal", () -> backgroundMode = 2));
        contextMenu.add(new JMenuItem("Abrir Explorador", () -> {
            explorer.refreshView();
            explorer.setVisible(true); 
            explorer.setMinimized(false);
        }));
        contextMenu.add(new JMenuItem("Acerca de JVMOS", () -> aboutWindow.setVisible(true)));
        
        startMenu = new JPopupMenu();
        startMenu.add(new JMenuItem("Abrir JExplorer", () -> {
            explorer.refreshView();
            explorer.setVisible(true); 
            explorer.setMinimized(false);
        }));
        startMenu.add(new JMenuItem("Cerrar Ventanas", () -> {
            explorer.setVisible(false); 
            editor.setVisible(false); 
            aboutWindow.setVisible(false);
        }));
        startMenu.add(new JMenuItem("Apagar Equipo", () -> kernel.UI.shutdown()));
    }
    
    private void initAboutWindow() {		
        aboutWindow = new JFrame("Acerca de JVMOS", 262, 250, 500, 220);
        aboutWindow.add(new JLabel("JVMOS - Version 1.0", 170, 40, 0x00000000));
        aboutWindow.add(new JLabel("Sistema operativo escrito en ASM y Java", 20, 70, 0x00000000));
        aboutWindow.add(new JLabel("Hecho por: Allan Ayes Ramirez", 20, 100, 0x00000000));
        aboutWindow.add(new JLabel("Memoria RAM: 128 MB (Estatica BIOS)", 20, 130, 0x00000000));
        aboutWindow.add(new JLabel("Video: VBE VESA 1024x768 @ 32bpp", 20, 160, 0x00000000));
        aboutWindow.add(new JButton("Aceptar", 200, 185, 100, 24, () -> aboutWindow.setVisible(false)));        
    }
    
    public void addWindow(JFrame win) {
        if (windowCount < MAX_WINDOWS && win != null) {
            windows[windowCount++] = win;
        }
    }
    
    // Lógica del repintado
    public void paint() {
        drawBackground();
        
        for (int i = 0; i < windowCount; i++) {
            if (windows[i].isVisible() && !windows[i].isMinimized()) {
                windows[i].paint(g);
            }
        }
        
        drawTaskbar();
        contextMenu.paint(g);
        startMenu.paint(g);
    }
    
    // Enrutar los eventos 
    public boolean handleMouse(int mx, int my, int btn) {
        // Prioridad Máxima: Menús Flotantes
        if (contextMenu.isVisible() && contextMenu.handleMouse(mx, my, btn)) return true;
        if (startMenu.isVisible() && startMenu.handleMouse(mx, my, btn)) return true;
        
        // Clics fuera de menús los cierran
        if (btn == 1 || btn == 2) {
            contextMenu.setVisible(false);
            startMenu.setVisible(false);
        }

        // Clic Derecho en el escritorio abre el menú contextual
        if (btn == 2 && my < 726) {
            int cx = mx > 830 ? 830 : mx;
            int cy = my > 600 ? 600 : my;
            contextMenu.show(cx, cy);
            return true;
        }

        // Ventanas Estructuradas (JFrames en orden inverso para Z-Index)
        for (int i = windowCount - 1; i >= 0; i--) {
            if (windows[i].isVisible() && !windows[i].isMinimized()) {
                if (windows[i].handleMouse(mx, my, btn)) return true;
            }
        }
        
        if (btn == 1 && my >= 726) {
            if (mx >= 5 && mx <= 85) {
                startMenu.show(5, 726 - startMenu.getHeight()); // Asume que JPopupMenu tiene getHeight()
                return true;
            }
            if (explorer.isVisible() && mx >= 95 && mx <= 235) {
                explorer.setMinimized(!explorer.isMinimized());
                return true;
            }
        }
        return false;
    }
    
    // Las funciones gráficas de fondo y taskbar aquí por ahora
    private void drawBackground() {
        if (backgroundMode == 0) {
            g.setColor(0x00000055); g.fillRect(0, 0, 1024, 726);
        } else if (backgroundMode == 1) {
            for (int y = 0; y < 726; y += 8) {
                int red = (y * 255) / 726;
                g.setColor(((red / 2) << 16) | ((255 - red) / 2));
                g.fillRect(0, y, 1024, 8);
            }
        } else {
             g.setColor(0x00000000); g.fillRect(0, 0, 1024, 726);
        }
    }
	
	private void drawTaskbar() {
       int taskbarY = 726;
        g.setColor(0x00C0C0C0); g.fillRect(0, taskbarY, 1024, 42);
        g.setColor(0x00FFFFFF); g.fillRect(0, taskbarY, 1024, 2);

        g.setColor(startMenu.isVisible() ? 0x00808080 : 0x00008000);
        g.fillRect(5, taskbarY + 4, 80, 32);
        g.setColor(0x00FFFFFF); g.drawString("INICIO", 22, taskbarY + 24);

        if (explorer.isVisible()) {
            g.setColor(explorer.isMinimized() ? 0x00A0A0A0 : 0x00E0E0E0);
            g.fillRect(95, taskbarY + 4, 140, 32);
            g.setColor(0x00000000); g.drawString("JExplorer", 110, taskbarY + 24);
        }
        
    }
	
	
	// Getter y Setters
	public JFrame getAboutWindow(){
		return aboutWindow;
	}
	public Graphics2D getGraphics(){
		return g;
	}   
    public JFrame[] getWindows(){
		return windows;
	}
    public int getWindowCount(){
		return windowCount;
	}
    public int getMaxWindows(){
		return MAX_WINDOWS;
	}    
    public JPopupMenu getContextMenu(){
		return contextMenu;
	}
    public JPopupMenu getStartMenu(){
		return startMenu;
	}
    public JExplorer getExplorer(){
		return explorer;
	}
    public JEditor getEditor(){
		return editor;
	}    
    public int getBackgroundMode(){
		return backgroundMode;
	}
}
