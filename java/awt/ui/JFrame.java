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
    }

    public void add(JComponent c) {
        if (childCount < MAX_CHILDREN) {
            c.parent = this;
            // Las coordenadas del hijo son relativas a la ventana
            c.x += this.x; 
            c.y += this.y;
            children[childCount++] = c;
        }
    }

    @Override
    public void paint(Graphics2D g) {
        if (!visible) return;

        // Dibujar Borde y Fondo
        g.setColor(0x00E0E0E0);
        g.fillRect(x, y, width, height);
        
        // Barra de Título
        g.setColor(0x001F4E5B);
        g.fillRect(x + 3, y + 3, width - 6, TITLE_BAR_HEIGHT);
        
        // Texto del Título
        g.setColor(0x00FFFFFF);
        g.drawString(title, x + 10, y + 10);
        
        // Botón de Cierre (X)
        int btnX = x + width - 23;
        int btnY = y + 5;
        g.setColor(0x00FF0000);
        g.fillRect(btnX, btnY, 18, 18);
        g.setColor(0x00FFFFFF);
        g.drawString("X", btnX + 5, btnY + 5);

        // 3. Dibujar hijos iterativamente
        for (int i = 0; i < childCount; i++) {
            if (children[i].isVisible()) {
                children[i].paint(g);
            }
        }
    }

    @Override
    public boolean handleMouse(int mx, int my, int btn) {
        if (!visible || !contains(mx, my)) return false;

        // Verificar botón de cierre
        int btnX = x + width - 23;
        int btnY = y + 5;
        if (btn == 1 && mx >= btnX && mx <= btnX + 18 && my >= btnY && my <= btnY + 18) {
            this.visible = false;
            return true;
        }

        // Verificar arrastre de la barra de título
        if (btn == 1 && my >= y && my <= y + TITLE_BAR_HEIGHT + 3) {
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
                return true; // El hijo consumió el evento
            }
        }
        
        return true; // El clic fue dentro de la ventana, lo consumimos para que no pase al fondo
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
