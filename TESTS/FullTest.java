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

package kernel;

import kernel.Native;
import java.io.PrintStream;
import java.lang.System;
import java.awt.Graphics2D;
import java.awt.Color;
import java.awt.Toolkit;

public class FullTest {

    public static void main(String[] args) {
        
        // ==========================================================
        // 1. INICIALIZACIÓN BÁSICA Y PUERTO SERIE (Syscall 20)
        // ==========================================================
        System.out = new PrintStream();
        System.out.println("\n=== JVMOS MULTI-SYSCALL TEST SUITE ===");
        
        Graphics2D g = new Graphics2D();
        Toolkit tk = Toolkit.getDefaultToolkit();

        // ==========================================================
        // 2. PRUEBAS DE VRAM Y GRÁFICOS (Syscalls 1, 2, 3, 4, 5, 15)
        // ==========================================================
        System.out.println("[Test] Dibujando interfaz...");
        
        // a) Rellenar el fondo de Azul Oscuro (fill_rect)
        g.setColor(new Color(0, 0, 128)); 
        g.fillRect(0, 0, 1024, 768);

        // b) Dibujar una "ventana" Blanca (fill_rect)
        g.setColor(new Color(255, 255, 255));
        g.fillRect(100, 100, 800, 500);

        // c) Dibujar un borde Rojo para la ventana (draw_rect)
        g.setColor(new Color(255, 0, 0));
        g.drawRect(100, 100, 800, 500);

        // d) Dibujar una línea diagonal decorativa (draw_line)
        g.setColor(new Color(200, 200, 200)); // Gris claro
        g.drawLine(101, 101, 899, 599);

        // e) Renderizar texto (draw_string)
        g.setColor(new Color(0, 0, 0)); // Texto negro
        g.drawString("Prueba de multiples Syscalls en JVMOS", 120, 130);

        // ==========================================================
        // 3. PRUEBAS DE RELOJ Y TICKS (Syscalls 13 y 18)
        // ==========================================================
        System.out.println("[Test] Consultando reloj RTC y PIT...");
        
        // Consultar Ticks del procesador
        int ticks = Native.sys(Native.SYS_GET_TICKS, 0, 0, 0, 0);
        g.drawString("Ticks de CPU al iniciar: ", 120, 160);
        g.drawInt(ticks, 310, 160);

        // Consultar Reloj RTC (Hora y Minuto)
        int hora = Native.sys(Native.SYS_GET_TIME, 2, 0, 0, 0); // 2 = horas
        int min  = Native.sys(Native.SYS_GET_TIME, 1, 0, 0, 0); // 1 = minutos
        g.drawString("Hora CMOS (RTC): ", 120, 190);
        g.drawInt(hora, 260, 190);
        g.drawString(" : ", 280, 190);
        g.drawInt(min, 310, 190);

        // ==========================================================
        // 4. PRUEBA DE SONIDO Y RETARDO (Syscalls 12 y 22)
        // ==========================================================
        System.out.println("[Test] Reproduciendo sonido...");
        g.setColor(new Color(0, 0, 255)); // Azul
        g.drawString("Reproduciendo Beep (1000Hz)...", 120, 240);
        
        tk.beep(1000); // Activar PC Speaker a 1000Hz
        
        // Suspender el procesador por 800 milisegundos (sys_sleep)
        Native.sys(Native.SYS_SLEEP, 800, 0, 0, 0); 
        
        tk.beep(0); // Apagar el sonido

        // ==========================================================
        // 5. APAGADO DEL SISTEMA (Syscall 17)
        // ==========================================================
        g.setColor(new Color(0, 128, 0)); // Verde
        g.drawString("Pruebas completadas con exito.", 120, 290);
        g.drawString("El sistema se apagara en 3 segundos...", 120, 320);
        
        System.out.println("Apagando en 3 segundos...");
        Native.sys(Native.SYS_SLEEP, 3000, 0, 0, 0);
        Native.sys(Native.SYS_EXIT, 0, 0, 0, 0); 
    }
}
