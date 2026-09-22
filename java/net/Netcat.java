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
import java.lang.System;
import java.lang.Thread;
import kernel.Native;

public class Netcat {

    public static String[] execute(String args, FileSystem fs, int currentDirLba) {
        boolean listen = false;
        boolean verbose = false;
        int port = -1;
        int timeout = 3000; 
        String targetIpStr = "";
        String filename = "";

        // Parseador manual de argumentos
        String[] tokens = new String[10];
        int tokenCount = 0, start = 0;
        for (int i = 0; i <= args.length(); i++) {
            if (i == args.length() || args.charAt(i) == ' ') {
                if (i > start) {
					tokens[tokenCount++] = args.substring(start, i);
				}
                start = i + 1;
            }
        }

        int i = 0;
        while (i < tokenCount) {
            String t = tokens[i];
            if (t.equals("-l")) {
				listen = true; i++; 
			}
            else if (t.equals("-v")) {
				verbose = true; i++; 
			}
            else if (t.equals("-p") && i + 1 < tokenCount) {
				port = parseNum(tokens[i+1]); i += 2; 
			}
            else if (t.equals("-q") && i + 1 < tokenCount) {
				timeout = parseNum(tokens[i+1]) * 100; i += 2; 
			} 
            else break; 
        }

        if (listen) {
            if (port == -1) {
				return new String[]{"[!] Error: Puerto requerido (-p)."};
			}
            if (i < tokenCount) {
				filename = tokens[i];
			}
            else { 
				return new String[]{"[!] Error: Archivo destino requerido."};
			}
            return doListen(port, timeout, verbose, filename, fs, currentDirLba);
        } else {
            if (tokenCount - i < 3) {
				return new String[]{"[!] Uso: nc [-v] [-q #] <ip> <port> <archivo>"};
			}
            targetIpStr = tokens[i];
            port = parseNum(tokens[i+1]);
            filename = tokens[i+2];
            return doSend(targetIpStr, port, verbose, filename, fs, currentDirLba);
        }
    }

    private static int parseNum(String s) {
        int val = 0;
        for(int i=0; i<s.length(); i++) {
            char c = s.charAt(i);
            if(c>='0' && c<='9') val = val * 10 + (c-'0');
        }
        return val;
    }

    private static String[] doListen(int port, int timeout, boolean verbose, String filename, FileSystem fs, int currentDirLba) {
        if (verbose) {
            log("Escuchando en UDP " + port + "... (Timeout: " + (timeout/100) + "s)");
        }
        Native.sys(Native.SYS_SLEEP, 10, 0, 0, 0); 
        
        byte[] rxBuffer = new byte[8192];
        DatagramPacket rxPacket = new DatagramPacket(rxBuffer, 8192);
        RawSocket raws = NetworkShell.getRawSocket();

        int attempts = 0;
        while (attempts < timeout) {
            rxPacket.setLength(8192);
            int len = raws.receive(rxPacket);

            if (len >= 42) {
                int etherType = ((rxBuffer[12] & 0xFF) << 8) | (rxBuffer[13] & 0xFF);
                
                // 1. Interceptar y responder peticiones ARP al vuelo para que el host remoto nos hable
                if (etherType == 0x0806 && (rxBuffer[20] & 0xFF) == 0x00 && (rxBuffer[21] & 0xFF) == 0x01) {
                    byte[] localIp = NetworkShell.getLocalIP();
                    boolean isOurIp = true;
                    for (int m = 0; m < 4; m++) {
                        if ((rxBuffer[38 + m] & 0xFF) != (localIp[m] & 0xFF)) { isOurIp = false; break; }
                    }
                    if (isOurIp) {
                        byte[] mac = NetworkShell.getMAC();
                        for (int m = 0; m < 6; m++) {
                            rxBuffer[m] = rxBuffer[6 + m];
                            rxBuffer[6 + m] = mac[m];
                        }
                        rxBuffer[21] = 0x02; // ARP Reply
                        
                        byte[] senderMac = new byte[6];
                        byte[] senderIp = new byte[4];
                        System.arraycopy(rxBuffer, 22, senderMac, 0, 6);
                        System.arraycopy(rxBuffer, 28, senderIp, 0, 4);
                        
                        System.arraycopy(mac, 0, rxBuffer, 22, 6);
                        System.arraycopy(localIp, 0, rxBuffer, 28, 4);
                        System.arraycopy(senderMac, 0, rxBuffer, 32, 6);
                        System.arraycopy(senderIp, 0, rxBuffer, 38, 4);
                        
                        raws.send(new DatagramPacket(rxBuffer, 60));
                        continue; // Responder y seguir esperando el UDP
                    }
                }

                // 2. Procesar el Payload UDP entrante
                if (etherType == 0x0800 && (rxBuffer[23] & 0xFF) == 17) { 
                    int ipHdrLen = (rxBuffer[14] & 0x0F) * 4;
                    int udpOffset = 14 + ipHdrLen;
                    int dstPort = ((rxBuffer[udpOffset+2] & 0xFF) << 8) | (rxBuffer[udpOffset+3] & 0xFF);

                    if (dstPort == port) {
                        int udpLen = ((rxBuffer[udpOffset+4] & 0xFF) << 8) | (rxBuffer[udpOffset+5] & 0xFF);
                        int payloadLen = udpLen - 8;
                        if (payloadLen > 0) {
                            byte[] fileData = new byte[payloadLen];
                            System.arraycopy(rxBuffer, udpOffset + 8, fileData, 0, payloadLen);
                            if (fs.writeFile(filename, fileData, currentDirLba)) {
                                if (verbose) log("Recibido y guardado: " + payloadLen + " bytes.");
                                return new String[]{"[+] Archivo " + filename + " recibido exitosamente."};
                            }
                            return new String[]{"[-] Error al escribir archivo en disco."};
                        }
                    }
                }
            }
            Native.sys(Native.SYS_SLEEP, 10, 0, 0, 0);
            attempts++;
        }
        return new String[]{"[-] Tiempo de escucha agotado."};
    }

