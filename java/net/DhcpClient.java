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
    private NetworkAdapter adapter;
    private byte[] mac;             
    private int xid = 0x3903F311;   

    public DhcpClient(NetworkAdapter adapter, byte[] mac) {
        this.adapter = adapter;
        this.mac = mac;
    }

	public boolean discoverAndConfigure() {
        if (!adapter.isInitialized()) return false;

        byte[] txFrame = new byte[350]; 
        int txLen = buildDhcpPacket(txFrame, (byte) 1, null, null); 
        DatagramPacket txPacket = new DatagramPacket(txFrame, txLen);
        adapter.send(txPacket);

        byte[] rx = new byte[1536];
        DatagramPacket rxPacket = new DatagramPacket(rx, 1536);

        int state = 0; // 0 = DISCOVER sent, waiting OFFER; 1 = REQUEST sent, waiting ACK
        byte[] offeredIp = new byte[4];
        byte[] serverId = new byte[4];

        for (int i = 0; i < 100; i++) { // Timeout ampliado para dar tiempo al flujo completo
            if (i > 0 && i % 20 == 0) {
                adapter.send(txPacket); // Retransmitir el estado actual
            }

            rxPacket.setLength(1536);
            int len = adapter.receive(rxPacket);
            
            if (len > 240) {
                // Validar Ethernet (0x0800)
                if ((rx[12] & 0xFF) == 0x08 && (rx[13] & 0xFF) == 0x00) { 
                    
                    // Validar IPv4 (Versión 4) y UDP (Protocolo 17)
                    if ((rx[14] & 0xF0) == 0x40 && (rx[23] & 0xFF) == 17) { 
                        
                        // Validar IHL (Internet Header Length) dinámico
                        int ipHdrLen = (rx[14] & 0x0F) * 4;
                        int udpOffset = 14 + ipHdrLen;
                        
                        // Validar Puerto Origen (67) y Puerto Destino (68)
                        if ((rx[udpOffset] & 0xFF) == 0x00 && (rx[udpOffset + 1] & 0xFF) == 67 &&
                            (rx[udpOffset + 2] & 0xFF) == 0x00 && (rx[udpOffset + 3] & 0xFF) == 68) {
                            
                            int dhcpOffset = udpOffset + 8;
                            
                            // Validar DHCP op = 2 (Bootreply)
                            if ((rx[dhcpOffset] & 0xFF) == 2) {
                                
                                // Validar XID byte por byte para evadir el bug de OR (|) del JIT
                                if (rx[dhcpOffset + 4] == (byte)(this.xid >> 24) &&
                                    rx[dhcpOffset + 5] == (byte)(this.xid >> 16) &&
                                    rx[dhcpOffset + 6] == (byte)(this.xid >> 8) &&
                                    rx[dhcpOffset + 7] == (byte)this.xid) {

                                    // Validar chaddr (nuestra MAC)
                                    boolean macMatch = true;
                                    for(int m = 0; m < 6; m++) {
                                        if (rx[dhcpOffset + 28 + m] != this.mac[m]) {
                                            macMatch = false; break;
                                        }
                                    }

                                    if (macMatch) {
                                        // Validar Magic Cookie exacta (sin escaneos dinámicos)
                                        if ((rx[dhcpOffset + 236] & 0xFF) == 0x63 && 
                                            (rx[dhcpOffset + 237] & 0xFF) == 0x82 && 
                                            (rx[dhcpOffset + 238] & 0xFF) == 0x53 && 
                                            (rx[dhcpOffset + 239] & 0xFF) == 0x63) {
                                            
                                            // Extraer YIADDR (IP Ofertada)
                                            byte[] tempIp = new byte[4];
                                            System.arraycopy(rx, dhcpOffset + 16, tempIp, 0, 4);

                                            byte msgType = 0;
                                            byte[] dnsTmp = new byte[4]; 
                                            byte[] gwTmp = new byte[4];
                                            byte[] maskTmp = new byte[4];
                                            byte[] srvTmp = new byte[4];

                                            // Extraer opciones TLV
                                            int k = dhcpOffset + 240; 
                                            while (k < len) {
                                                int code = rx[k] & 0xFF; 
                                                if (code == 255) break; 
                                                if (code == 0) { k++; continue; } 
                                                
                                                if (k + 1 >= len) break; 
                                                int optLen = rx[k + 1] & 0xFF; 
                                                if (k + 2 + optLen > len) break; 

                                                if (code == 53 && optLen == 1) msgType = rx[k + 2];
                                                else if (code == 54 && optLen == 4) System.arraycopy(rx, k + 2, srvTmp, 0, 4);
                                                else if (code == 1 && optLen == 4) System.arraycopy(rx, k + 2, maskTmp, 0, 4);
                                                else if (code == 3 && optLen >= 4) System.arraycopy(rx, k + 2, gwTmp, 0, 4);
                                                else if (code == 6 && optLen >= 4) System.arraycopy(rx, k + 2, dnsTmp, 0, 4);
                                                k += 2 + optLen;
                                            }

                                            // MÁQUINA DE ESTADOS DHCP
                                            if (state == 0 && msgType == 2) { // Recibido OFFER
                                                System.arraycopy(tempIp, 0, offeredIp, 0, 4);
                                                System.arraycopy(srvTmp, 0, serverId, 0, 4);
                                                
                                                // Transicionar a REQUEST
                                                txLen = buildDhcpPacket(txFrame, (byte) 3, offeredIp, serverId);
                                                txPacket.setLength(txLen);
                                                adapter.send(txPacket);
                                                
                                                state = 1;
                                                i = 1; // Reiniciar contador para esperar el ACK
                                                
                                            } else if (state == 1 && msgType == 5) { // Recibido ACK
                                                // Asignar al sistema EXCLUSIVAMENTE tras el ACK
                                                NetworkShell.setLocalIP(offeredIp);
                                                if (maskTmp[0] != 0) NetworkShell.setMask(maskTmp);
                                                if (gwTmp[0] != 0) NetworkShell.setGW(gwTmp);
                                                if (dnsTmp[0] != 0) NetworkShell.setDNS1(dnsTmp);
                                                return true;
                                                
                                            } else if (state == 1 && msgType == 6) { // Recibido NAK
                                                // Revertir a DISCOVER
                                                state = 0;
                                                txLen = buildDhcpPacket(txFrame, (byte) 1, null, null);
                                                txPacket.setLength(txLen);
                                                adapter.send(txPacket);
                                                i = 1;
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            try { Thread.sleep(50); } catch (Exception e) {}
        }
        return false;
    }

    private int buildDhcpPacket(byte[] f, byte msgType, byte[] reqIp, byte[] srvId) {
        // Limpiar la memoria residual
        for (int i = 0; i < f.length; i++) f[i] = 0;

        for (int i = 0; i < 6; i++) f[i] = (byte) 0xFF;
        System.arraycopy(mac, 0, f, 6, 6);
        f[12] = 0x08; f[13] = 0x00; 
        
        f[14] = 0x45; f[15] = 0x00; 
        f[18] = 0x55; f[19] = 0x66; // ID no nulo para evitar filtros SLIRP
        f[20] = 0x00; f[21] = 0x00; // Flags limpios
        f[22] = 64;   f[23] = 17;   
        
        for (int i = 26; i < 30; i++) f[i] = 0; 
        for (int i = 30; i < 34; i++) f[i] = (byte) 0xFF; 

        f[34] = 0x00; f[35] = 68; 
        f[36] = 0x00; f[37] = 67; 

        int d = 42;
        f[d] = 1; f[d+1] = 1; f[d+2] = 6; 
        f[d+4] = (byte)(xid >> 24); f[d+5] = (byte)(xid >> 16);
        f[d+6] = (byte)(xid >> 8);  f[d+7] = (byte)xid;
        
        f[d+10] = (byte) 0x80; f[d+11] = 0x00; // Flag Broadcast
        
        System.arraycopy(mac, 0, f, d + 28, 6); 

        int opt = d + 236;
        f[opt++] = 0x63; f[opt++] = (byte)0x82; f[opt++] = 0x53; f[opt++] = 0x63;
        
        f[opt++] = 53; f[opt++] = 1; f[opt++] = msgType;
        
        if (msgType == 3 && reqIp != null && srvId != null) {
            f[opt++] = 50; f[opt++] = 4; System.arraycopy(reqIp, 0, f, opt, 4); opt += 4;
            f[opt++] = 54; f[opt++] = 4; System.arraycopy(srvId, 0, f, opt, 4); opt += 4;
        }
        
        f[opt++] = 55; f[opt++] = 3; f[opt++] = 1; f[opt++] = 3; f[opt++] = 6;
        f[opt++] = (byte) 255;
        
        while (opt < 342) f[opt++] = 0;

        int totalLen = opt;

        int ipLen = totalLen - 14;
        f[16] = (byte)(ipLen >> 8); f[17] = (byte)ipLen;
        
        int udpLen = totalLen - 34;
        f[38] = (byte)(udpLen >> 8); f[39] = (byte)udpLen;

        int ipCk = Checksum.calculate(f, 14, 20);
        f[24] = (byte)(ipCk >> 8); f[25] = (byte)ipCk;

        return totalLen;
    }
}
