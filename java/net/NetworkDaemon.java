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

// Nueva clase para gestionar el tráfico de red en segundo plano
package java.net;

import java.lang.Thread;
import java.lang.System;
import java.net.RawSocket;
import java.net.DatagramPacket;

public class NetworkDaemon implements Runnable {
    private RawSocket socket;
	private DatagramPacket packet;
    private byte[] localIp;
    private byte[] mac;
	private byte[] buffer;	

    public NetworkDaemon(RawSocket socket, byte[] ip, byte[] mac) {
        this.socket = socket;
        this.localIp = ip;
        this.mac = mac;
		this.buffer = new byte[1536];
		this.packet = new DatagramPacket(this.buffer, 1536);
    }

    public void run() {
        // Scheduler se encarga de todo
        packet.setLength(1536);
        int len = socket.receive(packet);

        if (len >= 42) { 
            // Validar si el paquete es para nuestra MAC o es un Broadcast
            boolean forUs = true;
            boolean isBroadcast = true;
            for (int i = 0; i < 6; i++) {
                if (buffer[i] != mac[i]) forUs = false;
                if (buffer[i] != (byte)0xFF) isBroadcast = false;
            }

            if (forUs || isBroadcast) {
                int etherType = ((buffer[12] & 0xFF) << 8) | (buffer[13] & 0xFF);

                if (etherType == 0x0806) { 
                    handleIncomingArp(buffer, len);
                } else if (etherType == 0x0800) { 
                    int ipHdrLen = (buffer[14] & 0x0F) * 4;
                    int protocol = buffer[23] & 0xFF;

                    if (protocol == 1) { 
                        handleIncomingIcmp(buffer, len, ipHdrLen);
                    }
                }
            }
        }
    }

    private void handleIncomingArp(byte[] frame, int len) {
        // Validar que sea un ARP Request (Opcode = 1)
        if ((frame[20] & 0xFF) == 0x00 && (frame[21] & 0xFF) == 0x01) {
            
            // Verificar si el Target IP (offset 38) es nuestra IP local
            boolean isOurIp = true;
            for (int i = 0; i < 4; i++) {
                if (frame[38 + i] != localIp[i]) {
                    isOurIp = false;
                    break;
                }
            }

            if (isOurIp) {
                // Intercambiar MACs de Ethernet
                for (int i = 0; i < 6; i++) {
                    frame[i] = frame[6 + i];       
                    frame[6 + i] = mac[i];         
                }

                // Cambiar Opcode a ARP Reply (2)
                frame[21] = 0x02;

                // Extraer los datos del remitente original
                byte[] senderMac = new byte[6];
                byte[] senderIp = new byte[4];
                System.arraycopy(frame, 22, senderMac, 0, 6);
                System.arraycopy(frame, 28, senderIp, 0, 4);

                // Llenar los nuevos datos ARP
                System.arraycopy(mac, 0, frame, 22, 6);
                System.arraycopy(localIp, 0, frame, 28, 4);
                System.arraycopy(senderMac, 0, frame, 32, 6);
                System.arraycopy(senderIp, 0, frame, 38, 4);

                // Enviar la respuesta ARP
                DatagramPacket reply = new DatagramPacket(frame, len);
                socket.send(reply);
            }
        }
    }

    private void handleIncomingIcmp(byte[] frame, int len, int ipHdrLen) {
        int icmpOffset = 14 + ipHdrLen;
        
        // Verificar si la IP destino de IPv4 (offset 30) es nuestra IP local
        boolean isOurIp = true;
        for (int i = 0; i < 4; i++) {
            if (frame[30 + i] != localIp[i]) {
                isOurIp = false;
                break;
            }
        }

        if (isOurIp) {
            // Validar que sea un ICMP Echo Request (Tipo 8)
            if ((frame[icmpOffset] & 0xFF) == 8) {
                
                // Intercambiar MACs de Ethernet
                for (int i = 0; i < 6; i++) {
                    frame[i] = frame[6 + i];
                    frame[6 + i] = mac[i];
                }

                // Intercambiar IPs de IPv4
                byte tempIp;
                for (int i = 0; i < 4; i++) {
                    tempIp = frame[26 + i];
                    frame[26 + i] = frame[30 + i]; 
                    frame[30 + i] = tempIp;        
                }

                // Recalcular Checksum IP
                frame[24] = 0; frame[25] = 0; 
                int ipCk = Checksum.calculate(frame, 14, 20);
                frame[24] = (byte)(ipCk >> 8); frame[25] = (byte)ipCk;

                // Cambiar tipo ICMP a Echo Reply (Tipo 0)
                frame[icmpOffset] = 0;

                // Recalcular Checksum ICMP
                int icmpTotalLen = len - icmpOffset;
                frame[icmpOffset + 2] = 0; frame[icmpOffset + 3] = 0; 
                int icmpCk = Checksum.calculate(frame, icmpOffset, icmpTotalLen);
                frame[icmpOffset + 2] = (byte)(icmpCk >> 8); frame[icmpOffset + 3] = (byte)icmpCk;

                // Enviar la respuesta ICMP (Ping Reply)
                DatagramPacket reply = new DatagramPacket(frame, len);
                socket.send(reply);
            }
        }
    }
}
