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

import java.lang.System;
import java.awt.Graphics2D;
import java.awt.Color;
import java.io.FileSystem;
import java.io.File;
import java.net.NetworkShell;
import java.net.Netcat;
import java.util.Calendar;
import java.lang.Thread;
import java.apps.JTunelScope;
import java.apps.RenderDemos;
import java.awt.g3d.Demo3D;
import kernel.Native;
import kernel.UI;

public class SystemShell {
	// Clases de sistema de archivo y hardware general
	private static Graphics2D g;
	private static FileSystem fs;
    private static Portapapeles portapapeles;

    // Estado visual del Shell
    private static int cursorX = 85;
    private static int cursorY = 80;

    // Pila de navegación para soportar 'cd ..' en múltiples niveles
    private static int[] lbaStack = new int[32];
    private static int lbaDepth = 0;
    
    private static int currentDirLba;
    private static int parentDirLba;
    private static String currentDirPath;

    // Función constructor para inicializar variables
    public static void init(FileSystem fileSys, Graphics2D graphics, Portapapeles clip) {
        fs = fileSys;
        g = graphics;
        portapapeles = clip;
        
        currentDirLba = FileSystem.ROOT_DIR_LBA;
        parentDirLba = FileSystem.ROOT_DIR_LBA;
        lbaStack[0] = currentDirLba;
        lbaDepth = 0;
        currentDirPath = "/";
    }

    // Ciclo principal del shell
	public static void startLoop() {
        g.clearScreen();
        shellHeader();

        int lastKey = 0, cmdLen = 0;
        int[] cmdBuffer = new int[128];
        
        drawPrompt();

        while (true) {
            int asciiChar = Native.sys(Native.SYS_READ_KEYBOARD, 0, 0, 0, 0);

            if (asciiChar != 0 && asciiChar != lastKey) {
                if (asciiChar == 13) { // ENTER
                    cursorY += 25;
                    
                    if (cmdLen > 0) {
                        processCommand(cmdLen, cmdBuffer);
                    }

                    for (int i = 0; i < cmdLen; i++){ 
                        cmdBuffer[i] = 0;
                    }
                    cmdLen = 0;

                    if (cursorY > 700) { 
                        // Syscall 28: Mover VRAM hacia arriba 25 píxeles (la altura de tus líneas)
                        Native.sys(Native.SYS_SCROLL_UP, 25, 0, 0, 0); 
                        cursorY -= 25; // El cursor retrocede en Y para quedarse en la misma línea visual
                    }
                    drawPrompt();
                    
                } else if (asciiChar == 8) { // BACKSPACE
                    int limitX = 90 + (currentDirPath.length() * 10) + 20;
                    if (cmdLen > 0 && cursorX > limitX) {
                        cmdLen--;
                        cmdBuffer[cmdLen] = 0;
                        cursorX -= 10;
                        g.setColor(Color.BLACK);
                        g.fillRect(cursorX, cursorY, 12, 20);
                    }
                } else if (asciiChar >= 32 && asciiChar <= 165) { // CARACTERES
                    if (cmdLen < 60) {
                        cmdBuffer[cmdLen] = asciiChar; cmdLen++;
                        g.setColor(Color.WHITE); g.drawChar((char)asciiChar, cursorX, cursorY);
                        cursorX += 10;
                    }
                }
                lastKey = asciiChar;
            } else if (asciiChar == 0) {
                lastKey = 0;
            }
            try {
                Thread.sleep(1);
            } catch(Exception e) {
                // No capturar nada aquí
            }
        }
    }

