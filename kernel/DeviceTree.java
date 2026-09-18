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

package kernel;

public class DeviceTree {
    
    // Base de datos de dispositivos PCI conocidos (Vendor ID, Device ID)
	// Creado a partir de pruebas en hardware real
    public static String getDeviceName(int vendorId, int deviceId) {
        
        if (vendorId == 0x1234) { // QEMU
            if (deviceId == 0x1111) return "QEMU Virtual Video Controller (VGA)";
        } 
        else if (vendorId == 0x10EC) { // Realtek
            if (deviceId == 0x8139) return "Realtek RTL8139 Fast Ethernet";
            if (deviceId == 0x8168) return "Realtek RTL8168/8111 Gigabit Ethernet";
            if (deviceId == 0x8136) return "Realtek RTL810xE PCI Express Fast Ethernet";
            if (deviceId == 0xC822) return "Realtek RTL8822CE 802.11ac Wi-Fi";
            if (deviceId == 0x5209) return "Realtek RTS5209 PCI Express Card Reader";
            return "Dispositivo Realtek Desconocido";
        } 
        else if (vendorId == 0x1022) { // AMD (Procesadores y Puentes)
            if (deviceId == 0x2000) return "AMD PCnet-FAST III (VirtualBox)";
            if (deviceId == 0x15D0) return "AMD Family 17h PCIe Host Bridge";
            if (deviceId == 0x15DF) return "AMD Family 17h PCIe Root Port";
            if (deviceId == 0x15E3) return "AMD Audio Processor";
            if (deviceId == 0x1510) return "AMD Family 16h Processor Root Complex";
            if (deviceId == 0x1700) return "AMD FCH PCIe Root Port";
            return "Dispositivo AMD Desconocido";
        } 
        else if (vendorId == 0x1002) { // AMD / ATI (Gráficos y Chipsets SBx00)
            if (deviceId == 0x15D8) return "AMD Radeon Vega Graphics";
            if (deviceId == 0x15DE) return "AMD Audio Coprocessor";
            if (deviceId == 0x9802) return "AMD Radeon HD 7310/8000 Series Graphics";
            if (deviceId == 0x4391) return "AMD SB7x0/SB9x0 SATA Controller [AHCI]";
            if (deviceId == 0x4397) return "AMD SB7x0/SB9x0 USB EHCI Controller";
            if (deviceId == 0x4385) return "AMD SBx00 SMBus Controller";
            if (deviceId == 0x43A0) return "AMD SBx00 PCI to PCI Bridge";
            return "Dispositivo AMD/ATI Desconocido";
        } 
        else if (vendorId == 0x8086) { // Intel
            if (deviceId == 0x100E) return "Intel PRO/1000 Gigabit Ethernet";
            if (deviceId == 0x7000) return "Intel PIIX3 ISA Bridge";
            if (deviceId == 0x1237) return "Intel 440FX (Natoma) Host Bridge";
            return "Dispositivo Intel Desconocido";
        } 
        else if (vendorId == 0x15AD) { // VMware
            if (deviceId == 0x1029) return "VMware SVGA II Adapter";
            return "Dispositivo VMware Desconocido";
        } 
        else if (vendorId == 0x106B) { // Apple
            return "Dispositivo Apple";
        }
        
        return "Dispositivo Desconocido";
    }
}
