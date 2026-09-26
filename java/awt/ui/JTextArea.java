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

public class JTextArea extends JComponent {
    
    // Búfer estático de memoria para evitar instanciación de objetos (ej. 4096 caracteres)
    private char[] textBuffer;
    private int textLength;
    private int caretIndex;
    
    // Estilos
    private int bgColor;
    private int textColor;
    private int fontWidth = 8;
    private int fontHeight = 16;
    
    // Parpadeo del cursor (se puede enlazar a los milisegundos del sistema)
    private int blinkCounter = 0;

    public JTextArea(int x, int y, int width, int height) {
        super(x, y, width, height);
        this.textBuffer = new char[4096]; // Límite de 4KB de texto plano por instancia
        this.textLength = 0;
        this.caretIndex = 0;
        this.bgColor = 0x00FFFFFF; // Blanco
        this.textColor = 0x00000000; // Negro
    }

    // Teclado
    @Override
    public boolean handleKey(int ascii) {
        if (!visible) return false;

        // Backspace (Borrar hacia atrás)
        if (ascii == 8) {
            if (caretIndex > 0) {
                // Desplazar el arreglo hacia la izquierda para sobreescribir el carácter
                for (int i = caretIndex; i < textLength; i++) {
                    textBuffer[i - 1] = textBuffer[i];
                }
                textLength--;
                caretIndex--;
            }
            return true;
        }
        
        // Enter (Salto de línea)
        if (ascii == 13) {
            insertChar('\n');
            return true;
        }
        
        // Caracteres imprimibles (ASCII estándar)
        if (ascii >= 32 && ascii <= 126) {
            insertChar((char) ascii);
            return true;
        }

        return false;
    }

    private void insertChar(char c) {
        if (textLength < textBuffer.length) {
            // Desplazar el arreglo hacia la derecha para hacer espacio si el cursor no está al final
            for (int i = textLength; i > caretIndex; i--) {
                textBuffer[i] = textBuffer[i - 1];
            }
            textBuffer[caretIndex] = c;
            textLength++;
            caretIndex++;
        }
    }

    // Mouse
    @Override
    public boolean handleMouse(int mx, int my, int btn) {
        if (!visible || !contains(mx, my)) return false;
        
        // Al hacer clic, podríamos calcular en qué fila/columna está el mouse 
        // para mover el caretIndex. Por ahora, simplemente tomamos el foco.
        return true; 
    }

    // Render
    @Override
    public void paint(Graphics2D g) {
        if (!visible) return;

        // Fondo del área de texto
        g.setColor(bgColor);
        g.fillRect(x, y, width, height);
        
        // Bordes estilo campo de texto (Hundido)
        g.setColor(0x00808080); // Sombra superior e izquierda
        g.fillRect(x, y, width, 2);
        g.fillRect(x, y, 2, height);
        g.setColor(0x00E0E0E0); // Brillo inferior y derecho
        g.fillRect(x, y + height - 2, width, 2);
        g.fillRect(x + width - 2, y, 2, height);

        // Renderizado del texto
        g.setColor(textColor);
        
        int drawX = x + 5;
        int drawY = y + 5;
        
        int cursorPixelX = drawX;
        int cursorPixelY = drawY;

        for (int i = 0; i < textLength; i++) {
            // Registrar la posición en píxeles exacta de donde debe ir el cursor
            if (i == caretIndex) {
                cursorPixelX = drawX;
                cursorPixelY = drawY;
            }

            char c = textBuffer[i];
            
            if (c == '\n') {
                drawX = x + 5;          // Retorno de carro
                drawY += fontHeight;    // Nueva línea
            } else {
                // Dibujar carácter usando la rutina base de Graphics2D
                g.drawChar(c, drawX, drawY);
                drawX += fontWidth;     // Avanzar columna
                
                // Auto-Wrap: Si choca con el borde derecho, bajar a la siguiente línea
                if (drawX + fontWidth > x + width - 5) {
                    drawX = x + 5;
                    drawY += fontHeight;
                }
            }
        }
        
        // Si el cursor está al final de todo el texto (después del último ciclo)
        if (caretIndex == textLength) {
            cursorPixelX = drawX;
            cursorPixelY = drawY;
        }

        // Dibujar el Cursor (Caret)
        blinkCounter++;
        if (blinkCounter % 60 < 30) { // Alterna visibilidad cada ~30 frames
            g.setColor(0x00000000); // Negro
            g.fillRect(cursorPixelX, cursorPixelY, 2, fontHeight);
        }
    }
    
    // Funciones auxiliares para cargar o extraer el texto al interactuar con FileSystem
    public String getText() {
        return new String(textBuffer, 0, textLength);
    }

    public void setText(String text) {
        if (text == null) text = "";
        char[] chars = text.toCharArray();
        int len = chars.length;
        if (len > textBuffer.length) len = textBuffer.length;
        
        for (int i = 0; i < len; i++) {
            textBuffer[i] = chars[i];
        }
        textLength = len;
        caretIndex = len; // Mover cursor al final del texto cargado
    }
	
	// Getter y Setters
	public char[] getTextBuffer(){
		return textBuffer;
	}
    public int getTextLength(){
		return textLength;
	}
    public int getCaretIndex(){
		return caretIndex;
	}
    public int getBgColor(){
		return bgColor;
	}
    public int getTextColor(){
		return textColor;
	}
    public int getFontWidth(){
		return fontWidth;
	}
    public int getFontHeight(){
		return fontHeight;
	}
    public int getBlinkCounter(){
		return blinkCounter;
	}
}
