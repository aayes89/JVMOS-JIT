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

public class JButton extends JComponent {
    private String text;
    private int bgColor;
    private int textColor;
    private boolean isPressed;
    private ActionCallback action;
	private int actionId;

    public JButton(String text, int x, int y, int width, int height, ActionCallback action, int actionId) {
        super(x, y, width, height);
        this.text = text;
        this.bgColor = Color.LIGHT_GRAY; // Gris claro por defecto al fondo
        this.textColor = Color.BLACK; // Negro por defecto
        this.action = action;
		this.actionId = actionId;
        this.isPressed = false;
    }

    @Override
    public void paint(Graphics2D g) {
        if (!visible) return;		
        
		int absX = getAbsoluteX();
        int absY = getAbsoluteY();
		
		// Fondo oscurecido si está presionado
        g.setColor(isPressed ? Color.GRAY : bgColor);
        g.fillRect(absX, absY, width, height);        
        
        // Bordes 3D simulados
        if (!isPressed) {
            g.setColor(Color.WHITE); // Brillo superior e izquierdo
            g.fillRect(absX, absY, width, 2);
            g.fillRect(absX, absY, 2, height);
            
            g.setColor(0x00555555); // Sombra inferior y derecha
            g.fillRect(absX, absY + height - 2, width, 2);
            g.fillRect(absX + width - 2, absY, 2, height);
        }

        // Centrado aproximado del texto        
        int textX = absX + (width - (text.length() * 8)) / 2;
        int textY = absY + (height / 2) + 4; 
        
        // Desplazamiento visual al presionar
        if (isPressed) { 
            textX++; 
            textY++; 
        }         		
		g.setColor(textColor);
        g.drawString(text, textX, textY);
    }

    @Override
    public boolean handleMouse(int mx, int my, int btn) {
        if (!visible) return false;

        boolean isInside = contains(mx, my);

        // Si el usuario arrastra el mouse fuera del botón, cancelar el estado presionado
        if (!isInside) {
            isPressed = false;
            return false;
        }

        if (btn == 1) {
            isPressed = true;
            return true; // Consume el evento
        } else if (btn == 0 && isPressed) {
            isPressed = false;
            if (action != null) {
                action.execute(actionId); // Disparar la acción configurada
            }
            return true;
        }
        
        // Si está dentro de los límites, consume el evento para que no afecte elementos detrás
        return true; 
    }
}
