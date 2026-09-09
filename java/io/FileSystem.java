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

package java.io;

import java.lang.System;
import java.lang.String;
import kernel.Native;

public class FileSystem {	
    public static final int SUPERBLOCK_LBA = 1;
    public static final int ROOT_DIR_LBA = 2;    
	public static final int MAGIC_SIGNATURE = 0x424D4653; // Mi formato "BMFS" en Hexadecimal
	private static final int ROOT_SECTORS = 8;   
    private int totalSectors; // Cache del tamaño del disco	
	private int cantFiles;
	private File[] listOfFiles;
    
    private DiskIO disk;
    private int nextFreeLba; // Nombre unificado

    // Constructores
    public FileSystem(DiskIO diskController) {
        this.disk = diskController;   		
    }   

    // Formatear disco
    public void format(int diskSectors, boolean fullWipe) {
        this.totalSectors = diskSectors;
        this.nextFreeLba = ROOT_DIR_LBA + ROOT_SECTORS;
        byte[] emptySector = new byte[512]; 
		
		// Limpiar memoria con 0
		for(int i=0;i<512;i++){
			emptySector[i] = 0;
		}

        // Limpieza profunda (Zero-fill)
        if (fullWipe) {
            System.out.print("Realizando formateo completo a bajo nivel... ");
            // Desde LBA 1 para no borrar el Bootloader (GRUB) en LBA 0
            for (int i = 1; i < totalSectors; i++) {
                disk.writeSector(i, emptySector);
            }
            System.out.println("Completado.");
        } else {
            // Formateo rápido: Solo limpiar la tabla del Directorio Raíz
            for (int i = 0; i < ROOT_SECTORS; i++) {
                disk.writeSector(ROOT_DIR_LBA + i, emptySector);
            }
        }

        // Serializar el Superbloque
        byte[] superBlock = new byte[512];
        intToBytes(MAGIC_SIGNATURE, superBlock, 0);     // Offset 0: Firma BMFS
        intToBytes(this.nextFreeLba, superBlock, 4);    // Offset 4: Puntero de espacio libre
        intToBytes(this.totalSectors, superBlock, 8);   // Offset 8: Capacidad total

        disk.writeSector(SUPERBLOCK_LBA, superBlock);
    }
    
    // Montar sistema leyendo el Superbloque
    public boolean mount() {
        byte[] superBlock = disk.readSector(SUPERBLOCK_LBA);
        
        int signature = bytesToInt(superBlock, 0);
        if (signature != MAGIC_SIGNATURE) {
			// Disco no tiene formato BMFS o está corrupto
            return false; 
        }
        
        this.nextFreeLba = bytesToInt(superBlock, 4);
        this.totalSectors = bytesToInt(superBlock, 8);
        
        return this.nextFreeLba >= (ROOT_DIR_LBA + ROOT_SECTORS);
    }
    	
    // Listar archivos recibiendo la ruta del directorio actual
    public File[] listFiles(int dirLba, String parentPath) {
		this.cantFiles = 0;
		this.listOfFiles = new File[128];
        for (int s = 0; s < ROOT_SECTORS; s++) {
            byte[] table = disk.readSector(dirLba + s);
            
            for (int i = 0; i < 16; i++) {
                int offset = i * 32;
                if (table[offset + 16] == File.FLAG_EMPTY) continue;

                int nameLen = 0;
                while (nameLen < 16 && table[offset + nameLen] != 0) nameLen++;
                String entryName = new String(table, offset, nameLen);

                byte type = table[offset + 16];
                int startLba = bytesToInt(table, offset + 17);
                int size = bytesToInt(table, offset + 21);
                int parent = bytesToInt(table, offset + 25);

                // Construcción lógica del Path absoluto
                String sep = parentPath.equals("/") ? "" : "/";
                String fullPath = parentPath + sep + entryName;
                
				if(cantFiles == listOfFiles.length){ expandCapacity(cantFiles + 1);}
				listOfFiles[cantFiles++] = new File(fullPath, entryName, type, startLba, size, parent);
            }
        }
        
        return listOfFiles;
    }

