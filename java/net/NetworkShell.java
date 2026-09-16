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
import kernel.Native;

public class NetworkShell {
    
    private static NetworkAdapter adapter;
    private static RawSocket rawSocket;
	private static int portBase;			   // Puerto ioBase
	private static int type;				   // Tipo de Tarjeta
	private static byte[] localIp;             // IP Local
	private static byte[] mask;	               // Mascara de red
	private static byte[] gw; 		           // Gateway
	private static byte[] dns1; 		       // DNS primario
	private static byte[] dns2; 		       // DNS secundario (opcional)
    private static byte[] mac = new byte[6];
    
    // Inicializa los subsistemas de red
    public static void init(Graphics2D g) {
        portBase = detectNetworkCardIoPort(g);
        
        // Inicialización manual 
        localIp = new byte[]{(byte)10,(byte)0,(byte)2,(byte)15};    //   ip: 10.0.2.15
        mask = new byte[]{(byte)255,(byte)255,(byte)255,(byte)0};   // mask: 255.255.255.0
        gw = new byte[]{(byte)10,(byte)0,(byte)2,(byte)2};          //   gw: 10.0.2.2
        dns1 = new byte[]{(byte)8,(byte)8,(byte)8,(byte)8};         // dns1: 8.8.8.8 google
        dns2 = new byte[]{(byte)1,(byte)1,(byte)1,(byte)1};         // dns2: 1.1.1.1 one.one.one.one

		if(type == NetworkAdapter.TYPE_RTL8139){	// RTL8139 = 1
			// Inicializando el adaptador RTL8139
			adapter = new NetworkAdapter(NetworkAdapter.TYPE_RTL8139, portBase);
			adapter.init();
		}else if(type == NetworkAdapter.TYPE_PCNET){	// PCnet = 5
			// Inicializando el adaptador PCnet
			adapter = new NetworkAdapter(NetworkAdapter.TYPE_PCNET, portBase);
			adapter.init();			
		}
        
        rawSocket = new RawSocket(); 
    }

