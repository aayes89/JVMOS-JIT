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

public class JMenuItem extends JComponent {
    private String text;
    private boolean isHovered;
    private ActionCallback action;
	private int actionId;

    public JMenuItem(String text, ActionCallback action, int actionId) {
		// Dimensiones estándar para un ítem de menú
        super(0, 0, 180, 24); 
		this.width = 180;
		this.height = 24;
        this.text = text;
        this.action = action;
		this.actionId = actionId;
        this.isHovered = false;		
    }

    @Override
    public void paint(Graphics2D g) {
        if (!visible) return;
        
        int absX = getX();
        int absY = getY();
        
        // 0xFF000080 = Azul Marino, 0xFFC0C0C0 = Gris Claro
        g.setColor(isHovered ? 0xFF000080 : 0xFFC0C0C0);
        g.fillRect(absX, absY, width, height);
         
        // 0xFFFFFFFF = Blanco, 0xFF000000 = Negro
        g.setColor(isHovered ? 0xFFFFFFFF : 0xFF000000);
        g.drawString(text, absX + 15, absY + 16);
    }

    @Override
	public boolean handleMouse(int mx, int my, int btn) {
		if (!visible) return false;     
		isHovered = contains(mx, my);
		
		// Ejecutar con clic izquierdo o derecho
		if (isHovered && (btn == 1 || btn == 2)) {
			if (action != null) action.execute(actionId);
			return true;
		}
		return isHovered; 
	}
    @Override
    public boolean handleKey(int key){
        // No hacer nada aquí
        return key!=0?true:false;
    }
}
