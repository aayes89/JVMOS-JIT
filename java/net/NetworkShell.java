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

import java.net.NetworkAdapter;
import java.net.DatagramPacket;
import java.net.RawSocket;
import java.lang.Runtime;
import java.lang.Thread;
import java.awt.Graphics2D;
import java.awt.Color;
import java.lang.System;
import java.io.PrintStream;

public class NetworkShell {
    
    private static NetworkAdapter adapter;
    private static RawSocket rawSocket;
	private static int portBase;
	private static byte[] localIp = {(byte)10,(byte)10,(byte)10,(byte)99};  //   ip: 10.10.10.99
	private static byte[] mask = {(byte)255,(byte)255,(byte)255,(byte)0};	// mask: 255.255.255.0
	private static byte[] gw = {(byte)10,(byte)10,(byte)10,(byte)254}; 		//   gw: 10.10.10.254
	private static byte[] dns1 = {(byte)8,(byte)8,(byte)8,(byte)8}; 		// dns1: 8.8.8.8 google
	private static byte[] dns2 = {(byte)1,(byte)1,(byte)1,(byte)1}; 		// dns2: 1.1.1.1 one.one.one.one
    private static byte[] mac = new byte[6];
    
    // Inicializa los subsistemas de red
    public static void init(int ioPortBase) {
		portBase = ioPortBase;
        // Inicializa el adaptador RTL8139 por defecto
        adapter = new NetworkAdapter(NetworkAdapter.TYPE_RTL8139, ioPortBase);
        adapter.init();
        
        rawSocket = new RawSocket(ioPortBase);
    }

    // Procesa los comandos delegados desde Boot.java
    public static String[] execute(String netCmd, String arg) {
        if (adapter == null || !adapter.isInitialized()) {
            return new String[] { "[!] Error: Interfaz de red no inicializada." };
        }

        if (netCmd.equals("macconfig") || netCmd.equals("ifconfig")) {
            return handleIfconfig();
        } 
        else if(netCmd.equals("dhcp")){
            return handleDHCP();
        }
        else if (netCmd.equals("ip")) {
            return handleSetIp(arg);
        }
        else if (netCmd.equals("mask")) {
            return handleSetMask(arg);
        }
        else if (netCmd.equals("gw")) {
            return handleSetGw(arg);
        }
        else if (netCmd.equals("arp-ping")) {
            return handleArpPing(arg);
        }
        else if(netCmd.equals("ping")){
            return handleIcmpPing(arg);
        }
        else if(netCmd.equals("nslookup")){
            return handleNslookup(arg);
        }
        else if (netCmd.equals("wget")) {
            return handleWget(arg);
        }
        
        return new String[] { "Comandos de red: dhcp, ifconfig, ip, mask, gw, arp-ping, ping, nslookup, wget" };
    }

    private static String[] handleDHCP(){
        mac = getMacAddress();
        DhcpClient dhcp = new DhcpClient(adapter, mac);
        if(dhcp.discoverAndConfigure()){
            return new String[]{
                "[+] Red configurada via DHCP.",
                " -- Servicio de Red de JVMOS-JIT --",
                " Nombre de adaptador: eth0",
                " Link encap: Ethernet",
                macToString(" HWaddr (MAC): ", mac),
                ipToString (" inet addr: ", localIp),
                ipToString (" Mask: ", mask),
                ipToString (" Gateway: ", gw),
                ipToString (" DNS1: ", dns1),
                ipToString (" DNS2: ", dns2),
                " Estado: UP RUNNING | MTU: 1500"
            };
        }
        return new String[]{"[-] Error: No hay DHCP. Utiliza configuración estática (ip, mask, gw)."};
    }