    // Detectar el puerto base de la tarjet de red (RTL8139 en QEMU y PCnet en VBox)
    public static int detectNetworkCardIoPort(Graphics2D g) {
		int posy = 40;
        for (int bus = 0; bus < 8; bus++) {
            for (int slot = 0; slot < 32; slot++) {
                // syscall 21: SYS_PCI_READ - Leer config de PCI
                int id = Native.sys(Native.SYS_PCI_READ, bus, slot, 0, 0x00);
                
                if (id != 0xFFFFFFFF && id != 0) {
                    int vendorId = id & 0xFFFF;
                    int deviceId = (id >>> 16) & 0xFFFF;
                    
                    String vendorIdHex = Integer.toHexString(vendorId);
                    String deviceIdHex = Integer.toHexString(deviceId);

                    if(deviceIdHex.equals("1111")){
                        System.out.println("PCI [" + bus + ":" + slot + "] Encontrado: Vendor 0x" + vendorIdHex + " Device 0x" + deviceId + " => QEMU Virtual Video Controller (VGA)");
						g.drawString("[+] Adaptador QEMU Virtual Video Controller encontrado",20,posy);
						posy+=10;
					}
                    else if(vendorId == 0x10EC && deviceId == 0x8139){
                        System.out.println("PCI [" + bus + ":" + slot + "] Encontrado: Vendor 0x" + vendorIdHex + " Device 0x" + deviceId + " => Realtek RTL8139");
						type = NetworkAdapter.TYPE_RTL8139;
						g.drawString("[+] Adaptador RTL8139 encontrado",20,posy);						
						posy+=10;
					}
                    else if(vendorId == 0x1022 && deviceId == 0x2000){
                        System.out.println("PCI [" + bus + ":" + slot + "] Encontrado: Vendor 0x" + vendorIdHex + " Device 0x" + deviceId + " => AMD PCnet-FAST III");
						type = NetworkAdapter.TYPE_PCNET;
						g.drawString("[+] Adaptador PCnet encontrado",20,posy);						
						posy+=10;
					}
					else if(vendorId == 0x8086){ // INTEL
						if(deviceId == 0x7000){
							System.out.println("PCI [" + bus + ":" + slot + "] Encontrado: Vendor 0x" + vendorIdHex + " Device 0x" + deviceId + " => Intel PIIX3 ISA Bridge");
							g.drawString("[+] Adaptador Intel PIIX3 ISA Bridge encontrado",20,posy);
							posy+=10;							
						}else if(deviceId == 0x1237){
							System.out.println("PCI [" + bus + ":" + slot + "] Encontrado: Vendor 0x" + vendorIdHex + " Device 0x" + deviceId + " => Intel 440FX (Natoma)");
							g.drawString("[+] Adaptador Intel 440FX encontrado",20,posy);
							posy+=10;							
						}else if(deviceId == 0x100e){
							System.out.println("PCI [" + bus + ":" + slot + "] Encontrado: Vendor 0x" + vendorIdHex + " Device 0x" + deviceId + " => Intel PRO/1000");
							type = NetworkAdapter.TYPE_E1000;
							g.drawString("[+] Adaptador Intel PRO/1000 encontrado",20,posy);						
							posy+=10;
						}else{
							System.out.println("PCI [" + bus + ":" + slot + "] Encontrado: Vendor 0x" + vendorIdHex + " Device 0x" + deviceId + " => Intel PRO/1000");				
							g.drawString("[+] Adaptador Intel encontrado",20,posy);						
							posy+=10;
						}
					}
					else if(vendorId == 0x106B){ // Apple
						System.out.println("PCI [" + bus + ":" + slot + "] Encontrado: Vendor 0x" + vendorIdHex + " Device 0x" + deviceId + " => Intel PRO/1000");
						g.drawString("[+] Adaptador Apple encontrado",20,posy);						
						posy+=10;						
					}
					else if(vendorId == 0x15DA && deviceId == 0x1029){
                        System.out.println("PCI [" + bus + ":" + slot + "] Encontrado: Vendor 0x" + vendorIdHex + " Device 0x" + deviceId + " => VMware");
						g.drawString("[+] Adaptador VMware encontrado",20,posy);						
						posy+=10;
					}
                    else {
                        System.out.println("PCI [" + bus + ":" + slot + "] Encontrado: Vendor 0x" + vendorIdHex + " Device 0x" + deviceId);
						g.drawString("Adaptador 0x"+vendorIdHex+" Device 0x"+deviceId+" encontrado",20,posy);
						posy+=10;
					}
                                       
                    // Si hay Realtek (QEMU) o AMD (VirtualBox)
                    if ((vendorId == 0x10EC && deviceId == 0x8139) || (vendorId == 0x1022 && deviceId == 0x2000)) {
                        // Leer Command Register (Offset 0x04)
                        int cmd = Native.sys(Native.SYS_PCI_READ, bus, slot, 0, 0x04);
                        
                        // Activar Bit 2 (Bus Master) y Bit 0 (I/O Space)
                        cmd |= 0x0005; 
                        
                        // Syscall 29: SYS_PCI_WRITE - Escribir la configuración de vuelta
                        Native.sys(Native.SYS_PCI_WRITE, bus, slot, 0x04, cmd);

                        for (int barOffset = 0x10; barOffset <= 0x24; barOffset += 4) {
                            int bar = Native.sys(Native.SYS_PCI_READ, bus, slot, 0, barOffset);
                            // Verificar si es un puerto I/O (Bit 0 debe ser 1)
                            if ((bar & 0x1) == 1) {
                                return bar & ~0x3;
                            }
                        }
                    }
                }
            }
        }
        System.out.println("[!] Tarjeta de red compatible no detectada por PCI. Forzando puerto I/O: 0xC000");
        return 0xC000; // Fallback 
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

    // Obtención de configuración de red vía servicio DHCP si estuviera disponible
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
        return new String[]{"[-] Error: No hay DHCP. Utiliza las funciones (ip, mask, gw) para modo manual."};
    }

