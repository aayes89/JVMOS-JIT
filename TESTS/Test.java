/*MIT License

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

package tests;

import java.awt.Color;
import java.awt.Graphics2D;
import java.lang.Thread;
import java.awt.Toolkit;
import java.util.Calendar;

public class Tests {

    public static void main(String[] args) {
        java.lang.System.out = new java.io.PrintStream(); // INICIALIZACIÓN VITAL
        
        Native.sys(Native.SYS_SET_COLOR, 0x00FF0000, 0, 0, 0); 
        Native.sys(Native.SYS_FILL_RECT, 0, 0, 1024, 768);
        Native.sys(Native.SYS_SET_COLOR, 0x00FFFFFF, 0, 0, 0);
        Native.sys(Native.SYS_DRAW_STRING, 50, 50, "FASE 1: Native.sys puro OK. Esperando 2 segundos...", 0);

        Thread.sleep(2000);

        Graphics2D g = new Graphics2D();
        Color background = new Color(0x000000FF);
        Color textPaint = new Color(0x00FFFF00);

        g.setColor(background);
        g.fillRect(0, 0, 1024, 768);

        g.setColor(textPaint);
        g.drawString("FASE 1-3: JIT, Heap y Graphics2D OK.", 50, 50);

        // ==========================================================
        // FASE 4: Renderizado Dinámico Baremetal 
        // ==========================================================
        java.lang.System.out.println("=====================================");
        java.lang.System.out.println("[Micro-rt] Iniciando Fase 4 en el Puerto Serie (COM1)...");
        
        long ticks = java.lang.System.currentTimeMillis();
        
        g.setColor(new Color(0x00FFFFFF)); 
        g.drawString("FASE 4: PrintStream y System.out OK.", 50, 90);
        g.drawString("Ticks actuales del sistema: ", 50, 120);
        g.drawInt((int)ticks, 330, 120); 

        java.lang.System.out.print("Fase 4 completada. Ticks: ");
        java.lang.System.out.println((int)ticks);

        // ==========================================================
        // FASE 5: Prueba de Hardware (RTC CMOS y PC Speaker)
        // ==========================================================
        int day = Calendar.get(Calendar.DAY);
        int month = Calendar.get(Calendar.MONTH);
        int year = Calendar.get(Calendar.YEAR);
        int hour = Calendar.get(Calendar.HOUR);
        int min = Calendar.get(Calendar.MINUTE);
        
        g.setColor(new Color(0x0000FF00)); 
        g.drawString("FASE 5: Calendar y Toolkit OK.", 50, 160);
        g.drawString("Reloj RTC: ", 50, 190);
        
        int px = 160; 
        px = g.drawInt(day, px, 190);
        px = g.drawChar('/', px, 190);
        px = g.drawInt(month, px, 190);
        px = g.drawChar('/', px, 190);
        px = g.drawInt(20, px, 190);
        px = g.drawInt(year, px, 190);
        px = g.drawChar(' ', px, 190);
        px = g.drawInt(hour, px, 190);
        px = g.drawChar(':', px, 190);
        px = g.drawInt(min, px, 190);

        java.lang.System.out.println("Haciendo sonar el PC Speaker...");
        
        Toolkit.getDefaultToolkit().beep(1500); 
        Thread.sleep(300); 
        Native.sys(22, 0, 0, 0, 0); 
        
        g.setColor(new Color(0x00FF8800)); 
        g.drawString("Todas las pruebas superadas! Interfaz Grafica lista.", 50, 240);
        java.lang.System.out.println("Pruebas finalizadas con exito.");

        while (true) {
            Thread.sleep(1000);
        }
    }
}
