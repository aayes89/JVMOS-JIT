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

import java.io.DiskIO;
import java.io.FileSystem;
import java.io.PrintStream;
import java.net.NetworkShell;
import java.awt.Color;
import java.awt.Graphics2D;
import java.lang.Thread;
import kernel.Native;
import kernel.SystemShell;

public class Boot {     
    // ==========================================
    // Sistema de archivos y hardware
    // ==========================================
    private static DiskIO disk;
    private static FileSystem fs;
    private static Portapapeles portapapeles;
    private static Graphics2D g;  

    public static void main(String[] args) {
        //Native.sys(1, 0x0000FF00, 0, 0, 0); // verde
        Native.sys(5, 20, 20, "INICIANDO JVMOS-JIT...", 0); 

        java.lang.System.out = new PrintStream();
        System.out.println("[Boot] Inicializando subsistemas Micro-RT de JVMOS-JIT...");
        g = new Graphics2D();
        
        portapapeles = new Portapapeles();

        // inicializar Teclado
        System.initKeyboard();             

        // inicializar BMFS (Sistema de archivos)
        initFS();                   
        
        // Inicializar redes        
        NetworkShell.init(g); 
		
		// Prueba del GC
		testGarbageCollector();
		
        Native.sys(12,3000,0,0,0); // sleep 3s      
        
        System.out.println("[GRAPHICS] Inicializando subsistema grafico...");
        System.out.println("[HARDWARE] Inicializando controladores I/O...");
        System.out.println("[JVMOS-JIT] Iniciando entorno interactivo...");
        
        dramaticBIOS();
        
        // ==========================================
        // Cedo control al shell del sistema
        // ==========================================
        SystemShell.init(fs, g, portapapeles);
        SystemShell.startLoop();
    }
	
	public static void testGarbageCollector() {
        System.out.println("[GC Test] Creando 50,000 arreglos (Aprox 200MB) para forzar el Recolector...");
        
        for (int i = 0; i < 50000; i++) {
            // Cada arreglo ocupa unos 4KB. Multiplicado por 50,000 excede tu RAM de 128MB.
            // Si el GC no los detectara como "muertos" y los reciclara, el SO crashearía aquí.
            int[] basura = new int[1024]; 
            basura[0] = i; 
        }
        
        System.out.println("[GC Test] Prueba superada! La memoria fue reciclada sin crashear.");
    }
    
    public static void initFS() {
        disk = new DiskIO();
        fs = new FileSystem(disk);
        if (!fs.mount()) {
            // Formateo Rápido (false) evita congelar el Kernel 20 minutos por IO-blocking
            fs.format(40960, false); 
        }
        System.out.println("Sistema de Archivos BMFS inicializado!");
    }

    public static void dramaticBIOS() {		
        g.clearScreen(); 
        g.setColor(Color.GREEN); 
        g.drawString("JVMOS BIOS [v2.5]", 20, 25); 
        g.drawString("=============================================", 20, 45);
        g.drawString("[ OK ]", 20, 75); g.setColor(Color.WHITE); g.drawString("Verificando CPU x86 [Protected Mode 32-Bit]...", 90, 75);
        g.setColor(Color.GREEN); 
        g.drawString("[ OK ]", 20, 95); g.setColor(Color.WHITE); g.drawString("Memoria RAM Detectada: [128MB]", 90, 95);
        g.setColor(Color.GREEN); 
        g.drawString("[ OK ]", 20, 115); g.setColor(Color.WHITE); g.drawString("Cargando Driver PS/2 Keyboard [LATAM ISO Map]", 90, 115);
        g.setColor(Color.GREEN); 
        g.drawString("[ OK ]", 20, 135); g.setColor(Color.WHITE); g.drawString("Cargando Driver Mouse i8042 [240 DPI]", 90, 135);
        g.setColor(Color.GREEN); 
        g.drawString("[ OK ]", 20, 155); g.setColor(Color.WHITE); g.drawString("Montando Sistema de Archivos JVMFS [ATA IDE LBA28]", 90, 155);
        g.setColor(Color.GREEN); 
        g.drawString("[ OK ]", 20, 175); g.setColor(Color.WHITE); g.drawString("Modo de Video VBE VESA [1024x768 @ 32bpp]", 90, 175);
        g.setColor(Color.GREEN); 
        g.drawString("=============================================", 20, 45);
        g.drawString("SISTEMA LISTO. Iniciando Shell interactivo...", 20, 205);
        try { 
            Thread.sleep(2000);
        } catch(Exception e) {
            // System.err.println(e.getMessage());          
        } 
        g.clearScreen();
    }
}
