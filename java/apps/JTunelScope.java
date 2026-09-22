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

package java.apps;

import kernel.Native;
import java.awt.Point;
import java.lang.Math;

public class JTunelScope {

    private static final double FOCAL = 600;
    private static final double SIZE = 200;
    private static final double MAX_Z = 5000;
    private static final double STEP_Z = 200;
    private static final double SPEED = 8;
    private static final int WIDTH = 1024;
    private static final int HEIGHT = 768;

    private static double lookX = 0;
    private static double lookY = 0;
    private static int score = 0;
    private static int level = 1;
    private static int health = 100;
    private static boolean running = true;

    private static double[] slices;
    private static Roca[] rocks;
    private static int numRocks = 0;

    // Object Pool de Puntos (Evita colapsar el Heap)
    private static final Point pA = new Point(0, 0);
    private static final Point pB = new Point(0, 0);
    private static final Point pC = new Point(0, 0);
    private static final Point pD = new Point(0, 0);

    static class Roca {
        double x, y, z;
        Roca(double x, double y, double z) {
            this.x = x; this.y = y; this.z = z;
        }
    }

    public static void execute() {
        initGame();
        while (running) {
            update();
            render();
            Native.sys(Native.SYS_SLEEP, 16, 0, 0, 0);
            int key = Native.sys(Native.SYS_READ_KEYBOARD, 0, 0, 0, 0);
            if (key == 1 || key == 27) running = false;
        }
        Native.sys(Native.SYS_SET_COLOR, 0x000000, 0, 0, 0);
        Native.sys(Native.SYS_FILL_RECT, 0, 0, WIDTH, HEIGHT);
    }

    private static void initGame() {
        score = 0; level = 1; health = 100; running = true;
        int numSlices = (int) ((MAX_Z - 300) / STEP_Z) + 1;
        slices = new double[numSlices];
        int idx = 0;
        for (double z = 300; z < MAX_Z && idx < numSlices; z += STEP_Z) {
            slices[idx++] = z;
        }

        rocks = new Roca[100];
        for (int i = 0; i < rocks.length; i++) {
            rocks[i] = new Roca(0, 0, MAX_Z); 
        }
        generarRocas(level * 5);
    }

    private static void generarRocas(int amount) {
        if (amount > rocks.length) amount = rocks.length;
        numRocks = amount;
        for (int i = 0; i < amount; i++) {
            Roca r = rocks[i];
            r.x = Math.random() * SIZE * 2 - SIZE;
            r.y = Math.random() * SIZE * 2 - SIZE;
            r.z = 500 + Math.random() * MAX_Z;
        }
    }

    private static void proyectar(double x, double y, double z, Point out) {
        double scale = FOCAL / z;
        // Mutación directa de las variables float x, y de java.awt.Point
        out.x = (float) ((WIDTH / 2.0) + (x - lookX) * scale);
        out.y = (float) ((HEIGHT / 2.0) + (y - lookY) * scale);
    }

    private static void update() {
        if (health <= 0) return;

        int mx = Native.sys(Native.SYS_READ_MOUSE, 0, 0, 0, 0);
        int my = Native.sys(Native.SYS_READ_MOUSE, 1, 0, 0, 0);

        if (mx < 0) mx = 0; if (mx > WIDTH) mx = WIDTH;
        if (my < 0) my = 0; if (my > HEIGHT) my = HEIGHT;

        double nx = (mx - (WIDTH / 2.0)) / (WIDTH / 2.0);
        double ny = (my - (HEIGHT / 2.0)) / (HEIGHT / 2.0);
        lookX = nx * 120;
        lookY = ny * 120;

        for (int i = 0; i < slices.length; i++) {
            slices[i] -= SPEED;
            if (slices[i] < 300) slices[i] = MAX_Z;
        }

        for (int i = 0; i < numRocks; i++) {
            Roca r = rocks[i];
            r.z -= SPEED * 2;

            if (r.z < 250) {
                proyectar(r.x, r.y, r.z, pA);
                int dx = (int) pA.x - (WIDTH / 2);
                int dy = (int) pA.y - (HEIGHT / 2);
                
                if ((dx * dx) + (dy * dy) < 2025) { // 45 * 45
                    health -= 20;
                    r.z = MAX_Z;
                    r.x = Math.random() * SIZE * 2 - SIZE;
                    r.y = Math.random() * SIZE * 2 - SIZE;
                    continue;
                }
            }

            if (r.z < 80) {
                score += 10;
                r.z = MAX_Z;
                r.x = Math.random() * SIZE * 2 - SIZE;
                r.y = Math.random() * SIZE * 2 - SIZE;
            }
        }

        if (score >= 100) {
            score = 0; level++;
            generarRocas(level * 5);
        }
    }

