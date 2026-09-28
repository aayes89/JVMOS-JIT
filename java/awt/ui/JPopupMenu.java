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

// Tomado de implementación original y adaptado a JVMOS-JIT
public class JPopupMenu extends JComponent {
    private JMenuItem[] items;
    private int itemCount;
    private static final int MAX_ITEMS = 16;

    public JPopupMenu() {
        super(0, 0, 184, 4);
		this.width = 184;
		this.height = 4;
        this.items = new JMenuItem[MAX_ITEMS];
        this.itemCount = 0;
        this.visible = false;
    }

    public void add(JMenuItem item) {
		if (itemCount < MAX_ITEMS && item != null) {
            item.parent = this;                        
            item.setX(2); 
            item.setY(2 + (itemCount * 24));
            item.setWidth(this.width - 4);            
            items[itemCount++] = item;			
            this.height = (itemCount * 24) + 4;
        }
    }

    // Posiciona el menú y lo hace visible
    public void show(int screenX, int screenY) {
        this.x = screenX;
        this.y = screenY;		
        this.visible = true;
    }

    @Override
    public void paint(Graphics2D g) {
        if (!visible) return;		
		
        int absX = getAbsoluteX();
        int absY = getAbsoluteY();
        
        g.setColor(0xFFFFFFFF); 
        g.fillRect(absX, absY, width, 2); 
        g.fillRect(absX, absY, 2, height); 
        
        g.setColor(0xFF555555); // Gris oscuro OPACO
        g.fillRect(absX, absY + height - 2, width, 2); 
        g.fillRect(absX + width - 2, absY, 2, height); 
		
        g.setColor(0xFFC0C0C0);
        g.fillRect(absX + 2, absY + 2, width - 4, height - 4);

        for (int i = 0; i < itemCount; i++) {
            items[i].paint(g);
        }
    }
    

    @Override
	public boolean handleMouse(int mx, int my, int btn) {
		if (!visible) return false;
		
		boolean itemHandled = false;
		for (int i = 0; i < itemCount; i++) {
			if (items[i].handleMouse(mx, my, btn)) {
				itemHandled = true;
				// Ocultar al ejecutar
				if (btn == 1 || btn == 2) this.visible = false;                
			}
		}        
		return contains(mx, my) || itemHandled; 
	}
}
