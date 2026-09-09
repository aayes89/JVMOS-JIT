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

package java.net;

import java.io.FileSystem;
import java.io.File;

public class NetworkListener {
    private NetworkAdapter adapter;
    private FileSystem fs;
	
	// Ethernet MTU, con algun extra añadido por si acaso
    private static final int BUFFER_SIZE = 1536; 

	// Constructor 
    public NetworkListener(NetworkAdapter adapter, FileSystem fs) {
        this.adapter = adapter;
        this.fs = fs;
    }

    /**
     * Escucha la llegada de datos. Si se reciben datos, extrae la carga útil,
	 * asume que se trata de un archivo y lo guarda en el sistema de archivos. 
	 * @param fileName El nombre con el que se guardará el archivo recibido (p. ej., "Program.class")
	 * @return true si el archivo se recibió y guardó correctamente; false en caso contrario.
     */	
    public boolean listenAndSave(String fileName) {
        if (!adapter.isInitialized()) return false;

        byte[] rawBuffer = new byte[BUFFER_SIZE];
        DatagramPacket packet = new DatagramPacket(rawBuffer, BUFFER_SIZE);

        System.out.print("Esperando datos entrantes...");
        // Bloqueo de la llamada de recepción, esto sería asíncrono o se ejecutaría en un hilo independiente.
        int bytesReceived = adapter.receive(packet); 

        if (bytesReceived > 0) {
            System.out.println("Recivido: " + bytesReceived + " bytes.");
            
            // Extraer los datos (payload). 
			// HAL devuelve tramas Ethernet sin procesar.
			// Se debe omitir las cabeceras Ethernet (14 bytes), IP (20 bytes) y UDP (8 bytes). 
			// Tamaño total de las cabeceras = 42 bytes. 
			// -* Cambiará cuando implemente la pila TCP/IP algún día *-
            int headerOffset = 42; 
            int payloadSize = bytesReceived - headerOffset;

            if (payloadSize <= 0) return false;

            byte[] payload = new byte[payloadSize];
            System.arraycopy(rawBuffer, headerOffset, payload, 0, payloadSize);

            // Guardo el payload en el disco
            boolean success = fs.writeFile(fileName, payload, FileSystem.ROOT_DIR_LBA);
            if (success) {
                System.out.println("Almacenado como: " + fileName);
            } else {
                System.out.println("Error: Fallo al intentar almacenar el archivo.");
            }
            return success;
        }
        return false;
    }
}