    // Implementación del comando PING adaptado para JVMOS-JIT
    private static String[] handleIcmpPing(String targetIpStr) {
        if (targetIpStr.length() < 7) return new String[] { "Uso: net ping <IP>" };
        byte[] destIp = parseIp(targetIpStr);
        mac = getMacAddress();

        byte[] nextHopIp = destIp;
        boolean sameSubnet = true;
        for(int i = 0; i < 4; i++) {
            if ((destIp[i] & mask[i]) != (localIp[i] & mask[i])) sameSubnet = false;
        }
        if (!sameSubnet) {
            nextHopIp = gw;
        }

        byte[] destMac = ArpTable.get(nextHopIp);
        if (destMac == null) {
            String nextHopStr = (nextHopIp[0]&0xFF) + "." + (nextHopIp[1]&0xFF) + "." + 
                                (nextHopIp[2]&0xFF) + "." + (nextHopIp[3]&0xFF);
            handleArpPing(nextHopStr); 
            
            destMac = ArpTable.get(nextHopIp);
            if (destMac == null) {
                return new String[] { "Ping a " + targetIpStr + ": Fallo al resolver MAC de ruteo." };
            }
        }

        byte[] frame = new byte[74];
        // Limpiar memoria residual de la RAM
        for (int i = 0; i < frame.length; i++) frame[i] = 0;

        System.arraycopy(destMac, 0, frame, 0, 6);
        System.arraycopy(mac, 0, frame, 6, 6);
        frame[12] = 0x08; frame[13] = 0x00; 

        frame[14] = 0x45; frame[15] = 0x00;
        frame[16] = 0x00; frame[17] = 60; 
        
        frame[18] = 0x12; frame[19] = 0x34; // ID IP 
        frame[20] = 0x00; frame[21] = 0x00; // Asegurar Flags limpios
        
        frame[22] = 64;   frame[23] = 1;  
        System.arraycopy(localIp, 0, frame, 26, 4);
        System.arraycopy(destIp, 0, frame, 30, 4);

        int ipCk = Checksum.calculate(frame, 14, 20);
        frame[24] = (byte)(ipCk >> 8); frame[25] = (byte)ipCk;

        frame[34] = 8; frame[35] = 0;
        frame[38] = 0x0A; frame[39] = 0x0B; 
        frame[40] = 0x00; frame[41] = 0x01; 

        int icmpCk = Checksum.calculate(frame, 34, 40);
        frame[36] = (byte)(icmpCk >> 8); frame[37] = (byte)icmpCk;

        DatagramPacket txPacket = new DatagramPacket(frame, frame.length);
        rawSocket.send(txPacket);

        byte[] rxBuffer = new byte[1536];
        DatagramPacket rxPacket = new DatagramPacket(rxBuffer, 1536);

        for (int i = 0; i < 150; i++) { // Timeout de 1.5s
            rxPacket.setLength(1536); 
            int len = rawSocket.receive(rxPacket);
            
            if (len >= 42) {
                int ipHdrLen = (rxBuffer[14] & 0x0F) * 4;
                int icmpOffset = 14 + ipHdrLen;
                // Si es IPv4, ICMP y Echo Reply
                if ((rxBuffer[12] & 0xFF) == 0x08 && (rxBuffer[13] & 0xFF) == 0x00 && 
                    (rxBuffer[23] & 0xFF) == 1 && (rxBuffer[icmpOffset] & 0xFF) == 0) {
					// Verificamos IP del PING
                    if ((rxBuffer[26] & 0xFF) == (destIp[0] & 0xFF) && 
                        (rxBuffer[27] & 0xFF) == (destIp[1] & 0xFF) && 
                        (rxBuffer[28] & 0xFF) == (destIp[2] & 0xFF) && 
                        (rxBuffer[29] & 0xFF) == (destIp[3] & 0xFF)) {
                        
                        return new String[] { "Respuesta de " + targetIpStr + ": bytes=" + len + " TTL=" + (rxBuffer[22] & 0xFF) };
                    }
                }
            }
            try { Thread.sleep(10); } catch (Exception e) {}
        }
        return new String[] { "Ping a " + targetIpStr + ": Tiempo de espera agotado." };
    }
	
    // TODO - Réplica de comando para consultar dirección en internet
    private static String[] handleNslookup(String domain) {
        if (domain.length() < 3) {
            return new String[] { "Uso: net nslookup <dominio>" };
        }
        return new String[] { "Consulta DNS enviada a " + ipToString("", dns1) + " para: " + domain, "[!] Resolutor UDP activo." };
    }

    // TODO - réplica de comando en Linux para descargar
    private static String[] handleWget(String url) {
        if (url.length() < 4) {
            return new String[] { "Uso: net wget <url>" };
        }
        return new String[] { "Iniciando descarga HTTP GET desde: " + url + "...", "[!] Requiere Handshake TCP de la capa 4." };
    }
	
    // Obtiene la MAC de la tarjeta de red (funciona en QEMU)
	private static byte[] getMacAddress(){ 
		byte[] mac = new byte[16];
		for(int i=0;i<6;i++){
			mac[i] = (byte) Runtime.inb(portBase + i);
		}
		return mac;
	}
	
    // Obtiene la IP de una cadena de texto y la convierte a arreglo de bytes
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

    // equivalente a ifconfig de una interfaz activa en Linux
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
	
    // Establecer la IP manualmente
	private static String[] handleSetIp(String arg) {
        if (arg.length() < 7) return new String[] { "Uso: net ip <direccion_ip>" };
        localIp = parseIp(arg);
        return new String[] { ipToString("inet addr: ",localIp) };
    }

    // Establecer la Máscara de red manualmente
    private static String[] handleSetMask(String arg) {
        if (arg.length() < 7) return new String[] { "Uso: net mask <mascara>" };
        mask = parseIp(arg);
        return new String[] { ipToString("Mask: ",mask) };
    }

