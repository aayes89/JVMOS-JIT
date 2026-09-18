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
		}
		else if(type == NetworkAdapter.TYPE_RTL8168){	// RTL8168 = 2
			// Inicializando el adaptador RTL8168
			adapter = new NetworkAdapter(NetworkAdapter.TYPE_RTL8168, portBase);
			adapter.init();			
		}
		else if(type == NetworkAdapter.TYPE_PCNET){	// PCnet = 6
			// Inicializando el adaptador PCnet
			adapter = new NetworkAdapter(NetworkAdapter.TYPE_PCNET, portBase);
			adapter.init();			
		}

		rawSocket = new RawSocket(); 
		
		// Demonio de red en segundo plano 
		NetworkDaemon daemon = new NetworkDaemon(rawSocket, localIp, getMacAddress());
		Thread daemonThread = new Thread(daemon);
		daemonThread.start();
    }

    // Detectar el puerto base de la tarjet de red (RTL8139 en QEMU y PCnet en VBox)
	public static int detectNetworkCardIoPort(Graphics2D g) {
        int posy = 40;
        int foundIoPort = 0xC000;
        boolean cardFound = false;

        for (int bus = 0; bus < 8; bus++) {
            for (int slot = 0; slot < 32; slot++) {
                int id = Native.sys(Native.SYS_PCI_READ, bus, slot, 0, 0x00);
                
                if (id != 0xFFFFFFFF && id != 0) {
                    int vendorId = id & 0xFFFF;
                    int deviceId = (id >>> 16) & 0xFFFF;

                    if (vendorId == 0x10EC && deviceId == 0x8139) {
                        type = NetworkAdapter.TYPE_RTL8139;
                        g.drawString("[+] RTL8139 detectado", 20, posy); posy += 10;
                        cardFound = true;
                    } else if (vendorId == 0x10EC && deviceId == 0x8168) {
                        type = NetworkAdapter.TYPE_RTL8168;
                        g.drawString("[+] RTL8168 Gigabit detectado", 20, posy); posy += 10;
                        cardFound = true;
                    } else if (vendorId == 0x1022 && deviceId == 0x2000) {
                        type = NetworkAdapter.TYPE_PCNET;
                        g.drawString("[+] PCnet detectado", 20, posy); posy += 10;
                        cardFound = true;
                    } else if (vendorId == 0x8086 && deviceId == 0x100E) {
                        type = NetworkAdapter.TYPE_E1000;
                        g.drawString("[+] Intel PRO/1000 detectado", 20, posy); posy += 10;
                        cardFound = true;
                    }

                    if (cardFound) {
                        // Activar Bus Master e I/O
                        int cmd = Native.sys(Native.SYS_PCI_READ, bus, slot, 0, 0x04);
                        cmd |= 0x0005; 
                        Native.sys(Native.SYS_PCI_WRITE, bus, slot, 0x04, cmd);

                        for (int barOffset = 0x10; barOffset <= 0x24; barOffset += 4) {
                            int bar = Native.sys(Native.SYS_PCI_READ, bus, slot, 0, barOffset);
                            if ((bar & 0x1) == 1) {
                                foundIoPort = bar & ~0x3;
                                return foundIoPort; // Retornar puerto I/O válido inmediatamente
                            }
                        }
                        
                        g.drawString("[-] Error: La tarjeta no soporta I/O heredado (Solo MMIO).", 20, posy);
                        return foundIoPort; // Devuelve 0xC000 si no hay I/O
                    }
                }
            }
        }
        return foundIoPort; 
    }
	/* 
	versión antigua
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

                    /*if (vendorId == 0x1234 && deviceId == 0x1111) { // QEMU
                        System.out.println("PCI [" + bus + ":" + slot + "] Encontrado: Vendor 0x" + vendorIdHex + " Device 0x" + deviceIdHex + " => QEMU Virtual Video Controller (VGA)");
                        g.drawString("[+] Adaptador QEMU Virtual Video Controller encontrado", 20, posy);
                        posy += 10;
                    }
                    else* 
					if (vendorId == 0x10EC){ // Realtek
						if(deviceId == 0x8139) {
							System.out.println("PCI [" + bus + ":" + slot + "] Encontrado: Vendor 0x" + vendorIdHex + " Device 0x" + deviceIdHex + " => Realtek RTL8139");
							type = NetworkAdapter.TYPE_RTL8139;
							g.drawString("[+] Adaptador RTL8139 encontrado", 20, posy);                        
							posy += 10;
						}						
						else if (deviceId == 0x8168) {
							System.out.println("PCI [" + bus + ":" + slot + "] Encontrado: Vendor 0x" + vendorIdHex + " Device 0x" + deviceIdHex + " => Realtek RTL8168 Gigabit Ethernet");
							type = NetworkAdapter.TYPE_RTL8168;
							g.drawString("[+] Adaptador Realtek RTL8168 Gigabit Ethernet encontrado", 20, posy);
							posy += 10;
						}					
						/*if (deviceId == 0x8136) {
							System.out.println("PCI [" + bus + ":" + slot + "] Encontrado: Vendor 0x" + vendorIdHex + " Device 0x" + deviceIdHex + " => Realtek RTL810xE Fast Ethernet");
							g.drawString("[+] Adaptador Realtek RTL810xE Fast Ethernet encontrado", 20, posy);
							posy += 10;
						}
						else if (deviceId == 0xC822) {
							System.out.println("PCI [" + bus + ":" + slot + "] Encontrado: Vendor 0x" + vendorIdHex + " Device 0x" + deviceIdHex + " => Realtek RTL8822CE Wi-Fi");
							g.drawString("[+] Adaptador Realtek RTL8822CE Wi-Fi encontrado", 20, posy);
							posy += 10;
						} else {
							System.out.println("PCI [" + bus + ":" + slot + "] Encontrado: Vendor 0x" + vendorIdHex + " Device 0x" + deviceIdHex + "=> Dispositivo Realtek Desconocido");
							g.drawString("[+] Adaptador 0x" + vendorIdHex + " Device 0x" + deviceIdHex + " de Realtek encontrado", 20, posy);
							posy += 10;
						}*
					}
                    else if (vendorId == 0x1022){ // AMD
						if(deviceId == 0x2000) {
							System.out.println("PCI [" + bus + ":" + slot + "] Encontrado: Vendor 0x" + vendorIdHex + " Device 0x" + deviceIdHex + " => AMD PCnet-FAST III");
							type = NetworkAdapter.TYPE_PCNET;
							g.drawString("[+] Adaptador PCnet encontrado", 20, posy);                        
							posy += 10;
						}
						/* else {
							System.out.println("PCI [" + bus + ":" + slot + "] Encontrado: Vendor 0x" + vendorIdHex + " Device 0x" + deviceIdHex + "=> Dispositivo AMD Desconocido");
							g.drawString("[+] Adaptador 0x" + vendorIdHex + " Device 0x" + deviceIdHex + " de AMD encontrado", 20, posy);
							posy += 10;
						}
						else if (deviceId == 0x15D0) {
							System.out.println("PCI [" + bus + ":" + slot + "] Encontrado: Vendor 0x" + vendorIdHex + " Device 0x" + deviceIdHex + " => AMD PCIe Host Bridge");
							g.drawString("[+] Adaptador AMD PCIe Host Bridge encontrado", 20, posy);
							posy += 10;
						}
						else if (deviceId == 0x15D8) {
							System.out.println("PCI [" + bus + ":" + slot + "] Encontrado: Vendor 0x" + vendorIdHex + " Device 0x" + deviceIdHex + " => AMD Radeon Vega Graphics");
							g.drawString("[+] Adaptador AMD Radeon Vega Graphics encontrado", 20, posy);
							posy += 10;
						}
						else if (deviceId == 0x15DE) {
							System.out.println("PCI [" + bus + ":" + slot + "] Encontrado: Vendor 0x" + vendorIdHex + " Device 0x" + deviceIdHex + " => AMD Audio Coprocessor");
							g.drawString("[+] Adaptador AMD Audio Coprocessor encontrado", 20, posy);
							posy += 10;
						}
						else if (deviceId == 0x15DF) {
							System.out.println("PCI [" + bus + ":" + slot + "] Encontrado: Vendor 0x" + vendorIdHex + " Device 0x" + deviceIdHex + " => AMD PCIe Root Port");
							g.drawString("[+] Adaptador AMD PCIe Root Port encontrado", 20, posy);
							posy += 10;
						}
						else if (deviceId == 0x15E3) {
							System.out.println("PCI [" + bus + ":" + slot + "] Encontrado: Vendor 0x" + vendorIdHex + " Device 0x" + deviceIdHex + " => AMD Audio Processor");
							g.drawString("[+] Adaptador AMD Audio Processor encontrado", 20, posy);
							posy += 10;
						}*						
                    } else if (vendorId == 0x8086) { // INTEL
						if (deviceId == 0x100E) {
                            System.out.println("PCI [" + bus + ":" + slot + "] Encontrado: Vendor 0x" + vendorIdHex + " Device 0x" + deviceIdHex + " => E1000");
                            type = NetworkAdapter.TYPE_E1000;
                            g.drawString("[+] Adaptador E1000 encontrado", 20, posy);                        
                            posy += 10;
						}
					}
                        /*if (deviceId == 0x7000) {
                            System.out.println("PCI [" + bus + ":" + slot + "] Encontrado: Vendor 0x" + vendorIdHex + " Device 0x" + deviceIdHex + " => Intel PIIX3 ISA Bridge");
                            g.drawString("[+] Adaptador Intel PIIX3 ISA Bridge encontrado", 20, posy);
                            posy += 10;                            
                        } else if (deviceId == 0x1237) {
                            System.out.println("PCI [" + bus + ":" + slot + "] Encontrado: Vendor 0x" + vendorIdHex + " Device 0x" + deviceIdHex + " => Intel 440FX (Natoma)");
                            g.drawString("[+] Adaptador Intel 440FX encontrado", 20, posy);
                            posy += 10;                            
                        } else {
							System.out.println("PCI [" + bus + ":" + slot + "] Encontrado: Vendor 0x" + vendorIdHex + " Device 0x" + deviceIdHex + "=> Dispositivo Intel Desconocido");
							g.drawString("[+] Adaptador 0x" + vendorIdHex + " Device 0x" + deviceIdHex + " de Intel encontrado", 20, posy);
							posy += 10;
						}	 									
                    } 
                    else if (vendorId == 0x106B) { // Apple
                        System.out.println("PCI [" + bus + ":" + slot + "] Encontrado: Vendor 0x" + vendorIdHex + " Device 0x" + deviceIdHex + " => Apple Device");
                        g.drawString("[+] Adaptador Apple encontrado", 20, posy);                        
                        posy += 10;                        
                    }
                    else if (vendorId == 0x15AD && deviceId == 0x1029) { // VMware (corregido 15DA a 15AD)
                        System.out.println("PCI [" + bus + ":" + slot + "] Encontrado: Vendor 0x" + vendorIdHex + " Device 0x" + deviceIdHex + " => Dispositivo VMware encontrado");
                        g.drawString("[+] Adaptador VMware encontrado", 20, posy);                        
                        posy += 10;
                    } else {
                        System.out.println("PCI [" + bus + ":" + slot + "] Encontrado: Vendor 0x" + vendorIdHex + " Device 0x" + deviceIdHex);
                        g.drawString("[+] Adaptador 0x" + vendorIdHex + " Device 0x" + deviceIdHex + " desconocido encontrado", 20, posy);
                        posy += 10;
                    } *                                  
                    
                                       
                    // Si hay Realtek (QEMU) o AMD (VirtualBox)
                    if ((vendorId == 0x10EC && deviceId == 0x8139) || (vendorId == 0x1022 && deviceId == 0x2000) || (vendorId == 0x10EC && deviceId == 0x8168)) {
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
	*/

    // Procesa los comandos delegados desde Boot.java
    public static String[] execute(String netCmd, String arg) {
        if (adapter == null || !adapter.isInitialized()) {
            return new String[] { "[!] Error: Interfaz de red no inicializada." };
        }
        if (netCmd.equals("ifconfig")) {
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
		else if(netCmd.equals("dns1")){
			return handleSetDNS(arg,1);
		}
		else if(netCmd.equals("dns2")){
			return handleSetDNS(arg,2);
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
        
        return new String[] { 
			"Comandos de red:\n",
			"dhcp - obtener ip autom\u00E1tica",
			"ifconfig - ver configuraci\u00F3n de red",
			"ip - establecer ip manualmente",
			"mask - establecer m\u00E1scara de red manualmente",
			"gw - establecer Gateway manualmente",
			"dns1 - establecer DNS1 manualmente",
			"dns2 - establecer DNS2 manualmente",
			"arp-ping - comando PING con ARP",
			"ping - comando PING con ICMP",
			"nslookup - comando de red",
			"wget - TODO"
		};
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
	
    // Réplica de comando para consultar dirección en internet
    private static String[] handleNslookup(String domain) {
        if (domain.length() < 3) return new String[] { "Uso: net nslookup <dominio>" };

        byte[] destIp = dns1; // Uso el DNS primario
        mac = getMacAddress();

        // Ruteo (ARP al Gateway si el DNS está fuera de la subred)
        byte[] nextHopIp = destIp;
        boolean sameSubnet = true;
        for(int i = 0; i < 4; i++) {
            if ((destIp[i] & mask[i]) != (localIp[i] & mask[i])) sameSubnet = false;
        }
        if (!sameSubnet) nextHopIp = gw;

        byte[] destMac = ArpTable.get(nextHopIp);
        if (destMac == null) {
            String nextHopStr = ipToString("", nextHopIp);
            handleArpPing(nextHopStr); 
            destMac = ArpTable.get(nextHopIp);
            if (destMac == null) return new String[] { "Error: Fallo al resolver MAC del Gateway." };
        }

        // El Payload DNS
        byte[] qname = encodeDomainName(domain);
        int dnsPayloadLen = 12 + qname.length + 4; // Header(12) + QNAME + QTYPE(2) + QCLASS(2)
        int udpLen = 8 + dnsPayloadLen;
        int ipTotalLen = 20 + udpLen;
        
        byte[] frame = new byte[14 + ipTotalLen];
        for (int i = 0; i < frame.length; i++) frame[i] = 0;

        // Cabecera Ethernet
        System.arraycopy(destMac, 0, frame, 0, 6);
        System.arraycopy(mac, 0, frame, 6, 6);
        frame[12] = 0x08; frame[13] = 0x00; // IPv4

        // Cabecera IPv4 
        frame[14] = 0x45; frame[15] = 0x00;
        frame[16] = (byte)(ipTotalLen >> 8); frame[17] = (byte)ipTotalLen;
        frame[18] = 0x11; frame[19] = 0x22; // ID IP
        frame[20] = 0x00; frame[21] = 0x00; // Flags
        frame[22] = 64;   frame[23] = 17;   // TTL=64, Protocolo=17 (UDP)
        System.arraycopy(localIp, 0, frame, 26, 4);
        System.arraycopy(destIp, 0, frame, 30, 4);
        
        int ipCk = Checksum.calculate(frame, 14, 20);
        frame[24] = (byte)(ipCk >> 8); frame[25] = (byte)ipCk;

        // Cabecera UDP
        int udpOffset = 34;
        frame[udpOffset] = (byte)0xC0; frame[udpOffset+1] = (byte)0x00; // Src Port: 49152
        frame[udpOffset+2] = 0x00;     frame[udpOffset+3] = 53;         // Dst Port: 53 (DNS)
        frame[udpOffset+4] = (byte)(udpLen >> 8); frame[udpOffset+5] = (byte)udpLen;
        frame[udpOffset+6] = 0x00;     frame[udpOffset+7] = 0x00;       // UDP Checksum (Opcional en IPv4 = 0)

        // Cabecera DNS
        int dnsOffset = 42;
        frame[dnsOffset] = 0x12; frame[dnsOffset+1] = 0x34;       // Transaction ID
        frame[dnsOffset+2] = 0x01; frame[dnsOffset+3] = 0x00;     // Flags: Standard Query
        frame[dnsOffset+4] = 0x00; frame[dnsOffset+5] = 0x01;     // Questions: 1
        // ANCOUNT, NSCOUNT, ARCOUNT ya son 0

        // Consulta DNS
        System.arraycopy(qname, 0, frame, dnsOffset + 12, qname.length);
        int qEnd = dnsOffset + 12 + qname.length;
        frame[qEnd] = 0x00; frame[qEnd+1] = 0x01;     // QTYPE: A (Host Address)
        frame[qEnd+2] = 0x00; frame[qEnd+3] = 0x01;   // QCLASS: IN (Internet)

        // Enviar y Esperar
        rawSocket.send(new DatagramPacket(frame, frame.length));

        byte[] rxBuffer = new byte[1536];
        DatagramPacket rxPacket = new DatagramPacket(rxBuffer, 1536);
        int attempts = 0;
        
        while (attempts < 200) { 
            rxPacket.setLength(1536); 
            int len = rawSocket.receive(rxPacket);
            
            if (len >= 42) {
                int etherType = ((rxBuffer[12] & 0xFF) << 8) | (rxBuffer[13] & 0xFF);
                
                // Intercepto ARP Requests entrantes para no ahogar al servidor DNS
                if (etherType == 0x0806 && (rxBuffer[20] & 0xFF) == 0x00 && (rxBuffer[21] & 0xFF) == 0x01) {
                    boolean isOurIp = true;
                    for (int m = 0; m < 4; m++) {
                        if ((rxBuffer[38 + m] & 0xFF) != (localIp[m] & 0xFF)) { isOurIp = false; break; }
                    }
                    
                    if (isOurIp) {
                        for (int m = 0; m < 6; m++) {
                            rxBuffer[m] = rxBuffer[6 + m]; 
                            rxBuffer[6 + m] = mac[m];      
                        }
                        rxBuffer[21] = 0x02; 
                        
                        byte[] senderMac = new byte[6];
                        byte[] senderIp = new byte[4];
                        System.arraycopy(rxBuffer, 22, senderMac, 0, 6);
                        System.arraycopy(rxBuffer, 28, senderIp, 0, 4);
                        
                        System.arraycopy(mac, 0, rxBuffer, 22, 6);
                        System.arraycopy(localIp, 0, rxBuffer, 28, 4);
                        System.arraycopy(senderMac, 0, rxBuffer, 32, 6);
                        System.arraycopy(senderIp, 0, rxBuffer, 38, 4);
                        
                        rawSocket.send(new DatagramPacket(rxBuffer, 60));
                        continue; // Responder ARP y seguir esperando la respuesta DNS
                    }
                }

                // Proceso las respuestas UDP / DNS
                if (etherType == 0x0800) {
                    int ipHdrLen = (rxBuffer[14] & 0x0F) * 4;
                    int protocol = rxBuffer[23] & 0xFF;

                    // Si es UDP (17)
                    if (protocol == 17) {
                        int rUdpOffset = 14 + ipHdrLen;
                        int srcPort = ((rxBuffer[rUdpOffset] & 0xFF) << 8) | (rxBuffer[rUdpOffset+1] & 0xFF);
                        int dstPort = ((rxBuffer[rUdpOffset+2] & 0xFF) << 8) | (rxBuffer[rUdpOffset+3] & 0xFF);
                        
                        // Validar que viene del puerto 53 hacia nuestro puerto 49152
                        if (srcPort == 53 && dstPort == 49152) {
                            int rDnsOffset = rUdpOffset + 8;
                            
                            // Verificar que sea una respuesta (Bit 15 = 1) y sin error
                            if ((rxBuffer[rDnsOffset+2] & 0x80) != 0) {
                                int anCount = ((rxBuffer[rDnsOffset+6] & 0xFF) << 8) | (rxBuffer[rDnsOffset+7] & 0xFF);
                                if (anCount > 0) {
                                    // Saltar Header y Question para llegar al Answer
                                    int ptr = rDnsOffset + 12;
                                    while (rxBuffer[ptr] != 0) ptr++; // Saltar QNAME
                                    ptr += 5; // Saltar nulo + QTYPE(2) + QCLASS(2)
                                    
                                    // Leer primer Answer (ignorar Name, Type, Class, TTL)
                                    ptr += 10; 
                                    int dataLen = ((rxBuffer[ptr] & 0xFF) << 8) | (rxBuffer[ptr+1] & 0xFF);
                                    ptr += 2;
                                    
                                    if (dataLen == 4) { // IPv4
                                        byte[] resolvedIp = new byte[]{rxBuffer[ptr], rxBuffer[ptr+1], rxBuffer[ptr+2], rxBuffer[ptr+3]};
                                        return new String[] { 
                                            "Servidor: " + ipToString("", dns1), 
                                            "Nombre:   " + domain,
                                            ipToString("Address:  ", resolvedIp)
                                        };
                                    }
                                } else {
                                    return new String[] { "Servidor: " + ipToString("", dns1), "*** No se encontro direccion IPv4 para " + domain };
                                }
                            }
                        }
                    }
                }
            }
            try { Thread.sleep(10); } catch (Exception e) {}
            attempts++;
        }
        return new String[] { "nslookup: Tiempo de espera agotado para el servidor " + ipToString("", dns1) };
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
	
	// Establecer el DNS primario (1) y secundario (2) manualmente
    private static String[] handleSetDNS(String arg, int idx) {
		// Por defecto one.one.one.one y google
		String[] adns = new String[]{"DNS por defecto establecidos","1.1.1.1","8.8.8.8"}; 
        if (arg.length() < 7) return new String[] { "Uso: net dns# <dns#>" };
		if(idx == 1){
			dns1 = parseIp(arg);
			adns = new String[] { ipToString("DNS1: ",dns1) };
		}
		else if(idx == 2){
			dns2 = parseIp(arg);
			adns = new String[] { ipToString("DNS2: ",dns2) };
		}
        return adns;
    }
	
	// Implementación del comando PING adaptado para JVMOS-JIT
    private static String[] handleIcmpPing(String targetIpStr) {
		if (targetIpStr.length() < 7) return new String[] { "Uso: net ping <IP>" };
		byte[] destIp = parseIp(targetIpStr);

		// Trampa de Loopback 
		String cleanIp = targetIpStr.trim();
		String iplocal = ipToString("", localIp);
		int time = (int)java.lang.System.currentTimeMillis();

		if(cleanIp.equals(iplocal) || cleanIp.equals("localhost") || cleanIp.startsWith("127.0.0.")){
			return new String[] { "Respuesta de " + cleanIp + ": 64 bytes TTL=64 time=" + time + "ms" };
		}

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
        frame[16] = 0x00; frame[17] = 28; 
        
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

        int icmpCk = Checksum.calculate(frame, 34, 8);
        frame[36] = (byte)(icmpCk >> 8); frame[37] = (byte)icmpCk;

        DatagramPacket txPacket = new DatagramPacket(frame, frame.length);
        rawSocket.send(txPacket);

        byte[] rxBuffer = new byte[1536];
        DatagramPacket rxPacket = new DatagramPacket(rxBuffer, 1536);

        // Bucle While para desacoplar el timeout del vaciado de red        
        int attempts = 0;
        while (attempts < 150) { 
            rxPacket.setLength(1536); 
            int len = rawSocket.receive(rxPacket);
            
            if (len > 0) {
                if (len >= 42) {
                    int etherType = ((rxBuffer[12] & 0xFF) << 8) | (rxBuffer[13] & 0xFF);

                    // Interceptar ARP Requests entrantes mientras esperamos
                    if (etherType == 0x0806 && (rxBuffer[20] & 0xFF) == 0x00 && (rxBuffer[21] & 0xFF) == 0x01) {
                        boolean isOurIp = true;
                        for (int m = 0; m < 4; m++) {
                            if ((rxBuffer[38 + m] & 0xFF) != (localIp[m] & 0xFF)) { isOurIp = false; break; }
                        }
                        
                        if (isOurIp) {
                            // Responder ARP Reply al vuelo
                            for (int m = 0; m < 6; m++) {
                                rxBuffer[m] = rxBuffer[6 + m]; // MAC destino = MAC origen del router
                                rxBuffer[6 + m] = mac[m];      // MAC origen = Nuestra MAC
                            }
                            rxBuffer[21] = 0x02; // Opcode: Reply
                            
                            byte[] senderMac = new byte[6];
                            byte[] senderIp = new byte[4];
                            System.arraycopy(rxBuffer, 22, senderMac, 0, 6);
                            System.arraycopy(rxBuffer, 28, senderIp, 0, 4);
                            
                            System.arraycopy(mac, 0, rxBuffer, 22, 6);
                            System.arraycopy(localIp, 0, rxBuffer, 28, 4);
                            System.arraycopy(senderMac, 0, rxBuffer, 32, 6);
                            System.arraycopy(senderIp, 0, rxBuffer, 38, 4);
                            
                            rawSocket.send(new DatagramPacket(rxBuffer, 60));
                            continue; // Seguir esperando el PING
                        }
                    }
                    
                    // Procesar respuestas ICMP
                    else if (etherType == 0x0800) {
                        int ipHdrLen = (rxBuffer[14] & 0x0F) * 4;
                        int icmpOffset = 14 + ipHdrLen;
                        
                        // Si es ICMP Echo Reply
                        if ((rxBuffer[23] & 0xFF) == 1 && (rxBuffer[icmpOffset] & 0xFF) == 0) {
                            if ((rxBuffer[26] & 0xFF) == (destIp[0] & 0xFF) && 
                                (rxBuffer[27] & 0xFF) == (destIp[1] & 0xFF) && 
                                (rxBuffer[28] & 0xFF) == (destIp[2] & 0xFF) && 
                                (rxBuffer[29] & 0xFF) == (destIp[3] & 0xFF)) {
                                
                                int timeMS = (int)java.lang.System.currentTimeMillis() - time;
                                return new String[] { "Respuesta de " + targetIpStr + ": bytes=" + len + " TTL=" + (rxBuffer[22] & 0xFF) + " time=" + timeMS+" ms"};
                            }
                        }
                    }
                }
                // Si es basura, vaciar cola
                continue;
            }
            try { Thread.sleep(10); } catch (Exception e) {}
            attempts++;
        }
        return new String[] { "Ping a " + targetIpStr + ": Tiempo de espera agotado." };
    }
    
    // Hacer PING vía ARP
    private static String[] handleArpPing(String targetIp) {
        if (targetIp.length() < 1) {
            return new String[] { "Uso: net arp-ping <IP>" };
        }
        
        byte[] destIp = parseIp(targetIp);
        byte[] srcMac = getMacAddress();
        
        // Trama completa de 60 bytes
        byte[] arpFrame = new byte[60]; 
                
        // CABECERA ETHERNET (14 bytes)      
        for(int i = 0; i < 6; i++) arpFrame[i] = (byte) 0xFF;   // MAC broadcast        
        for(int i = 0; i < 6; i++) arpFrame[6 + i] = srcMac[i]; // MAC origen        
        arpFrame[12] = 0x08; arpFrame[13] = 0x06;               // EtherType: ARP
        
        // MENSAJE ARP (28 bytes)     
        arpFrame[14] = 0x00; arpFrame[15] = 0x01;               // Hardware: Ethernet      
        arpFrame[16] = 0x08; arpFrame[17] = 0x00;               // Protocolo: IPv4       
        arpFrame[18] = 0x06; arpFrame[19] = 0x04;               // Longitud (MAC e IP)        
        arpFrame[20] = 0x00; arpFrame[21] = 0x01;               // Operación: (Request)
                
        for(int i = 0; i < 6; i++) arpFrame[22 + i] = srcMac[i]; // MAC local
        for(int i = 0; i < 4; i++) arpFrame[28 + i] = localIp[i];// IP local
        for(int i = 0; i < 6; i++) arpFrame[32 + i] = 0x00;      // MAC destino (0x00)        
        for(int i = 0; i < 4; i++) arpFrame[38 + i] = destIp[i]; // IP destino
        
        DatagramPacket packet = new DatagramPacket(arpFrame, arpFrame.length);                
        rawSocket.send(packet); 
                
        byte[] rxBuffer = new byte[1536];
        DatagramPacket rxPacket = new DatagramPacket(rxBuffer, 1536); 
        
        // Bucle While
        int attempts = 0;
        while (attempts < 100) {
            rxPacket.setLength(1536); 
            int bytesRead = rawSocket.receive(rxPacket); 
            
            if (bytesRead > 0) {
                // Validar si es ARP Reply
                if (bytesRead >= 42 && (rxBuffer[12] & 0xFF) == 0x08 && (rxBuffer[13] & 0xFF) == 0x06 && 
                   (rxBuffer[20] & 0xFF) == 0x00 && (rxBuffer[21] & 0xFF) == 0x02) {
                    
                    boolean match = true;
                    for (int i = 0; i < 4; i++) {
                        // Comparación segura de bytes con signo (& 0xFF)
                        if ((rxBuffer[28 + i] & 0xFF) != (destIp[i] & 0xFF)) {
                            match = false;
                            break;
                        }
                    }
                    if (match) {
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
                // Si es basura, vaciar cola
                continue;
            }
            try { Thread.sleep(10); } catch (Exception e) {}
            attempts++;
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
	
	// Codifica "google.com" a formato DNS -> [6]google[3]com[0]
    private static byte[] encodeDomainName(String domain) {
        byte[] qname = new byte[domain.length() + 2];
        int labelLenIdx = 0;
        int qnameIdx = 1;
        int len = 0;
        
        for (int i = 0; i < domain.length(); i++) {
            char c = domain.charAt(i);
            if (c == '.') {
                qname[labelLenIdx] = (byte) len;
                labelLenIdx = qnameIdx;
                qnameIdx++;
                len = 0;
            } else {
                qname[qnameIdx++] = (byte) c;
                len++;
            }
        }
        qname[labelLenIdx] = (byte) len;
        qname[qnameIdx] = 0; // Byte nulo final (Root)
        return qname;
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
