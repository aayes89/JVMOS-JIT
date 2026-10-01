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

    // Constructor para boton
    public JButton(int x, int y, int width, int height, ActionCallback action, int actionId) {
        super(x, y, width, height);
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.text = "";         // Ya no se pasa al constructor sino por los setter hasta corregir problema de memoria
        this.bgColor = Color.LIGHT_GRAY; // Gris claro por defecto al fondo
        this.textColor = Color.BLACK; // Negro por defecto
        this.action = action;
		this.actionId = actionId;
        this.isPressed = false;
    }

    @Override
    public void paint(Graphics2D g) {
        
        //if (!visible) return;		
		
		// Fondo oscurecido si está presionado
        g.setColor(isPressed ? Color.GRAY : bgColor);
        g.fillRect(this.getX(), getY(), width, height);        
        
        // Bordes 3D simulados
        
        if (!isPressed) {
            g.setColor(Color.WHITE); // Brillo superior e izquierdo
            g.fillRect(getX(), getY(), width, 2);
            g.fillRect(getX(), getY(), 2, height);
            
            g.setColor(0x00555555); // Sombra inferior y derecha
            g.fillRect(getX(), getY() + height - 2, width, 2);
            g.fillRect(getX() + width - 2, y, 2, height);
        }

        // Centrado aproximado del texto        
        int textX = getX() + (width - (text.length() * 8)) / 2;
        int textY = getY() + (height / 2) + 4; 
        g.setColor(Color.RED);
        g.drawString("AbsX: "+ textX, 900,100);
        g.drawString("AbsY: " + textY, 900,110);
        g.drawString("Texto: "+ text,900, 80);
              		
		g.setColor(textColor);
        g.drawString(text, textX, textY);
    }

    @Override
    public boolean handleMouse(int mx, int my, int btn) {
        if (!visible) return false;

        boolean isInside = contains(mx, my);
        
        if(isInside){
            if (btn == 1 || btn == 2) { // clic izquierdo o derecho?
                isPressed = true;
                action.execute(actionId); // Disparar la acción configurada para el boton
            } 
        }else if(!isInside){
            // Cancelar estado de boton presionado si no está sobre él y han sido presionados los botones del mouse
            if(btn == 1 || btn == 2){
                isPressed = false;
                // No hay acción que disparar (no hacer nada)
            }
        }
        
        // Consumir el evento siempre
        return true; 
    }
    @Override
    public boolean handleKey(int key){
        // No hacer nada aquí
        return key!=0?true:false;
    }

    public String getText(){
        return text;
    }
    public void setText(String txt){
        this.text = txt;
    }
}