    // Buscar un archivo pasando también la ruta padre
    public File lookup(String name, int dirLba, String parentPath) {
        for (int s = 0; s < ROOT_SECTORS; s++) {
            byte[] table = disk.readSector(dirLba + s);
            
            for (int i = 0; i < 16; i++) {
                int offset = i * 32;
                if (table[offset + 16] == File.FLAG_EMPTY) continue;
                
                int nameLen = 0;
                while (nameLen < 16 && table[offset + nameLen] != 0) nameLen++;
                String entryName = new String(table, offset, nameLen);
                
                if (entryName.equals(name)) {
                    byte type = table[offset + 16];
                    int startLba = bytesToInt(table, offset + 17);
                    int size = bytesToInt(table, offset + 21);
                    int parent = bytesToInt(table, offset + 25);
                    
                    String sep = parentPath.equals("/") ? "" : "/";
                    String fullPath = parentPath + sep + entryName;

                    return new File(fullPath, entryName, type, startLba, size, parent);
                }
            }
        }
        return null;
    }
    
    // Escribir archivo en disco
    public boolean writeFile(String name, byte[] data, int parentLba) {
        if (data == null || name.length() > 15) return false;
		
		int sectorsNeeded = (data.length + 511) / 512;
		// Está lleno el disco duro?
		if(this.nextFreeLba + sectorsNeeded > this.totalSectors){
			System.out.println("Error: Disco lleno.");
			return false;
		}
		        
        int startLba = this.nextFreeLba;
        this.nextFreeLba += sectorsNeeded;
        updateSuperblock();

        int offset = 0;
        for (int i = 0; i < sectorsNeeded; i++) {
            byte[] sec = new byte[512];
            int toCopy = (data.length - offset > 512) ? 512 : (data.length - offset);
            System.arraycopy(data, offset, sec, 0, toCopy);
            disk.writeSector(startLba + i, sec);
            offset += toCopy;
        }

        return addDirectoryEntry(parentLba, name, File.FLAG_FILE, startLba, data.length);
    }

	// Crear un directorio nuevo
	public boolean mkdir(String name, int parentLba){
		if(name == null || name.length() > 15) return false;
		// 8 sectores (128 entradas) igual que la raiz
		int sectorsNeeded = ROOT_SECTORS;
		if(this.nextFreeLba + sectorsNeeded > this.totalSectors){
			System.out.println("Error: Disco lleno. No se puede crear el directorio.");
			return false;
		}
		int startLba = this.nextFreeLba;
        this.nextFreeLba += sectorsNeeded;
        updateSuperblock();

        // Limpiar los sectores físicos con ceros (evitar residuos)        
        byte[] emptySector = new byte[512];
        for (int i = 0; i < 512; i++) {
            emptySector[i] = 0;
        }
        
        for (int i = 0; i < sectorsNeeded; i++) {
            disk.writeSector(startLba + i, emptySector);
        }

        // Registrar el nuevo directorio en la tabla de su padre con tamaño 0
        return addDirectoryEntry(parentLba, name, File.FLAG_DIR, startLba, 0);
    }
	
    // Leer los datos físicos pertenecientes al archivo
    public byte[] readFile(File f) {
        if (f == null || f.length() == 0 || f.isDirectory()) return new byte[0];

        byte[] buffer = new byte[f.length()];
        int sectors = (f.length() + 511) / 512;
        int currentLba = f.getStartLBA();
        int offset = 0;

        for (int i = 0; i < sectors; i++) {
            byte[] sec = disk.readSector(currentLba + i);
            int toCopy = (f.length() - offset > 512) ? 512 : (f.length() - offset);
            System.arraycopy(sec, 0, buffer, offset, toCopy);
            offset += toCopy;
        }
        return buffer;
    }
    
