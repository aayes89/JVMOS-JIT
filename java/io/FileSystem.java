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

public class FileSystem {
    private static final int DIR_TABLE_LBA = 2; // índice
    private static final int DATA_START_LBA = 11;
    
    private DiskIO disk;
    private int nextFreeLBA; // rastrear espacio libre

	// Constructores
    public FileSystem(DiskIO diskController) {
        this.disk = diskController;
        this.nextFreeLBA = DATA_START_LBA;
		// falta implementar tabla de archivos aquí para buscar el próximo LBA libre
    }
	public FileSystem(DiskIO diskController, int startLBA) {
        this.disk = diskController;
        this.nextFreeLBA = startLBA;
		// falta implementar tabla de archivos aquí para buscar el próximo LBA libre
    }

    // equivalente a dar formato al disco
    public void format() {
        byte[] emptySector = new byte[512];
        for (int i = 0; i < 9; i++) {
            disk.writeSector(DIR_TABLE_LBA + i, emptySector);
        }
        this.nextFreeLBA = DATA_START_LBA;
    }

    // Buscar un archivo en un directorio
    public File lookup(String name) {
        // Leer el primer sector de la tabla de directorios
        byte[] table = disk.readSector(DIR_TABLE_LBA);
        
        // Escanear las 16 entradas en el sector (32 bytes cada una)
        for (int i = 0; i < 16; i++) {
            int offset = i * 32;
            if (table[offset] == 0) continue; // vacio
            
            // Reconstruir el nombre
            int nameLen = 0;
            while (nameLen < 16 && table[offset + nameLen] != 0) nameLen++;
            String entryName = new String(table, offset, nameLen);
            
            if (entryName.equals(name)) {
                boolean isDir = (table[offset + 16] == File.FLAG_DIR);
                // Reconstruir el entero de 32-bit para LBA (LE)
                int lba = (table[offset + 17] & 0xFF) | 
                         ((table[offset + 18] & 0xFF) << 8) | 
                         ((table[offset + 19] & 0xFF) << 16) | 
                         ((table[offset + 20] & 0xFF) << 24);
                         
                // Reconstruir el entero de 32-bit para Size
                int size = (table[offset + 21] & 0xFF) | 
                          ((table[offset + 22] & 0xFF) << 8) | 
                          ((table[offset + 23] & 0xFF) << 16) | 
                          ((table[offset + 24] & 0xFF) << 24);
                          
                return new File(entryName, isDir, lba, size);
            }
        }
        return null; // archivo no encontrado
    }

    // Crear un archivo nuevo y lo escribe en el sector físico
    public boolean createFile(String name, byte[] data) {
		// Nombre muy largo para FAT (no más de 16 caracteres)
        if (name.length() > 15) return false; 
        
        byte[] table = disk.readSector(DIR_TABLE_LBA);
        
        // Encontrar una casilla libre en la tabla de directorios
        int freeSlot = -1;
        for (int i = 0; i < 16; i++) {
            if (table[i * 32] == 0) {
                freeSlot = i * 32;
                break;
            }
        }
        if (freeSlot == -1) return false; // Directorio lleno

        // Escribir la información en el sector
        disk.writeSector(this.nextFreeLBA, data); // Asumiendo que los datos <= 512 bytes
        
        // Serializar los metadatos del archivo en la casilla libre
        byte[] nameBytes = name.getBytes();
        System.arraycopy(nameBytes, 0, table, freeSlot, nameBytes.length);
        table[freeSlot + nameBytes.length] = 0; // añadido terminación Null
        
        table[freeSlot + 16] = File.FLAG_FILE;
        
        // Serializar LBA a bytes
        table[freeSlot + 17] = (byte) (this.nextFreeLBA & 0xFF);
        table[freeSlot + 18] = (byte) ((this.nextFreeLBA >> 8) & 0xFF);
        table[freeSlot + 19] = (byte) ((this.nextFreeLBA >> 16) & 0xFF);
        table[freeSlot + 20] = (byte) ((this.nextFreeLBA >> 24) & 0xFF);
        
        // Serializar Size a bytes
        table[freeSlot + 21] = (byte) (data.length & 0xFF);
        table[freeSlot + 22] = (byte) ((data.length >> 8) & 0xFF);
        table[freeSlot + 23] = (byte) ((data.length >> 16) & 0xFF);
        table[freeSlot + 24] = (byte) ((data.length >> 24) & 0xFF);

        // Guardar la tabla de directorios actualizada al disco
        disk.writeSector(DIR_TABLE_LBA, table);
        
        this.nextFreeLBA++; // Avanzar al siguiente puntero de espacio libre
        return true;
    }

    // Leer los datos físicos pertenecientes al archivo
    public byte[] readFile(File file) {
        if (file == null || file.getStartLBA() == -1) return new byte[0];
        
        // Por ahora, asumo la lectura a un sólo sector
        // Será necesario leer múltiples sectores basado en file.length()
        byte[] rawSector = disk.readSector(file.getStartLBA());
        
        // Recorto el sector de 512 bytes al tamaño real del archivo.
        byte[] actualData = new byte[file.length()];
        System.arraycopy(rawSector, 0, actualData, 0, file.length());
        
        return actualData;
    }
}
