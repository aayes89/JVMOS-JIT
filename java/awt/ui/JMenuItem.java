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

public class JMenuItem extends JComponent {
    private String text;
    private boolean isHovered;
    private ActionCallback action;
	private int actionId;

    public JMenuItem(String text, ActionCallback action, int actionId) {
        super(0, 0, 180, 24); // Dimensiones estándar para un ítem de menú
        this.text = text;
        this.action = action;
		this.actionId = actionId;
        this.isHovered = false;		
    }

    @Override
    public void paint(Graphics2D g) {
        if (!visible) return;

        // Fondo (Azul clásico si está seleccionado, gris si no)
        g.setColor(isHovered ? 0x00000080 : 0x00C0C0C0);
        g.fillRect(x, y, width, height);

        // Texto (Blanco si está seleccionado, negro si no)
        g.setColor(isHovered ? 0x00FFFFFF : 0x00000000);
        g.drawString(text, x + 15, y + 16);
    }

    @Override
    public boolean handleMouse(int mx, int my, int btn) {
        if (!visible) return false;        
        isHovered = contains(mx, my);
        if (isHovered && btn == 1) {
            if (action != null) action.execute(actionId);
            return true;
        }

        return isHovered; // Consume el evento si el ratón está encima
    }
}
