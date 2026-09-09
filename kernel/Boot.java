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
import java.io.File;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Toolkit;
import java.util.Calendar;
import java.io.PrintStream;
import java.lang.Thread;

public class Boot {
     
    // ==========================================
    // SISTEMA DE ARCHIVOS Y NAVEGACIÓN EN ÁRBOL
    // ==========================================
    private static DiskIO disk;
    private static FileSystem fs;
    
    // Pila de navegación para soportar 'cd ..' en múltiples niveles
    private static int[] lbaStack = new int[32];
    private static int lbaDepth = 0;
    
    private static int currentDirLba;
    private static int parentDirLba;
    private static String currentDirPath;
    
    // ==========================================
    // PORTAPAPELES
    // ==========================================
    private static Portapapeles portapapeles;

    private static Graphics2D g;  
    private static int cursorX = 85;
    private static int cursorY = 80;

    public static void main(String[] args) {
        java.lang.System.out = new PrintStream();
        System.out.println("[Boot] Inicializando subsistemas Micro-RT de JVMOS-JIT...");
        
        g = new Graphics2D();
        portapapeles = new Portapapeles();
        
        initKeyboard();
        initFS();        
        
        System.out.println("[GRAPHICS] Inicializando subsistema grafico...");
        System.out.println("[HARDWARE] Inicializando controladores I/O...");
        System.out.println("[JVMOS-JIT] Iniciando entorno interactivo...");
        
        dramaticBIOS();
        clearScreen();
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
						clearScreen(); 
						cursorY = 40; 
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
				}catch(Exception e){
					// No capturar nada aquí
				}
        }
    }
    
    public static void initFS() {
        disk = new DiskIO();
        fs = new FileSystem(disk);
        if (!fs.mount()) {
            // Formateo Rápido (false) evita congelar el Kernel 20 minutos por IO-blocking
            fs.format(40960, false); 
        }
        currentDirLba = FileSystem.ROOT_DIR_LBA;
        parentDirLba = FileSystem.ROOT_DIR_LBA;
        lbaStack[0] = currentDirLba;
        lbaDepth = 0;
        currentDirPath = "/";
    }

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
                        if (cursorY > 700) { clearScreen(); cursorY = 40; }
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
        else if (cmd.equals("cube")) { 
			runCube3D(); 
			clearScreen(); 
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
			clearScreen(); 
			cursorY = 40; 
		}
        else if (cmd.equals("help")) { 
			printHelp(); 
		}
        else if (cmd.equals("exit")) { 
			shutdown(); 
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
            clearScreen();
            cursorY = 40;
        }
    }
    
    private static void drawPrompt() {
        g.setColor(Color.GREEN); 
        g.drawString("JVMOS [", 20, cursorY);
        g.drawString(currentDirPath, 90, cursorY);
        
        int pathLen = currentDirPath.length();
        int endBracketX = 90 + (pathLen * 10);
        
        g.drawString("]>", endBracketX, cursorY);
        cursorX = endBracketX + 20;
    }

    public static void printHelp() {
        g.setColor(Color.GREEN); printLine("COMANDOS DISPONIBLES:");
        g.setColor(Color.WHITE);
        printLine("  help       : Muestra este menu");
        printLine("  ls / dir   : Lista archivos");
        printLine("  mkdir      : Crea directorio");
        printLine("  cd         : Cambia directorio (soporta ..)");
        printLine("  rm         : Elimina archivo");
        printLine("  cp         : Copia archivo a RAM");
        printLine("  mv         : Corta archivo a RAM");
        printLine("  paste      : Pega desde la RAM");
        printLine("  run / java : Ejecuta .class");
        printLine("  format     : Formatea la particion actual");
        printLine("  cls / clear: Limpia pantalla");
        printLine("  date       : Muestra fecha");
        printLine("  time       : Muestra hora");
        printLine("  cube       : Animacion 3D");
        printLine("  startx     : Interfaz Grafica (Deshabilitada)");
        printLine("  ver        : Info del sistema");
        printLine("  exit       : Apagar equipo");
    }
    
    private static String generateSafeCopyName(String original) {
        byte[] dBytes = original.getBytes();
        byte[] nBytes = new byte[15];
        nBytes[0] = 'C'; nBytes[1] = '_';
        int limit = dBytes.length < 13 ? dBytes.length : 13;
        java.lang.System.arraycopy(dBytes, 0, nBytes, 2, limit);
        return new String(nBytes);
    }
    
    // =========================================================================
    // MOTOR 3D BAREMETAL (Cubo Giratorio)
    // =========================================================================
    public static void runCube3D() {
        clearScreen();
        g.setColor(Color.CYAN);
        g.drawString("Baremetal 3D Engine (JIT Integer Math) - Presiona ESC para salir", 20, 20);

        int[] cubeX = {-50, 50, 50, -50, -50, 50, 50, -50};
        int[] cubeY = {-50, -50, 50, 50, -50, -50, 50, 50};
        int[] cubeZ = {-50, -50, -50, -50, 50, 50, 50, 50};
        int[] edges = { 0,1, 1,2, 2,3, 3,0, 4,5, 5,6, 6,7, 7,4, 0,4, 1,5, 2,6, 3,7 };
        int[] projX = new int[8], projY = new int[8], oldProjX = new int[8], oldProjY = new int[8];
        int angleX = 0, angleY = 0, angleZ = 0;

        while (true) {
            g.setColor(Color.BLACK);
            for (int i = 0; i < 12; i++) {
                int p1 = edges[i * 2], p2 = edges[i * 2 + 1];
                if (oldProjX[p1] != 0) g.drawLine(oldProjX[p1], oldProjY[p1], oldProjX[p2], oldProjY[p2]);
            }

            int sinX = Math.sin(angleX), cosX = Math.cos(angleX);
            int sinY = Math.sin(angleY), cosY = Math.cos(angleY);
            int sinZ = Math.sin(angleZ), cosZ = Math.cos(angleZ);

            for (int i = 0; i < 8; i++) {
                int x = cubeX[i], y = cubeY[i], z = cubeZ[i];
                int xy = (y * cosX - z * sinX) / 256, xz = (y * sinX + z * cosX) / 256; y = xy; z = xz;
                int yx = (x * cosY + z * sinY) / 256, yz = (-x * sinY + z * cosY) / 256; x = yx; z = yz;
                int zx = (x * cosZ - y * sinZ) / 256, zy = (x * sinZ + y * cosZ) / 256; x = zx; y = zy;

                int z_shifted = z + 150;
                projX[i] = (x * 400) / z_shifted + 512; projY[i] = (y * 400) / z_shifted + 384;
            }

            g.setColor(Color.GREEN);
            for (int i = 0; i < 12; i++) {
                int p1 = edges[i * 2], p2 = edges[i * 2 + 1];
                g.drawLine(projX[p1], projY[p1], projX[p2], projY[p2]);
            }

            for (int i = 0; i < 8; i++) { oldProjX[i] = projX[i]; oldProjY[i] = projY[i]; }

            angleX = (angleX + 2) % 360; angleY = (angleY + 3) % 360; angleZ = (angleZ + 1) % 360;
            try{Thread.sleep(16);}catch(Exception e){}
            if (Native.sys(Native.SYS_READ_KEYBOARD, 0, 0, 0, 0) == 27) break;
        }
    }

    public static void runStartX() {
        g.setColor(Color.RED); printLine("El modo Grafico (Startx) esta deshabilitado temporalmente.");
    }

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

    public static void clearScreen() {
		g.setColor(Color.BLACK); 
		g.fillRect(0, 0, 1024, 768);
	}
    public static void initKeyboard() {
		Native.sys(Native.SYS_SET_KBD_LAYOUT, 1, 0, 0, 0);
	}

    public static void dramaticBIOS() {
        clearScreen(); 
		try { 
			Thread.sleep(250);
		} catch(Exception e) {
			// System.err.println(e.getMessage());			
		}
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
			Thread.sleep(1000);
		} catch(Exception e) {
			// System.err.println(e.getMessage());			
		} 
		clearScreen();
    }

    public static void shellHeader() {
        g.setColor(Color.CYAN);
        g.drawString("JVMOS BAREMETAL TERMINAL", 20, 30);
        g.drawString("Escribe 'help' para ver los comandos.", 20, 50);
        g.drawString("----------------------------------------------------------", 20, 65);
        cursorY = 85;
    }

    public static void shutdown() {
        g.setColor(Color.RED);
		g.drawString("SISTEMA APAGADO. CERRANDO EN 2s...", 380, 360);
        try { 
			Thread.sleep(1500);
		} catch(Exception e){
			// System.err.println(e.getMessage); 
		} 
		java.lang.System.exit(0);
    }
}