    // Procesar comandos en el shell
	public static void processCommand(int cmdLen, int[] cmdBuffer) {
        int spaceIdx = -1;
        for (int i = 0; i < cmdLen; i++) {
            if (cmdBuffer[i] == ' ') {
                spaceIdx = i;
                break;
            }
        }
        
        String cmd = "";
        String arg = "";
        
        if (spaceIdx == -1) {
            byte[] cb = new byte[cmdLen];
            for(int i=0; i<cmdLen; i++) {
                cb[i] = (byte)cmdBuffer[i];
            }
            cmd = new String(cb);
        } else {
            byte[] cb = new byte[spaceIdx];
            for(int i=0; i<spaceIdx; i++) {
                cb[i] = (byte)cmdBuffer[i];
            }
            cmd = new String(cb);
            
            byte[] ab = new byte[cmdLen - spaceIdx - 1];
            for(int i=spaceIdx+1; i<cmdLen; i++) {
                ab[i - spaceIdx - 1] = (byte)cmdBuffer[i];
            }
            arg = new String(ab);
        }

        // ================= RUTEO DE COMANDOS =================
        if (cmd.equals("ls") || cmd.equals("dir")) {
            File[] files = fs.listFiles(currentDirLba, currentDirPath);
            int totalFiles = fs.getCantFiles();
            
            if (totalFiles == 0) {
                g.setColor(Color.WHITE); printLine("Directorio vacio.");
            } else {
                g.setColor(Color.CYAN);
                g.drawString("NOMBRE", 20, cursorY);
                g.drawString("TIPO", 250, cursorY);
                g.drawString("SIZE(B)", 350, cursorY);
                cursorY += 25;
                
                for (int i = 0; i < totalFiles; i++) {
                    File f = files[i];
                    if (f != null && f.exists() && f.getName().length() > 0 && f.getName().charAt(0) >= 32) {
                        g.setColor(Color.WHITE);
                        g.drawString(f.getName(), 20, cursorY);
                        
                        if(f.isDirectory()) {
                            g.drawString("<DIR>", 250, cursorY);
                        } else {
                            g.drawString("<" + f.getExtension() + ">", 250, cursorY);
                        }
                        
                        g.drawInt(f.length(), 350, cursorY);                        
                        cursorY += 25;
                        if (cursorY > 700) { 
                            // Syscall 28: Mover VRAM hacia arriba 25 píxeles (la altura de tus líneas)
                            Native.sys(Native.SYS_SCROLL_UP, 25, 0, 0, 0); 
                            cursorY -= 25; // El cursor retrocede en Y para quedarse en la misma línea visual
                        }
                    }
                }
            }
        } 
        else if (cmd.equals("cd")) {
            if(arg.length() < 1) {
                g.setColor(Color.RED); printLine("Uso: cd <directorio> o cd ..");
            } else if(arg.equals("..")) {
                if(lbaDepth > 0) {
                    lbaDepth--;
                    currentDirLba = lbaStack[lbaDepth];
                    if (lbaDepth > 0) parentDirLba = lbaStack[lbaDepth - 1];
                    else parentDirLba = FileSystem.ROOT_DIR_LBA;
                    
                    int lastSlash = currentDirPath.lastIndexOf('/');
                    if (lastSlash == 0) {
                        currentDirPath = "/";
                    } else if (lastSlash > 0) {
                        byte[] pBytes = currentDirPath.getBytes();
                        byte[] newBytes = new byte[lastSlash];
                        java.lang.System.arraycopy(pBytes, 0, newBytes, 0, lastSlash);
                        currentDirPath = new String(newBytes);
                    }
                }
            } else {
                File target = fs.lookup(arg, currentDirLba, currentDirPath);
                if (target != null && target.isDirectory()) {
                    parentDirLba = currentDirLba;
                    currentDirLba = target.getStartLBA();
                    
                    if (lbaDepth < 31) {
                        lbaDepth++;
                        lbaStack[lbaDepth] = currentDirLba;
                    }
                    
                    byte[] pB = currentDirPath.getBytes();
                    byte[] tB = target.getName().getBytes();
                    if (currentDirPath.equals("/")) {
                        byte[] nB = new byte[1 + tB.length];
                        nB[0] = '/';
                        java.lang.System.arraycopy(tB, 0, nB, 1, tB.length);
                        currentDirPath = new String(nB);
                    } else {
                        byte[] nB = new byte[pB.length + 1 + tB.length];
                        java.lang.System.arraycopy(pB, 0, nB, 0, pB.length);
                        nB[pB.length] = '/';
                        java.lang.System.arraycopy(tB, 0, nB, pB.length + 1, tB.length);
                        currentDirPath = new String(nB);
                    }
                } else {
                    g.setColor(Color.RED); printLine("Directorio no encontrado.");
                }
            }
        }
        else if (cmd.equals("write")) {
            int space = arg.indexOf(' ');
            if (space == -1) {
                g.setColor(Color.RED); printLine("Uso: write <archivo.txt> <contenido>");
            } else {
                byte[] argBytes = arg.getBytes();
                
                // Extraer el nombre del archivo
                byte[] nameB = new byte[space];
                for(int i = 0; i < space; i++) {
                    nameB[i] = argBytes[i];
                }
                String fileName = new String(nameB);
                
                // Extraer el contenido
                int txtLen = argBytes.length - space - 1;
                byte[] txtB = new byte[txtLen];
                for(int i = 0; i < txtLen; i++){
                    txtB[i] = argBytes[space + 1 + i];
                }
                
                if (fs.writeFile(fileName, txtB, currentDirLba)) {
                    g.setColor(Color.GREEN); printLine("Archivo guardado: " + fileName);
                } else {
                    g.setColor(Color.RED); printLine("Error al guardar archivo.");
                }
            }
        }
        else if (cmd.equals("cat")) {
            if (arg.length() < 1) { 
                g.setColor(Color.RED); printLine("Uso: cat <archivo>"); 
            } else {
                File f = fs.lookup(arg, currentDirLba, currentDirPath);
                if (f != null && f.isFile()) {
                    byte[] data = fs.readFile(f);
                    if (data != null && data.length > 0) {
                        g.setColor(Color.WHITE); printLine(new String(data));
                    } else {
                        g.setColor(Color.YELLOW); printLine("[Archivo vacio]");
                    }
                } else {
                    g.setColor(Color.RED); printLine("Archivo no encontrado o es directorio.");
                }
            }
        }
        else if (cmd.equals("mkdir")) {
            if (arg.length() < 1) { 
                g.setColor(Color.RED);
                printLine("Uso: mkdir <nombre>"); 
                return; 
            }
            if (fs.mkdir(arg, currentDirLba)) {
                g.setColor(Color.GREEN); 
                printLine("Directorio creado."); 
            }
            else { 
                g.setColor(Color.RED);
                printLine("Error al crear directorio."); 
            }
        }
        else if (cmd.equals("rm")) {
            if (arg.length() < 1) {
                g.setColor(Color.RED);
                printLine("Uso: rm <nombre>"); 
                return; 
            }
            File target = fs.lookup(arg, currentDirLba, currentDirPath);
            if (target != null) {
                if (fs.delete(target, currentDirLba)) { 
                    g.setColor(Color.GREEN); 
                    printLine("Eliminado."); 
                }
                else { 
                    g.setColor(Color.RED); 
                    printLine("Error al eliminar."); 
                }
            } else {
                g.setColor(Color.RED);
                printLine("Archivo no encontrado.");
            }
        }
        else if (cmd.equals("cp")) {
            if (arg.length() < 1) { 
                g.setColor(Color.RED);
                printLine("Uso: cp <archivo>"); 
                return; 
            }
            File source = fs.lookup(arg, currentDirLba, currentDirPath);
            if (source == null || !source.isFile()) {
                g.setColor(Color.RED); 
                printLine("Archivo origen invalido.");
            } else if (portapapeles.capturarArchivo(source, currentDirLba, false)) {
                g.setColor(Color.GREEN); 
                printLine("Copiado al portapapeles.");
            } else {
                g.setColor(Color.RED); 
                printLine("No se pudo copiar.");
            }
        }
        else if (cmd.equals("mv")) {
            if (arg.length() < 1) { 
                g.setColor(Color.RED); 
                printLine("Uso: mv <archivo>"); 
                return; 
            }
            File source = fs.lookup(arg, currentDirLba, currentDirPath);
            if (source == null || !source.isFile()) {
                g.setColor(Color.RED); 
                printLine("Archivo origen invalido.");
            } else if (portapapeles.capturarArchivo(source, currentDirLba, true)) {
                g.setColor(Color.GREEN); 
                printLine("Cortado al portapapeles.");
            } else {
                g.setColor(Color.RED); 
                printLine("No se pudo cortar.");
            }
        }
        else if (cmd.equals("paste")) {
            if (!portapapeles.tieneArchivo()) {
                g.setColor(Color.RED); 
                printLine("Portapapeles vacio."); 
                return;
            }
            File clipFile = portapapeles.obtenerArchivo();
            int sourceDirLba = portapapeles.obtenerLba();
            File source = fs.lookup(clipFile.getName(), sourceDirLba, ""); 
            
            if (source != null) {
                byte[] data = fs.readFile(source);
                String destName = source.getName();
                
                if (fs.lookup(destName, currentDirLba, currentDirPath) != null) {
                    destName = generateSafeCopyName(source.getName());
                }
                
                if (fs.writeFile(destName, data, currentDirLba)) {
                    if (portapapeles.esCortar()) {
                        fs.delete(source, sourceDirLba);
                        portapapeles.limpiarArchivo();
                        g.setColor(Color.GREEN);
                        printLine("Movido exitosamente.");
                    } else {
                        g.setColor(Color.GREEN);
                        printLine("Pegado exitoso.");
                    }
                } else { 
                    g.setColor(Color.RED);
                    printLine("Error al escribir archivo."); 
                }
            } else { 
                g.setColor(Color.RED);
                printLine("Archivo original perdido.");
            }
        }
        else if (cmd.equals("format")) {
            g.setColor(Color.YELLOW); 
            printLine("Formateando disco (Wipe rapido)...");
            fs.format(40960, false);
            currentDirLba = FileSystem.ROOT_DIR_LBA;
            parentDirLba = FileSystem.ROOT_DIR_LBA;
            lbaStack[0] = currentDirLba;
            lbaDepth = 0;
            currentDirPath = "/";
            portapapeles.limpiar();
            g.setColor(Color.GREEN); 
            printLine("Disco formateado.");
        }
        else if (cmd.equals("run") || cmd.equals("java")) {
            if (arg.length() < 1) { 
                g.setColor(Color.RED); 
                printLine("Uso: run <archivo.class>"); 
                return;
            }
            if(arg.indexOf(".class") != -1) {
                g.setColor(Color.GREEN); 
                printLine("Ejecutando " + arg + "...");
                fs.execute(arg, currentDirLba, currentDirPath);
            } else {
                g.setColor(Color.RED); 
                printLine("Solo ejecuta .class");
            }
        }
        else if (cmd.equals("nc")) {
            String[] lineas = Netcat.execute(arg, fs, currentDirLba);
            g.setColor(Color.WHITE);
            for (int i = 0; i < lineas.length; i++) {
                printLine(lineas[i]);
            }
        }
        else if (cmd.equals("cube")) { 
            RenderDemos.runCubeWireframe();
            g.clearScreen(); 
            cursorY = 40; 
        }
        else if(cmd.equals("cube2d")){
            RenderDemos.runCube2D();
            g.clearScreen();
            cursorY = 40;
        }else if(cmd.equals("cube3d")){
            RenderDemos.runCubeMesh3D();            
            g.clearScreen();
            cursorY = 40;
        }else if(cmd.equals("demo3d")){
            Demo3D.run(g);
            g.clearScreen();
            cursorY = 40;
        }
        else if (cmd.equals("startx")) {
           runStartX(); 
        }
        else if (cmd.equals("time")) { 
            showTime(cursorY); 
            cursorY += 25; 
        }
        else if (cmd.equals("date")) { 
            showDate(cursorY); 
            cursorY += 25; 
        }
        else if (cmd.equals("ver")) {
            g.setColor(Color.GREEN);
            printLine("JVMOS Kernel v2.5 (Baremetal Java x86)");
            printLine("Micro-rt Integrado - Slam 2026");
        }
        else if (cmd.equals("clear") || cmd.equals("cls")) { 
            g.clearScreen(); 
            cursorY = 40; 
        }       
        else if (cmd.equals("help")) { 
            SystemShell.printHelp(); 
        }
        else if (cmd.equals("exit")) { 
            shutdown(); 
        }
        else if(cmd.equals("reboot")){
            reboot();
        }
        else if(cmd.equals("tunel")){
            // juego de muestra
            JTunelScope.execute();
        }
        else if (cmd.equals("net")) {
            String netCmd = "";
            String netArg = "";
            int space = arg.indexOf(' ');
            
            if (space == -1) {
                netCmd = arg;
            } else {
                // Separador manual de argumentos usando getBytes y arraycopy
                byte[] argBytes = arg.getBytes();
                byte[] cmdB = new byte[space];
                java.lang.System.arraycopy(argBytes, 0, cmdB, 0, space);
                netCmd = new String(cmdB);
                
                byte[] argB = new byte[argBytes.length - space - 1];
                java.lang.System.arraycopy(argBytes, space + 1, argB, 0, argB.length);
                netArg = new String(argB);
            }
            
            String[] lineas = NetworkShell.execute(netCmd, netArg);
            g.setColor(Color.WHITE);
            for (int i = 0; i < lineas.length; i++) {
                printLine(lineas[i]);
            }
        }
        else if (!cmd.equals("")) {
            g.setColor(Color.RED);
            g.drawString("Comando no reconocido: ", 20, cursorY);
            g.drawString(cmd, 250, cursorY);
            cursorY += 25;
        }
    }