    private static String[] handleIcmpPing(String targetIpStr) {
        if (targetIpStr.length() < 7) return new String[] { "Uso: net ping <IP>" };
        byte[] destIp = parseIp(targetIpStr);
        mac = getMacAddress();

        // Obtener la MAC destino (vía tabla ARP o directamente si es local)
        byte[] destMac = ArpTable.get(destIp);
        if (destMac == null) {
            handleArpPing(targetIpStr); // Resolver via ARP primero
            destMac = ArpTable.get(destIp);
            if (destMac == null) destMac = new byte[]{(byte)0xFF,(byte)0xFF,(byte)0xFF,(byte)0xFF,(byte)0xFF,(byte)0xFF};
        }

        // Trama Ethernet + IPv4 + ICMP Echo Request (74 bytes)
        byte[] frame = new byte[74];
        
        // Cabecera Ethernet
        System.arraycopy(destMac, 0, frame, 0, 6);
        System.arraycopy(mac, 0, frame, 6, 6);
        frame[12] = 0x08; frame[13] = 0x00; // IPv4

        // Cabecera IP (20 bytes)
        frame[14] = 0x45; frame[15] = 0x00;
        frame[16] = 0x00; frame[17] = 60; // Total Length 60
        frame[22] = 64;   frame[23] = 1;  // TTL 64, ICMP
        System.arraycopy(localIp, 0, frame, 26, 4);
        System.arraycopy(destIp, 0, frame, 30, 4);

        int ipCk = Checksum.calculate(frame, 14, 20);
        frame[24] = (byte)(ipCk >> 8); frame[25] = (byte)ipCk;

        // Cabecera ICMP (Echo Request: Type 8, Code 0)
        frame[34] = 8; frame[35] = 0;
        frame[38] = 0x01; frame[39] = 0x01; // Identifier
        frame[40] = 0x00; frame[41] = 0x01; // Sequence Number

        int icmpCk = Checksum.calculate(frame, 34, 40);
        frame[36] = (byte)(icmpCk >> 8); frame[37] = (byte)icmpCk;

        DatagramPacket txPacket = new DatagramPacket(frame, frame.length);
        rawSocket.send(txPacket);

        // Bucle de Escucha de ICMP Echo Reply (Type 0)
        byte[] rxBuffer = new byte[1536];
        DatagramPacket rxPacket = new DatagramPacket(rxBuffer, 1536);

        for (int i = 0; i < 100; i++) {
            int len = rawSocket.receive(rxPacket);
            if (len >= 42) {
                // Verificar IPv4 + ICMP Reply (Type 0)
                if (rxBuffer[12] == 0x08 && rxBuffer[13] == 0x00 && rxBuffer[23] == 1 && rxBuffer[34] == 0) {
                    return new String[] { "Respuesta ICMP (Ping) desde " + targetIpStr + ": bytes=" + len + " TTL=" + (rxBuffer[22] & 0xFF) };
                }
            }
            try { Thread.sleep(10); } catch (Exception e) {}
        }

        return new String[] { "Ping a " + targetIpStr + ": Tiempo de espera agotado." };
    }

    private static String[] handleNslookup(String domain) {
        if (domain.length() < 3) return new String[] { "Uso: net nslookup <dominio>" };
        return new String[] { "Consulta DNS enviada a " + ipToString("", dns1) + " para: " + domain, "[!] Resolutor UDP activo." };
    }

    private static String[] handleWget(String url) {
        if (url.length() < 4) return new String[] { "Uso: net wget <url>" };
        return new String[] { "Iniciando descarga HTTP GET desde: " + url + "...", "[!] Requiere Handshake TCP de la capa 4." };
    }
	
	private static byte[] getMacAddress(){
		byte[] mac = new byte[16];
		for(int i=0;i<6;i++){
			mac[i] = (byte) Runtime.inb(portBase + i);
		}
		return mac;
	}
	
	private static byte[] parseIp(String ipString){
		byte[] ip = new byte[4];
		int part = 0;
		int value = 0;
		for(int i=0;i < ipString.length(); i++){
			char c = ipString.charAt(i);
            if (c == '.') {
                ip[part++] = (byte) value;
                value = 0;
                if (part >= 4) break;
            } else if (c >= '0' && c <= '9') {
                value = value * 10 + (c - '0');
            }
		}
		if (part < 4) {
			ip[part] = (byte) value;
		}
        return ip;
    }

