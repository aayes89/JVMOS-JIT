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

public abstract class JComponent {
    protected int x;
    protected int y;
    protected int width;
    protected int height;
    protected boolean visible;
    
    // Jerarquía
    protected JComponent parent;

	// Constructor
    public JComponent(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.visible = true;
    }

    public abstract void paint(Graphics2D g);

    // Retorna true si el evento fue consumido por este componente
    public boolean handleMouse(int mx, int my, int btn) {
        return false; 
    }

    public boolean handleKey(int key) {
        return false;
    }

    public boolean contains(int mx, int my) {
        int absX = getAbsoluteX();
        int absY = getAbsoluteY();
        return (mx >= absX && mx <= absX + width && my >= absY && my <= absY + height);
    }
    
	// Getters y Setters
	public int getAbsoluteX(){
		// Posición actual o centro de la pantalla
		return (parent == null) ? x + parent.getAbsoluteX(): (width/2);
	}
	public int getAbsoluteY(){
		// Posición actual o centro de la pantalla
		return (parent == null) ? y + parent.getAbsoluteY(): (height/2);
	}
    public void setVisible(boolean v) {
		this.visible = v; 
	}
    public boolean isVisible() { 
		return visible; 
	}
	public int getX(){
		return x;
	}
	public void setX(int x){
		this.x = x;
	}
    public int getY(){
		return y;
	}
	public void setY(int y){
		this.y = y;
	}
    public int getWidth(){
		return width;
	}
	public void setWidth(int width){
		this.width = width;
	}
    public int getHeight(){
		return height;
	}
	public void setHeight(int height){
		this.height = height;
	}
    public JComponent getParent(){
		return parent;
	}	
}