    private static void render() {
        Native.sys(Native.SYS_SET_COLOR, 0x000000, 0, 0, 0);
        Native.sys(Native.SYS_FILL_RECT, 0, 0, WIDTH, HEIGHT);

        if (health <= 0) {
            Native.sys(Native.SYS_SET_COLOR, 0xFF0000, 0, 0, 0);
            Native.sys(Native.SYS_DRAW_STRING, (WIDTH / 2) - 40, HEIGHT / 2, "GAME OVER", 0);
            return;
        }

        Native.sys(Native.SYS_SET_COLOR, 0xFFFFFF, 0, 0, 0);
        for (double z : slices) {
            proyectar(-SIZE, -SIZE, z, pA);
            proyectar(SIZE, -SIZE, z, pB);
            proyectar(SIZE, SIZE, z, pC);
            proyectar(-SIZE, SIZE, z, pD);

            Native.sys(Native.SYS_DRAW_LINE, (int)pA.x, (int)pA.y, (int)pB.x, (int)pB.y);
            Native.sys(Native.SYS_DRAW_LINE, (int)pB.x, (int)pB.y, (int)pC.x, (int)pC.y);
            Native.sys(Native.SYS_DRAW_LINE, (int)pC.x, (int)pC.y, (int)pD.x, (int)pD.y);
            Native.sys(Native.SYS_DRAW_LINE, (int)pD.x, (int)pD.y, (int)pA.x, (int)pA.y);
        }

        int divisions = 10;
        for (int i = 0; i <= divisions; i++) {
            double x = -SIZE + (2 * SIZE * i / divisions);
            proyectar(x, -SIZE, 300, pA);
            proyectar(x, -SIZE, MAX_Z, pB);
            Native.sys(Native.SYS_DRAW_LINE, (int)pA.x, (int)pA.y, (int)pB.x, (int)pB.y);
            
            proyectar(x, SIZE, 300, pC);
            proyectar(x, SIZE, MAX_Z, pD);
            Native.sys(Native.SYS_DRAW_LINE, (int)pC.x, (int)pC.y, (int)pD.x, (int)pD.y);
            
            double y = -SIZE + (2 * SIZE * i / divisions);
            proyectar(-SIZE, y, 300, pA);
            proyectar(-SIZE, y, MAX_Z, pB);
            Native.sys(Native.SYS_DRAW_LINE, (int)pA.x, (int)pA.y, (int)pB.x, (int)pB.y);
            
            proyectar(SIZE, y, 300, pC);
            proyectar(SIZE, y, MAX_Z, pD);
            Native.sys(Native.SYS_DRAW_LINE, (int)pC.x, (int)pC.y, (int)pD.x, (int)pD.y);
        }

        Native.sys(Native.SYS_SET_COLOR, 0xFFA500, 0, 0, 0);
        for (int i = 0; i < numRocks; i++) {
            Roca r = rocks[i];
            proyectar(r.x, r.y, r.z, pA);
            int radius = (int) (25 * (FOCAL / r.z));
            Native.sys(Native.SYS_FILL_RECT, (int)pA.x - radius, (int)pA.y - radius, radius * 2, radius * 2);
        }

        Native.sys(Native.SYS_SET_COLOR, 0x00FF00, 0, 0, 0);
        Native.sys(Native.SYS_DRAW_STRING, 20, 30, "Nivel : " + level, 0);
        Native.sys(Native.SYS_DRAW_STRING, 20, 55, "Puntos: " + score + "/100", 0);
        Native.sys(Native.SYS_DRAW_STRING, 20, 80, "Salud : " + health, 0);
    }
}