	// ==========================================
    // UTILIDADES Y DIBUJADO
    // ==========================================

    private static void printLine(String text) {
        g.drawString(text, 20, cursorY);
        cursorY += 25;
        if (cursorY > 700) { 
            // Syscall 28: Mover VRAM hacia arriba 25 píxeles (la altura de tus líneas)
            Native.sys(Native.SYS_SCROLL_UP, 25, 0, 0, 0); 
            cursorY -= 25; // El cursor retrocede en Y para quedarse en la misma línea visual
        }
    }
    
    // Imprime el prólogo del shell
    public static void drawPrompt() {
        g.setColor(Color.GREEN); 
        g.drawString("JVMOS [", 20, cursorY);
        g.drawString(currentDirPath, 90, cursorY);
        
        int pathLen = currentDirPath.length();
        int endBracketX = 90 + (pathLen * 10);
        
        g.drawString("]>", endBracketX, cursorY);
        cursorX = endBracketX + 20;
    }

    // Imprime la ayuda del shell
	public static void printHelp() {
        g.setColor(Color.GREEN); printLine("COMANDOS DISPONIBLES:");
        g.setColor(Color.WHITE);
        printLine("  help       : Muestra este menu");
        printLine("  ls / dir   : Lista archivos");
        printLine("  mkdir      : Crea directorio");
        printLine("  cd         : Cambia directorio (soporta ..)");
        printLine("  write      : Crea un archivo con texto (write test.txt hola)");        
        printLine("  cat        : Muestra el contenido de un archivo");
        printLine("  rm         : Elimina archivo");
        printLine("  cp         : Copia archivo a RAM");
        printLine("  mv         : Corta archivo a RAM");
        printLine("  paste      : Pega desde la RAM");
        printLine("  run / java : Ejecuta .class");
        printLine("  format     : Formatea la particion actual");
        printLine("  nc         : Recibir archivos desde la red (similar a netcat)");
        printLine("  net        : Herramientas de red (ej. net ifconfig)");
        printLine("  cls / clear: Limpia pantalla");
        printLine("  date       : Muestra fecha");
        printLine("  time       : Muestra hora");
        printLine("  cube       : Animacion de Cubo en wireframe");
        printLine("  cube2d     : Animacion de Cubo en 3D");
        printLine("  cube3d     : Animacion de Cubo en 3D con Mesh");
        printLine("  demo3d     : Test de motor 3D final");
        printLine("  startx     : Interfaz Grafica (Deshabilitada)");
        printLine("  ver        : Info del sistema");
        printLine("  reboot     : Reiniciar el sistema");
        printLine("  exit       : Apagar equipo");
    }

