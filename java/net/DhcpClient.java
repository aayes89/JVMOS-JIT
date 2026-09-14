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

        // Armar DHCP Discover
        byte[] frame = new byte[342]; // Frame completo Ethernet + IP + UDP + DHCP Payload
        buildDhcpPacket(frame, (byte) 1); // Option 53: 1 = Discover

        DatagramPacket packet = new DatagramPacket(frame, frame.length);
        adapter.send(packet);

        // 2. Esperar respuesta DHCP Offer / Ack
        byte[] rx = new byte[1536];
        DatagramPacket rxPacket = new DatagramPacket(rx, 1536);

        for (int i = 0; i < 50; i++) { // Timeout de ~2.5 seg
            int len = adapter.receive(rxPacket);
            if (len > 240) {
                // Verificar si es respuesta UDP DHCP (puerto origen 67, destino 68)
                if ((rx[12] == 0x08 && rx[13] == 0x00) && rx[23] == 17) { // IPv4 + UDP
                    int udpOffset = 14 + ((rx[14] & 0x0F) * 4);
                    int dhcpOffset = udpOffset + 8;

                    // Magic Cookie DHCP (0x63, 0x82, 0x53, 0x63)
                    if (rx[dhcpOffset + 236] == (byte)0x63 && rx[dhcpOffset + 237] == (byte)0x82) {
                        
                        // Extraer YIADDR (Mi IP) en offset 16 dentro de la cabecera DHCP
                        byte[] offeredIp = new byte[4];
                        System.arraycopy(rx, dhcpOffset + 16, offeredIp, 0, 4);

                        // Parsear Opciones DHCP
                        byte[] mask = new byte[4];
                        byte[] gw = new byte[4];
                        byte[] dns1 = new byte[4];

                        parseOptions(rx, dhcpOffset + 240, len - (dhcpOffset + 240), mask, gw, dns1);

                        // Aplicar la configuración obtenida al Shell
                        NetworkShell.setLocalIP(offeredIp);
                        if (mask[0] != 0) NetworkShell.setMask(mask);
                        if (gw[0] != 0) NetworkShell.setGW(gw);
                        if (dns1[0] != 0) NetworkShell.setDNS1(dns1);

                        return true;
                    }
                }
            }
            try { Thread.sleep(50); } catch (Exception e) {}
        }
        return false;
    }

    // Construir paquete DHCP (extraido y adaptado de implementación oficial)
    private void buildDhcpPacket(byte[] f, byte msgType) {
        // Ethernet Header (Broadcast)
        for (int i = 0; i < 6; i++) f[i] = (byte) 0xFF;
        System.arraycopy(mac, 0, f, 6, 6);
        f[12] = 0x08; f[13] = 0x00; // IPv4

        // IP Header
        f[14] = 0x45; f[15] = 0x00; // Version 4, IHL 5
        f[16] = 0x01; f[17] = 0x48; // Total Length (328 bytes)
        f[22] = 64;   f[23] = 17;   // TTL 64, Protocol UDP
        for (int i = 26; i < 30; i++) f[i] = 0; // Src 0.0.0.0
        for (int i = 30; i < 34; i++) f[i] = (byte) 0xFF; // Dst 255.255.255.255
        
        int ipCk = Checksum.calculate(f, 14, 20);
        f[24] = (byte)(ipCk >> 8); f[25] = (byte)ipCk;

        // UDP Header (68 -> 67)
        f[34] = 0x00; f[35] = 68; // Src Port 68
        f[36] = 0x00; f[37] = 67; // Dst Port 67
        f[38] = 0x01; f[39] = 0x34; // Length 308

        // DHCP Payload (Offset 42)
        int d = 42;
        f[d] = 1; f[d+1] = 1; f[d+2] = 6; // BOOTREQUEST, Ethernet, HW Len 6
        f[d+4] = (byte)(xid >> 24); f[d+5] = (byte)(xid >> 16);
        f[d+6] = (byte)(xid >> 8);  f[d+7] = (byte)xid;
        
        System.arraycopy(mac, 0, f, d + 28, 6); // Client MAC Address

        // Magic Cookie (0x63825363)
        int opt = d + 236;
        f[opt++] = 0x63; f[opt++] = (byte)0x82; f[opt++] = 0x53; f[opt++] = 0x63;

        // Option 53: DHCP Message Type
        f[opt++] = 53; f[opt++] = 1; f[opt++] = msgType;
        // Option 55: Parameter Request List (Subnet Mask, Router, DNS)
        f[opt++] = 55; f[opt++] = 3; f[opt++] = 1; f[opt++] = 3; f[opt++] = 6;
        // Option 255: End
        f[opt] = (byte) 255;
    }

    // Parser básico para obtener Máscara de red, Gateway y DNS
    private void parseOptions(byte[] buf, int offset, int maxLen, byte[] mask, byte[] gw, byte[] dns) {
        int i = offset;
        while (i < offset + maxLen && buf[i] != (byte)255) {
            byte code = buf[i];
            if (code == 0) { i++; continue; }
            int len = buf[i + 1] & 0xFF;
            if (code == 1) System.arraycopy(buf, i + 2, mask, 0, 4);      // Subnet Mask
            else if (code == 3) System.arraycopy(buf, i + 2, gw, 0, 4);   // Router/GW
            else if (code == 6) System.arraycopy(buf, i + 2, dns, 0, 4);  // DNS Server
            i += 2 + len;
        }
    }
}