	private static String[] handleIfconfig() {
        mac = getMacAddress();
        
        return new String[] {
            " -- Servicio de Red de JVMOS-JIT --",
            " Nombre de adaptador: eth0",
            " Link encap: Ethernet",
            macToString(" HWaddr (MAC): ", mac),
            ipToString (" inet addr: ", localIp),
            ipToString (" Mask: ", mask),
            ipToString (" Gateway: ", gw),
            ipToString (" DNS1: ", dns1),
            ipToString (" DNS2: ", dns2),
            " Estado: UP RUNNING | MTU: 1500"
        };
    }
	
	private static String[] handleSetIp(String arg) {
        if (arg.length() < 7) return new String[] { "Uso: net ip <direccion_ip>" };
        localIp = parseIp(arg);
        return new String[] { ipToString("inet addr: ",localIp) };
    }

    private static String[] handleSetMask(String arg) {
        if (arg.length() < 7) return new String[] { "Uso: net mask <mascara>" };
        mask = parseIp(arg);
        return new String[] { ipToString("Mask: ",mask) };
    }

    private static String[] handleSetGw(String arg) {
        if (arg.length() < 7) return new String[] { "Uso: net gw <puerta_enlace>" };
        gw = parseIp(arg);
        return new String[] {ipToString("Gateway: ",gw) };
    }
	
    private static String[] handleArpPing(String targetIp) {
        if (targetIp.length() < 1) {
            return new String[] { "Uso: net arp-ping <IP>" };
        }
        
        byte[] destIp = parseIp(targetIp);
        byte[] srcMac = getMacAddress();
        
        // Trama completa de 60 bytes
		// los bytes 42 al 59 quedan a 0x00
        byte[] arpFrame = new byte[60]; 
                
        // CABECERA ETHERNET (14 bytes) - Wikipedia		
		// ----------------------------------------------------        
        for(int i = 0; i < 6; i++) arpFrame[i] = (byte) 0xFF; 	// MAC broadcast        
        for(int i = 0; i < 6; i++) arpFrame[6 + i] = srcMac[i]; // MAC origen        
        arpFrame[12] = 0x08; arpFrame[13] = 0x06;				// EtherType: ARP (0x0806)
        
        // MENSAJE ARP (28 bytes)
        // ----------------------------------------------------        
        arpFrame[14] = 0x00; arpFrame[15] = 0x01;				// Hardware: Ethernet (0x0001)        
        arpFrame[16] = 0x08; arpFrame[17] = 0x00;				// Protocolo: IPv4 (0x0800)        
        arpFrame[18] = 0x06; arpFrame[19] = 0x04;				// Longitud (MAC: 6 bytes) e (IPv4: 4 bytes)        
        arpFrame[20] = 0x00; arpFrame[21] = 0x01;				// Operación: (Request = 0x0001)
                
        for(int i = 0; i < 6; i++) arpFrame[22 + i] = srcMac[i]; // MAC local
        for(int i = 0; i < 4; i++) arpFrame[28 + i] = localIp[i];// IP local
        for(int i = 0; i < 6; i++) arpFrame[32 + i] = 0x00;	 	 // MAC destino        
        for(int i = 0; i < 4; i++) arpFrame[38 + i] = destIp[i]; // IP destino
        
        // Crear el DatagramPacket y enviarlo
        DatagramPacket packet = new DatagramPacket(arpFrame, arpFrame.length);                
        rawSocket.send(packet); 
				
        // ESCUCHA DE RESPUESTA (ARP Reply) con MTU 1500 + 36 bytes extras
        // ----------------------------------------------------
        byte[] rxBuffer = new byte[1536];
        DatagramPacket rxPacket = new DatagramPacket(rxBuffer, 1536); 
        
        // Bucle de Timeout ~1s: 100 veces con delay de 10ms 
        for (int attempt = 0; attempt < 100; attempt++) {
            int bytesRead = rawSocket.receive(rxPacket); 
            
            if (bytesRead > 0) {
                // Validar si es ARP (0x0806) o ARP Reply (0x0002)
                if (rxBuffer[12] == 0x08 && rxBuffer[13] == 0x06 && rxBuffer[20] == 0x00 && rxBuffer[21] == 0x02) {
					// Es la IP?
					boolean match = true;
					for (int i = 0; i < 4; i++) {
						if (rxBuffer[28 + i] != destIp[i]) {
							match = false;
							break;
                        }
					}
                    if (match) {
						String targetMac = toHex(rxBuffer[22]) + ":" + toHex(rxBuffer[23]) + ":" + 
						toHex(rxBuffer[24]) + ":" + toHex(rxBuffer[25]) + ":" + 
						toHex(rxBuffer[26]) + ":" + toHex(rxBuffer[27]);

                        return new String[] { 
							"ARP Request enviado a " + targetIp + "...",
							"Respuesta recibida: " + targetIp + " esta en la MAC " + targetMac
						};
                    }
                }
				// Restaurar la longitud del paquete para la siguiente lectura por si era otro tipo de tráfico
				rxPacket.setLength(1536); 
			}
			// Pausa de 10ms usando la syscall SLEEP (PIT IRQ0)
			//kernel.Native.sys(12, 10, 0, 0, 0);
			Thread.sleep(10);
		}
		return new String[] {
			"Enviando ARP Request (Broadcast) a " + targetIp + "...",
			"Tiempo de espera agotado. Host inalcanzable."
		};       
	}
	
