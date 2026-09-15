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

import java.lang.System;

// Caché IP -> MAC (Memoria aplanada a 1D para máxima compatibilidad JIT)
public class ArpTable {
    private static final int MAX_ENTRIES = 16;
    
    // Almacenamos todo de forma contigua: 16 * 4 = 64 bytes para IPs
    private static byte[] ips = new byte[MAX_ENTRIES * 4];
    // 16 * 6 = 96 bytes para MACs
    private static byte[] macs = new byte[MAX_ENTRIES * 6];
    
    private static int count = 0;

    public static synchronized void put(byte[] ip, byte[] mac) {
        if (ip == null || mac == null) return;

        // Buscar si la IP ya existe en la caché
        for (int i = 0; i < count; i++) {
            int ipOffset = i * 4;
            if (ips[ipOffset] == ip[0] && 
                ips[ipOffset + 1] == ip[1] && 
                ips[ipOffset + 2] == ip[2] && 
                ips[ipOffset + 3] == ip[3]) {
                
                // Si existe, actualizamos su MAC (offset = i * 6)
                System.arraycopy(mac, 0, macs, i * 6, 6);
                return;
            }
        }
        
        // Si no existe y hay espacio, la añadimos al final
        if (count < MAX_ENTRIES) {
            System.arraycopy(ip, 0, ips, count * 4, 4);
            System.arraycopy(mac, 0, macs, count * 6, 6);
            count++;
        }
    }

    public static synchronized byte[] get(byte[] ip) {
        if (ip == null) return null;

        for (int i = 0; i < count; i++) {
            int ipOffset = i * 4;
            
            // Comparamos byte a byte usando el offset
            if (ips[ipOffset] == ip[0] && 
                ips[ipOffset + 1] == ip[1] && 
                ips[ipOffset + 2] == ip[2] && 
                ips[ipOffset + 3] == ip[3]) {
                
                // Si coincide, extraemos la MAC correspondiente
                byte[] foundMac = new byte[6];
                System.arraycopy(macs, i * 6, foundMac, 0, 6);
                return foundMac;
            }
        }
        return null; // Si no existe en la caché
    }
}
