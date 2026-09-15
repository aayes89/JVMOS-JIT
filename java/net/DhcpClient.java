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

import java.lang.Thread;
import java.lang.System;

public class DhcpClient {
    private NetworkAdapter adapter; // Mi clase adaptador de red
    private byte[] mac;             // mi MAC
    private int xid = 0x3903F311;   // ID de Transacción aleatorio/fijo (probare luego con Random a ver que tal va)

    // Constructor
    public DhcpClient(NetworkAdapter adapter, byte[] mac) {
        this.adapter = adapter;
        this.mac = mac;
    }

    // extraido de implementación en C y adaptado a Java. (similar a como está en Linux)
    public boolean discoverAndConfigure() {
        if (!adapter.isInitialized()) return false;

        byte[] frame = new byte[342]; 
        buildDhcpPacket(frame, (byte) 1); 

        DatagramPacket packet = new DatagramPacket(frame, frame.length);
        adapter.send(packet);

        byte[] rx = new byte[1536];
        DatagramPacket rxPacket = new DatagramPacket(rx, 1536);

        for (int i = 0; i < 50; i++) { 
            if(i%20 == 0){
                adapter.send(packet);
            }

            int len = adapter.receive(rxPacket);
            if (len > 240) {
                if ((rx[12] == 0x08 && rx[13] == 0x00) && rx[23] == 17) { 
            
                // Enmascarar con 0xFF para evadir el bug de casteo de bytes negativos del JIT
                for (int j = 42; j < len - 4; j++) {
                    if ((rx[j] & 0xFF) == 0x63 && (rx[j+1] & 0xFF) == 0x82 && (rx[j+2] & 0xFF) == 0x53 && (rx[j+3] & 0xFF) == 0x63) {
                        
                        int dhcpOffset = j - 236; 
                        
                        byte[] offeredIp = new byte[4];
                        System.arraycopy(rx, dhcpOffset + 16, offeredIp, 0, 4);

                        byte[] dnsTmp = new byte[4]; 
                        byte[] gwTmp = new byte[4];
                        byte[] maskTmp = new byte[4];

                        int k = j + 4; 
                        while (k < len) {
                            byte code = rx[k];
                            if (code == (byte)255) break; 
                            if (code == 0) { k++; continue; } 
                            
                            if (k + 1 >= len) break; 
                            int optLen = rx[k + 1] & 0xFF;
                            if (k + 2 + optLen > len) break; 

                            if (code == 1 && optLen == 4) System.arraycopy(rx, k + 2, maskTmp, 0, 4);
                            else if (code == 3 && optLen >= 4) System.arraycopy(rx, k + 2, gwTmp, 0, 4);
                            else if (code == 6 && optLen >= 4) System.arraycopy(rx, k + 2, dnsTmp, 0, 4);
                            
                            k += 2 + optLen;
                        }

                        NetworkShell.setLocalIP(offeredIp);
                        if (maskTmp[0] != 0) NetworkShell.setMask(maskTmp);
                        if (gwTmp[0] != 0) NetworkShell.setGW(gwTmp);
                        if (dnsTmp[0] != 0) NetworkShell.setDNS1(dnsTmp);

                        return true;
                    }
                }
                }
            }
            rxPacket.setLength(1536);
            try { Thread.sleep(50); } catch (Exception e) {}
        }
        return false;
    }

    // Construir paquete DHCP (extraido y adaptado de implementación oficial)
    private void buildDhcpPacket(byte[] f, byte msgType) {
        // Cabecera Ethernet  
        // MAC destino (Broadcast - 255.255.255.255)
        for (int i = 0; i < 6; i++) f[i] = (byte) 0xFF;

        // MAC origen
        System.arraycopy(mac, 0, f, 6, 6);

        // IPv4
        f[12] = 0x08; 
        f[13] = 0x00; 

        // Cabecera IP
        f[14] = 0x45; f[15] = 0x00; // Version 4, IHL 5
        f[16] = 0x01; f[17] = 0x48; // Total (328 bytes)
        f[22] = 64;   f[23] = 17;   // TTL 64, Protocolo UDP
        for (int i = 26; i < 30; i++) f[i] = 0; // Src 0.0.0.0
        for (int i = 30; i < 34; i++) f[i] = (byte) 0xFF; // Dst 255.255.255.255
        
        int ipCk = Checksum.calculate(f, 14, 20);
        f[24] = (byte)(ipCk >> 8); f[25] = (byte)ipCk;

        // Cabecera UDP (68 -> 67)
        f[34] = 0x00; f[35] = 68; // Src Puerto 68
        f[36] = 0x00; f[37] = 67; // Dst Puerto 67
        f[38] = 0x01; f[39] = 0x34; // Longitud 308

        // Carga DHCP (Offset 42)
        int d = 42;
        f[d] = 1; f[d+1] = 1; f[d+2] = 6; // BOOTREQUEST, Ethernet, HW Len 6
        f[d+4] = (byte)(xid >> 24); f[d+5] = (byte)(xid >> 16);
        f[d+6] = (byte)(xid >> 8);  f[d+7] = (byte)xid;
        
        System.arraycopy(mac, 0, f, d + 28, 6); // MAC del cliente

        // Magic Cookie (0x63825363)
        int opt = d + 236;
        f[opt++] = 0x63; f[opt++] = (byte)0x82; f[opt++] = 0x53; f[opt++] = 0x63;

        // Opcion 53: Tipo de mensaje DHCP 
        f[opt++] = 53; f[opt++] = 1; f[opt++] = msgType;
        // Opcion 55: Lista de parámetros de petición (Subnet Mask, Router, DNS)
        f[opt++] = 55; f[opt++] = 3; f[opt++] = 1; f[opt++] = 3; f[opt++] = 6;
        // Opcion 255: End
        f[opt] = (byte) 255;
    }

    // Parser básico para obtener Máscara de red, Gateway, DNS y DHCP (TLV)
    private void parseOptions(byte[] buf, int offset, int maxLen, byte[] mask, byte[] gw, byte[] dns) {
        int i = offset;
        while (i < offset + maxLen) {
            byte code = buf[i];
            
            // Opciones de 1 solo byte (sin longitud)
            if (code == (byte)255) { // 255 = End
                break; 
            }
            if (code == 0) {         // 0 = Padding
                i++; 
                continue; 
            }
            
            // Opciones TLV (Tipo, Longitud, Valor)
            if (i + 1 >= offset + maxLen) break; // Protección contra desbordamiento
            int len = buf[i + 1] & 0xFF;
            
            if (i + 2 + len > offset + maxLen) break; // Protección contra datos corruptos

            if (code == 1 && len == 4) {
                System.arraycopy(buf, i + 2, mask, 0, 4);      // Subnet Mask
            } else if (code == 3 && len >= 4) {
                System.arraycopy(buf, i + 2, gw, 0, 4);        // Router/GW (Solo tomamos el primero)
            } else if (code == 6 && len >= 4) {
                System.arraycopy(buf, i + 2, dns, 0, 4);       // DNS Server (Solo tomamos el primero)
            }
            
            i += 2 + len;
        }
    }
}