    // Crea una copia segura de una cadena de texto
    private static String generateSafeCopyName(String original) {
        byte[] dBytes = original.getBytes();
        byte[] nBytes = new byte[15];
        nBytes[0] = 'C'; nBytes[1] = '_';
        int limit = dBytes.length < 13 ? dBytes.length : 13;
        System.arraycopy(dBytes, 0, nBytes, 2, limit);
        return new String(nBytes);
    }

    // Iniciar el modo gráfico del UI
    public static void runStartX() {
        g.setColor(Color.RED); 
        //printLine("El modo Grafico (Startx) esta deshabilitado temporalmente.");   
        UI mui = new UI();    
        mui.runStartX();      
    }

    // Muestra la hora
    public static void showTime(int y) {
        // Formato-> HORA: HH:mm:ss
        int hour = Calendar.get(Calendar.HOUR);
        int min  = Calendar.get(Calendar.MINUTE);
        int sec  = Calendar.get(Calendar.SECOND);
        g.setColor(Color.GREEN);
        g.drawString(" HORA: ", 20, y);
        // hora 01-12
        g.drawChar((char)((hour / 10) + '0'), 90, y); 
        g.drawChar((char)((hour % 10) + '0'), 100, y); 
        g.drawChar(':', 110, y);
        // minutos 00 a 59
        g.drawChar((char)((min / 10) + '0'), 120, y); 
        g.drawChar((char)((min % 10) + '0'), 130, y);  
        g.drawChar(':', 140, y);
        // segundos 00 a 59
        g.drawChar((char)((sec / 10) + '0'), 150, y); 
        g.drawChar((char)((sec % 10) + '0'), 160, y);
    }