	// Construye un String seguro: "Prefijo: 192.168.1.99"
    private static String ipToString(String prefix, byte[] ip) {
        byte[] buf = new byte[64];
        byte[] pref = prefix.getBytes();
        int pos = 0;
        
        for(int i = 0; i < pref.length; i++) {
            buf[pos++] = pref[i];
        }
        
        for (int i = 0; i < 4; i++) {
            int val = ip[i] & 0xFF;
            if (val >= 100) {
                buf[pos++] = (byte) ('0' + (val / 100));
                buf[pos++] = (byte) ('0' + ((val / 10) % 10));
                buf[pos++] = (byte) ('0' + (val % 10));
            } else if (val >= 10) {
                buf[pos++] = (byte) ('0' + (val / 10));
                buf[pos++] = (byte) ('0' + (val % 10));
            } else {
                buf[pos++] = (byte) ('0' + val);
            }
            if (i < 3) buf[pos++] = '.';
        }
        
        byte[] result = new byte[pos];
        System.arraycopy(buf, 0, result, 0, pos);
		System.out.println(new String(result));
        return new String(result);
    }

    // Construye un String seguro: "Prefijo: 00:11:22:33:44:55"
    private static String macToString(String prefix, byte[] mac) {
        byte[] buf = new byte[64];
        byte[] pref = prefix.getBytes();
        int pos = 0;
        
        for(int i = 0; i < pref.length; i++) {
            buf[pos++] = pref[i];
        }
        
        String hex = "0123456789ABCDEF";
        for (int i = 0; i < 6; i++) {
            int val = mac[i] & 0xFF;
            buf[pos++] = (byte) hex.charAt(val >>> 4);
            buf[pos++] = (byte) hex.charAt(val & 0x0F);
            if (i < 5) buf[pos++] = ':';
        }
        
        byte[] result = new byte[pos];
        System.arraycopy(buf, 0, result, 0, pos);
		System.out.println(new String(result));
        return new String(result);
    }
	
    // Convertidor a Hexadecimal
    private static String toHex(int b) {
        String hexChars = "0123456789ABCDEF";
        int high = (b & 0xF0) >> 4;
        int low = b & 0x0F;
        
        byte[] charArray = new byte[2];
        charArray[0] = (byte) hexChars.charAt(high);
        charArray[1] = (byte) hexChars.charAt(low);
        
        return new String(charArray);
    }

    // Setters y Getters
    public static void setLocalIP(byte[] ip){
        localIp = ip;
    }
    public static void setMask(byte[] mask){
        mask = mask;
    }
    public static void setGW(byte[] gw){
        gw = gw;
    }
    public static void setDNS1(byte[] dns){
        dns1 = dns;
    }
    public static void setDNS2(byte[] dns){
        dns2 = dns;
    }
}
