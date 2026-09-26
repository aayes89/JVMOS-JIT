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

// Tomado de implementación original y adaptado a JVMOS-JIT
public class JPopupMenu extends JComponent {
    private JMenuItem[] items;
    private int itemCount;
    private static final int MAX_ITEMS = 16;

    public JPopupMenu() {
        super(0, 0, 184, 4); // El alto crece dinámicamente al agregar ítems
        this.items = new JMenuItem[MAX_ITEMS];
        this.itemCount = 0;
        this.visible = false;
    }

    public void add(JMenuItem item) {
        if (itemCount < MAX_ITEMS) {
            item.parent = this;
            item.x = this.x + 2; 
            item.y = this.y + 2 + (itemCount * 24);
            item.width = this.width - 4;
            items[itemCount++] = item;
            this.height = (itemCount * 24) + 4; // Ajustar la altura del menú
        }
    }

    // Posiciona el menú y lo hace visible
    public void show(int screenX, int screenY) {
        this.x = screenX;
        this.y = screenY;
        
        // Actualizar las posiciones relativas de los hijos
        for (int i = 0; i < itemCount; i++) {
            items[i].x = this.x + 2;
            items[i].y = this.y + 2 + (i * 24);
        }
        this.visible = true;
    }

    @Override
    public void paint(Graphics2D g) {
        if (!visible) return;

        // Borde exterior 3D
        g.setColor(0x00FFFFFF); g.fillRect(x, y, width, 2); // Arriba
        g.fillRect(x, y, 2, height); // Izquierda
        g.setColor(0x00555555); g.fillRect(x, y + height - 2, width, 2); // Abajo
        g.fillRect(x + width - 2, y, 2, height); // Derecha

        // Fondo del contenedor
        g.setColor(0x00C0C0C0);
        g.fillRect(x + 2, y + 2, width - 4, height - 4);

        for (int i = 0; i < itemCount; i++) {
            items[i].paint(g);
        }
    }

    @Override
    public boolean handleMouse(int mx, int my, int btn) {
        if (!visible) return false;

        boolean clickedInside = contains(mx, my);

        for (int i = 0; i < itemCount; i++) {
            if (items[i].handleMouse(mx, my, btn)) {
                if (btn == 1) this.visible = false; // Auto-ocultar al hacer clic en una opción
                return true;
            }
        }

        // Si el usuario hace clic fuera del menú, este se cierra automáticamente
        if (btn == 1 && !clickedInside) {
            this.visible = false;
        }

        return clickedInside; // Bloquea los clics para que no atraviesen el menú
    }
}