    // Muestra la fecha
    public static void showDate(int y) {
        // Formato-> FECHA: dd/mm/aaaa
        int day = Calendar.get(Calendar.DAY);
        int month = Calendar.get(Calendar.MONTH);
        int year = Calendar.get(Calendar.YEAR);
        g.setColor(Color.GREEN); 
        g.drawString("FECHA: ", 20, y);
        // dias 01 a 31
        g.drawChar((char)((day / 10) + '0'), 90, y); 
        g.drawChar((char)((day % 10) + '0'), 100, y); 
        g.drawChar('/', 110, y);
        // mes 01 a 12
        g.drawChar((char)((month / 10) + '0'), 120, y); 
        g.drawChar((char)((month % 10) + '0'), 130, y); 
        g.drawString("/20", 140, y);
        // años 0000 a 9999
        g.drawChar((char)((year / 10) + '0'), 170, y); 
        g.drawChar((char)((year % 10) + '0'), 180, y);
    }

    
    // Imprime una cabecera inicial en el shell
    public static void shellHeader() {
        g.setColor(Color.CYAN);
        g.drawString("JVMOS BAREMETAL TERMINAL", 20, 30);
        g.drawString("Escribe 'help' para ver los comandos.", 20, 50);
        g.drawString("----------------------------------------------------------", 20, 65);
        cursorY = 85;
    }

    // Apaga el sistema
    public static void shutdown() {
        g.clearScreen();
        g.setColor(Color.RED);
        g.drawString("SISTEMA APAGADO. CERRANDO EN 2s...", 380, 360);
        try { 
            Thread.sleep(2000);
        } catch(Exception e){
            // System.err.println(e.getMessage); 
        } 
        java.lang.System.exit(0);
    }

    // Reinicia el sistema
    public static void reboot(){
        java.lang.System.reboot();
    }
	
}