    // Eliminar un archivo
    public boolean delete(File f, int dirLba) {
        for (int s = 0; s < ROOT_SECTORS; s++) {
            byte[] table = disk.readSector(dirLba + s);
            for (int i = 0; i < 16; i++) {
                int offset = i * 32;
                if (table[offset + 16] != File.FLAG_EMPTY) {
                    int nameLen = 0;
                    while (nameLen < 16 && table[offset + nameLen] != 0) nameLen++;
                    String entryName = new String(table, offset, nameLen);
                    
                    if (entryName.equals(f.getName())) {
                        table[offset + 16] = File.FLAG_EMPTY; 
                        disk.writeSector(dirLba + s, table);
                        return true;
                    }
                }
            }
        }
        return false;
    }
	
	// Ejecuta un programa almacenado en el sistema de archivos. 
    public boolean execute(String fileName, int dirLba, String parentPath) {
        File file = lookup(fileName, dirLba, parentPath);
        if (file == null || file.isDirectory()) {
            return false;
        }

        // Lee los datos del archivo en el disco
        byte[] programData = readFile(file);
        if (programData == null || programData.length == 0) {
            return false;
        }

        // Verificación básica (ej: el número mágico 0xCAFEBABE)
        if (programData.length >= 4 && 
            (programData[0] & 0xFF) == 0xCA && 
            (programData[1] & 0xFF) == 0xFE && 
            (programData[2] & 0xFF) == 0xBA && 
            (programData[3] & 0xFF) == 0xBE) {           
			// Syscall 30: sys_exec_jit
            Native.sys(30, 0, programData.length, programData, 0);
            return false;
        } else {
            System.out.println("Formato .class inválido!");
            return false;
        }
    }
    
    // ----- METODOS PRIVADOS AUXILIARES -----
	
	public void expandCapacity(int minimumCapacity) {
		int oldCapacity = listOfFiles.length;
		int newCapacity = oldCapacity << 1;

		if (newCapacity < minimumCapacity) {
			newCapacity = minimumCapacity;
		}

		File[] newValue = new File[newCapacity];

		System.arraycopy(listOfFiles,0,newValue,0,cantFiles);

		listOfFiles = newValue;
	}
	
    private void updateSuperblock() {
        byte[] superBlock = new byte[512];
        intToBytes(MAGIC_SIGNATURE, superBlock, 0);
        intToBytes(this.nextFreeLba, superBlock, 4);
        intToBytes(this.totalSectors, superBlock, 8);
        disk.writeSector(SUPERBLOCK_LBA, superBlock);
    }

    private boolean addDirectoryEntry(int dirLba, String name, byte type, int startLba, int size) {
        for (int s = 0; s < ROOT_SECTORS; s++) {
            byte[] table = disk.readSector(dirLba + s);
            for (int i = 0; i < 16; i++) {
                int offset = i * 32;
                if (table[offset + 16] == File.FLAG_EMPTY) {
                    byte[] nameBytes = name.getBytes();
                    System.arraycopy(nameBytes, 0, table, offset, nameBytes.length);
                    table[offset + nameBytes.length] = 0; 

                    table[offset + 16] = type;
                    intToBytes(startLba, table, offset + 17);
                    intToBytes(size, table, offset + 21);
                    intToBytes(dirLba, table, offset + 25);

                    disk.writeSector(dirLba + s, table);
                    return true;
                }
            }
        }
        return false; 
    }

	public int getCantFiles(){ return cantFiles; }
    private int bytesToInt(byte[] b, int offset) {
        return (b[offset] & 0xFF) | ((b[offset+1] & 0xFF) << 8) | ((b[offset+2] & 0xFF) << 16) | ((b[offset+3] & 0xFF) << 24);
    }

    private void intToBytes(int val, byte[] b, int offset) {
        b[offset] = (byte) (val & 0xFF);
        b[offset+1] = (byte) ((val >> 8) & 0xFF);
        b[offset+2] = (byte) ((val >> 16) & 0xFF);
        b[offset+3] = (byte) ((val >> 24) & 0xFF);
    }
}
