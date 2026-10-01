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
import java.awt.Color;

public class JFrame extends JComponent {
    private String title;
    private JComponent[] children;
    private int childCount;
    
    // Estado de ventana
    public boolean isDragging;
    public int dragOffsetX;
    public int dragOffsetY;
	protected boolean isMinimized;
    
    private static final int MAX_CHILDREN = 32;
    private static final int TITLE_BAR_HEIGHT = 24;

    public JFrame(String title, int x, int y, int width, int height) {
        super(x, y, width, height);
        this.title = title;
        this.children = new JComponent[MAX_CHILDREN];
        this.childCount = 0;
        this.visible = false; // Se inicia oculta por defecto
        this.isMinimized = false; 
    }

    public void add(JComponent c) {
        if (childCount < MAX_CHILDREN && c != null) {
            c.parent = this;           
            children[childCount++] = c;
        }
    }

    @Override
    public void paint(Graphics2D g) {
        if (!visible) return;		
		
        g.setColor(0xFFD4D0C8); // Gris clásico de Windows
        g.fillRect(x, y, width, height);
        
        g.setColor(0xFF1F4E5B); // Azul oscuro OPACO (Reemplaza 0x001F4E5B)
        g.fillRect(x + 3, y + 3, width - 6, TITLE_BAR_HEIGHT);
        
        g.setColor(0xFFFFFFFF);
        g.drawString(title, x + 10, y + 18);
        
        int btnX = x + width - 23;
        int btnY = y + 5;
        g.setColor(0xFFFF0000); // Rojo OPACO
        g.fillRect(btnX, btnY, 18, 18);
        g.setColor(0xFFFFFFFF);
        g.drawString("X", btnX + 6, btnY + 14);

        for (int i = 0; i < childCount; i++) {
            if (children[i].isVisible()) {
                children[i].paint(g);
            }
        }
    }

    @Override
    public boolean handleMouse(int mx, int my, int btn) {
        if (!visible || !contains(mx, my)) return false;
		
		int absX = getX();
		int absY = getY();
		
        // Verificar botón de cierre
        int btnX = absX + width - 23;
        int btnY = absY + 5;
        if (btn == 1 && mx >= btnX && mx <= btnX + 18 && my >= btnY && my <= btnY + 18) {
            this.visible = false;
            return true;
        }

        // Verificar arrastre de la barra de título
        if (btn == 1 && my >= absY && my <= absY + TITLE_BAR_HEIGHT + 3) {
            isDragging = true;
            dragOffsetX = mx - x;
            dragOffsetY = my - y;
            return true;
        }

        if (btn == 0) {
            isDragging = false;
        }

        // Rutear clic a los componentes hijos
        for (int i = childCount - 1; i >= 0; i--) {
            if (children[i].handleMouse(mx, my, btn)) {
                return true; 
            }
        }
        
        return true; 
    }

    @Override
    public boolean handleKey(int key){
        // No hacer nada aquí
        return key!=0?true:false;
    }
	
	// Getter y setters
	public boolean isMinimized() {
        return isMinimized;
    }

    public void setMinimized(boolean minimized) {
        this.isMinimized = minimized;
    }
	public String getTitle(){
		return title;
	}
	public void setTitle(String title){
		this.title = title;
	}
    public JComponent[] getChildren(){
		return children;
	}
    public int getChildCount(){
		return childCount;
	}
	public void setChildCount(int ccount){
		this.childCount = ccount;
	}
    public boolean getIsDragging(){
		return isDragging;
	}
	public void setIsDragging(boolean isDragging){
		this.isDragging = isDragging;
	}	
    public int getDragOffsetX(){
		return dragOffsetX;
	}
	public void setDragOffsetX(int offset){
		this.dragOffsetX = offset;
	}
    public int getDragOffsetY(){
		return dragOffsetY;
	}
	public void setDragOffsetY(int offset){
		this.dragOffsetY = offset;
	}
}
