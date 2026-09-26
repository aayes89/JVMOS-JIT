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

public class JPanel extends JComponent {
    private int bgColor;
    private JComponent[] children;
    private int childCount;
    private static final int MAX_CHILDREN = 32;

    public JPanel(int x, int y, int width, int height, int bgColor) {
        super(x, y, width, height);
        this.bgColor = bgColor;
        this.children = new JComponent[MAX_CHILDREN];
        this.childCount = 0;
    }

    public void add(JComponent c) {
        if (childCount < MAX_CHILDREN) {
            c.parent = this;
            c.x += this.x;
            c.y += this.y;
            children[childCount++] = c;
        }
    }

    @Override
    public void paint(Graphics2D g) {
        if (!visible) return;
        
        // Dibujar el fondo del panel
        g.setColor(bgColor);
        g.fillRect(x, y, width, height);

        // Dibujar hijos
        for (int i = 0; i < childCount; i++) {
            if (children[i].isVisible()) {
                children[i].paint(g);
            }
        }
    }

    @Override
    public boolean handleMouse(int mx, int my, int btn) {
        if (!visible || !contains(mx, my)) return false;

        for (int i = childCount - 1; i >= 0; i--) {
            if (children[i].handleMouse(mx, my, btn)) {
                return true;
            }
        }
        return true;
    }
}