    // Establecer el Gateway manualmente
    private static String[] handleSetGw(String arg) {
        if (arg.length() < 7) return new String[] { "Uso: net gw <puerta_enlace>" };
        gw = parseIp(arg);
        return new String[] {ipToString("Gateway: ",gw) };
    }
	
    // Hacer PING vía ARP
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
        for(int i = 0; i < 6; i++) arpFrame[i] = (byte) 0xFF;   // MAC broadcast        
        for(int i = 0; i < 6; i++) arpFrame[6 + i] = srcMac[i]; // MAC origen        
        arpFrame[12] = 0x08; arpFrame[13] = 0x06;               // EtherType: ARP (0x0806)
        
        // MENSAJE ARP (28 bytes)
        // ----------------------------------------------------        
        arpFrame[14] = 0x00; arpFrame[15] = 0x01;               // Hardware: Ethernet (0x0001)        
        arpFrame[16] = 0x08; arpFrame[17] = 0x00;               // Protocolo: IPv4 (0x0800)        
        arpFrame[18] = 0x06; arpFrame[19] = 0x04;               // Longitud (MAC: 6 bytes) e (IPv4: 4 bytes)        
        arpFrame[20] = 0x00; arpFrame[21] = 0x01;               // Operación: (Request = 0x0001)
                
        for(int i = 0; i < 6; i++) arpFrame[22 + i] = srcMac[i]; // MAC local
        for(int i = 0; i < 4; i++) arpFrame[28 + i] = localIp[i];// IP local
        for(int i = 0; i < 6; i++) arpFrame[32 + i] = 0x00;      // MAC destino        
        for(int i = 0; i < 4; i++) arpFrame[38 + i] = destIp[i]; // IP destino
        
        // Crear el DatagramPacket y enviarlo
        DatagramPacket packet = new DatagramPacket(arpFrame, arpFrame.length);                
        rawSocket.send(packet); 
                
        // (ARP Reply) con MTU 1500 + 36 bytes extras
        // ----------------------------------------------------
        byte[] rxBuffer = new byte[1536];
        DatagramPacket rxPacket = new DatagramPacket(rxBuffer, 1536); 
        
        // Bucle de Timeout ~1s: 100 veces con delay de 10ms 
        for (int attempt = 0; attempt < 100; attempt++) {
            // Restaurar siempre el tamaño MÁXIMO antes de intentar leer
            rxPacket.setLength(1536); 
            
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
                        // Guardar en la Caché ARP
                        byte[] targetMacBytes = new byte[6];
                        System.arraycopy(rxBuffer, 22, targetMacBytes, 0, 6);
                        ArpTable.put(destIp, targetMacBytes);

                        String targetMac = toHex(rxBuffer[22]) + ":" + toHex(rxBuffer[23]) + ":" + 
                        toHex(rxBuffer[24]) + ":" + toHex(rxBuffer[25]) + ":" + 
                        toHex(rxBuffer[26]) + ":" + toHex(rxBuffer[27]);

                        return new String[] { 
                            "ARP Request enviado a " + targetIp + "...",
                            "Respuesta recibida: " + targetIp + " esta en la MAC " + targetMac
                        };
                    }
                }
            }
            try { Thread.sleep(10); } catch (Exception e) {}
        }
        return new String[] {
            "Enviando ARP Request (Broadcast) a " + targetIp + "...",
            "Tiempo de espera agotado. Host inalcanzable."
        };       
    }
	
	// Construye el String de manera segura evitando el operador '+'
    private static String ipToString(String prefix, byte[] ip) {
        StringBuilder sb = new StringBuilder(prefix);
        sb.append(ip[0] & 0xFF).append('.');
        sb.append(ip[1] & 0xFF).append('.');
        sb.append(ip[2] & 0xFF).append('.');
        sb.append(ip[3] & 0xFF);
        return sb.toString();
    }

    // Convierte MAC a cadena de texto formato => ##:##:##:##:##:##
    private static String macToString(String prefix, byte[] mac) {
        String hex = "0123456789ABCDEF";
        StringBuilder sb = new StringBuilder(prefix);
        for (int i = 0; i < 6; i++) {
            int val = mac[i] & 0xFF;
            sb.append(hex.charAt(val >>> 4)).append(hex.charAt(val & 0x0F));
            if (i < 5) sb.append(':');
        }
        return sb.toString();
    }
	
    // Convierte un entero a su valor hexadecimal
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
        NetworkShell.localIp = ip;
    }
    public static void setMask(byte[] mask){
        NetworkShell.mask = mask;
    }
    public static void setGW(byte[] gw){
        NetworkShell.gw = gw;
    }
    public static void setDNS1(byte[] dns){
        NetworkShell.dns1 = dns;
    }
    public static void setDNS2(byte[] dns){
        NetworkShell.dns2 = dns;
    }
}
