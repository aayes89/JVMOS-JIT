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

// Caché IP -> MAC, con resolución dinámica para enviar paquetes unicast.
public class ArpTable {
    private static final int MAX_ENTRIES = 16;
    private static byte[] ips = new byte[MAX_ENTRIES * 4];
    private static byte[] macs = new byte[MAX_ENTRIES * 6];
    private static int count = 0;

    public static synchronized void put(byte[] ip, byte[] mac) {
        for (int i = 0; i < count; i++) {
            int off = i * 4;
            if (ips[off] == ip[0] && ips[off+1] == ip[1] && ips[off+2] == ip[2] && ips[off+3] == ip[3]) {
                System.arraycopy(mac, 0, macs, i * 6, 6);
                return;
            }
        }
        if (count < MAX_ENTRIES) {
            System.arraycopy(ip, 0, ips, count * 4, 4);
            System.arraycopy(mac, 0, macs, count * 6, 6);
            count++;
        }
    }

    public static synchronized byte[] get(byte[] ip) {
        for (int i = 0; i < count; i++) {
            int off = i * 4;
            if (ips[off] == ip[0] && ips[off+1] == ip[1] && ips[off+2] == ip[2] && ips[off+3] == ip[3]) {
                byte[] mac = new byte[6];
                System.arraycopy(macs, i * 6, mac, 0, 6);
                return mac;
            }
        }
        return null;
    }
}
