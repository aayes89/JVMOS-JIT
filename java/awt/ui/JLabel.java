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

public class JLabel extends JComponent {
    private String text;
    private int textColor;

    public JLabel(String text, int x, int y, int textColor) {
        // Asume un ancho estimado basado mi fuente estándar de 8x16 píxeles
        super(x, y, text.length() * 8, 16); 
        this.text = text;
        this.textColor = textColor;
    }

    public void setText(String text) { 
        this.text = text; 
    }

    @Override
    public void paint(Graphics2D g) {
        if (!visible || text == null) return;
        g.setColor(textColor);
        // El offset en Y ajusta la línea base de la fuente
        g.drawString(text, x, y + 12); 
    }
}