    private static String[] doSend(String targetIpStr, int destPort, boolean verbose, String filename, FileSystem fs, int currentDirLba) {
        File f = fs.lookup(filename, currentDirLba, "");
        if (f == null || f.isDirectory()) return new String[]{"[-] Error: Archivo origen no encontrado."};
        byte[] payload = fs.readFile(f);
        if (payload == null || payload.length == 0) return new String[]{"[-] Error: Archivo vacio o ilegible."};        
        
        byte[] destIp = NetworkShell.parseIp(targetIpStr);
        byte[] nextHopIp = destIp;
        boolean sameSubnet = true;
        
        byte[] localIP = NetworkShell.getLocalIP();
        byte[] mask = NetworkShell.getMask();
        
        for(int i = 0; i < 4; i++) {
            if ((destIp[i] & mask[i]) != (localIP[i] & mask[i])){
                sameSubnet = false;
            }
        }
        if (!sameSubnet) {
            nextHopIp = NetworkShell.getGW();
        }

        byte[] destMac = ArpTable.get(nextHopIp);
        if (destMac == null) {
            if (verbose) { 
                log("Resolviendo ruta ARP para " + targetIpStr + "...");
            }
            String nextHopStr = (nextHopIp[0]&0xFF) + "." + (nextHopIp[1]&0xFF) + "." + (nextHopIp[2]&0xFF) + "." + (nextHopIp[3]&0xFF);
            NetworkShell.handleArpPing(nextHopStr);
            destMac = ArpTable.get(nextHopIp);
            if (destMac == null) {
                return new String[]{"[-] Error: Fallo ARP al enrutar."};
            }
        }

        int udpLen = 8 + payload.length;
        int ipTotalLen = 20 + udpLen;
        
        // [!] FIX: Runt Padding a 60 bytes obligatorios para Ethernet
        int frameSize = 14 + ipTotalLen;
        if (frameSize < 60) {
            frameSize = 60;
        }
        
        byte[] frame = new byte[frameSize];
        for (int i = 0; i < frame.length; i++){
            frame[i] = 0;
        }

        System.arraycopy(destMac, 0, frame, 0, 6);
        System.arraycopy(NetworkShell.getMAC(), 0, frame, 6, 6);
        frame[12] = 0x08; 
        frame[13] = 0x00;
        frame[14] = 0x45;
        frame[15] = 0x00;
        frame[16] = (byte)(ipTotalLen >> 8);
        frame[17] = (byte)ipTotalLen;
        frame[18] = 0x11; 
        frame[19] = 0x22;
        frame[20] = 0x00; 
        frame[21] = 0x00;
        frame[22] = 64;
        frame[23] = 17; 
        System.arraycopy(localIP, 0, frame, 26, 4);
        System.arraycopy(destIp, 0, frame, 30, 4);

        int ipCk = Checksum.calculate(frame, 14, 20);
        frame[24] = (byte)(ipCk >> 8); 
        frame[25] = (byte)ipCk;

        int udpOffset = 34;
        frame[udpOffset] = (byte)0xC0; frame[udpOffset+1] = (byte)0x00; 
        frame[udpOffset+2] = (byte)(destPort >> 8); frame[udpOffset+3] = (byte)destPort;
        frame[udpOffset+4] = (byte)(udpLen >> 8); frame[udpOffset+5] = (byte)udpLen;
        frame[udpOffset+6] = 0x00; frame[udpOffset+7] = 0x00;

        System.arraycopy(payload, 0, frame, udpOffset + 8, payload.length);

        if (verbose){
            log("Enviando " + payload.length + " bytes a " + targetIpStr + ":" + destPort);
        }
        RawSocket raws = NetworkShell.getRawSocket();
        raws.send(new DatagramPacket(frame, frame.length));

        return new String[]{"[+] Envio UDP finalizado exitosamente."};
    }

    private static void log(String msg) {
        System.out.println(msg);
        //Native.sys(Native.SYS_SERIAL_PUTS, 0, 0, msg + "\n", 0);		
    }
}
