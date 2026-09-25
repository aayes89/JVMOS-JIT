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
import java.awt.Graphics2D;
import java.awt.Color;
import java.awt.g3d.Renderer3D;
import java.awt.g3d.Triangle3D;
import java.awt.g3d.Mesh;
import java.awt.g3d.Vertex3D;
import java.awt.g3d.Matrix3D;
import java.awt.g3d.MeshFactory;
import java.lang.Thread;
import java.lang.Math;

public class RenderDemos {

    private static Graphics2D g;

    // Constructor
    public RenderDemos(Graphics2D g){
        this.g = g;
    }

    // Cubo en wireframe giratorio
    public static void runCubeWireframe() {
        g.clearScreen();
        g.setColor(Color.CYAN);
        g.drawString("Baremetal 3D Engine (JIT Integer Math) - Presiona ESC para salir", 20, 20);

        int[] cubeX = {-50, 50, 50, -50, -50, 50, 50, -50};
        int[] cubeY = {-50, -50, 50, 50, -50, -50, 50, 50};
        int[] cubeZ = {-50, -50, -50, -50, 50, 50, 50, 50};
        int[] edges = { 0,1, 1,2, 2,3, 3,0, 4,5, 5,6, 6,7, 7,4, 0,4, 1,5, 2,6, 3,7 };
        int[] projX = new int[8];
        int[] projY = new int[8];
        int[] oldProjX = new int[8];
        int[] oldProjY = new int[8];
        int angleX = 0, angleY = 0, angleZ = 0;

        while (true) {
            g.setColor(Color.BLACK);
            for (int i = 0; i < 12; i++) {
                int p1 = edges[i * 2];
                int p2 = edges[i * 2 + 1];
                if (oldProjX[p1] != 0){
                    g.drawLine(oldProjX[p1], oldProjY[p1], oldProjX[p2], oldProjY[p2]);
                }
            }

            int sinX = Math.sin(angleX), cosX = Math.cos(angleX);
            int sinY = Math.sin(angleY), cosY = Math.cos(angleY);
            int sinZ = Math.sin(angleZ), cosZ = Math.cos(angleZ);

            for (int i = 0; i < 8; i++) {
                int x = cubeX[i], y = cubeY[i], z = cubeZ[i];
                int xy = (y * cosX - z * sinX) / 256, xz = (y * sinX + z * cosX) / 256; y = xy; z = xz;
                int yx = (x * cosY + z * sinY) / 256, yz = (-x * sinY + z * cosY) / 256; x = yx; z = yz;
                int zx = (x * cosZ - y * sinZ) / 256, zy = (x * sinZ + y * cosZ) / 256; x = zx; y = zy;

                int z_shifted = z + 150;
                projX[i] = (x * 400) / z_shifted + 512; projY[i] = (y * 400) / z_shifted + 384;
            }

            g.setColor(Color.GREEN);
            for (int i = 0; i < 12; i++) {
                int p1 = edges[i * 2], p2 = edges[i * 2 + 1];
                g.drawLine(projX[p1], projY[p1], projX[p2], projY[p2]);
            }

            for (int i = 0; i < 8; i++) { oldProjX[i] = projX[i]; oldProjY[i] = projY[i]; }
			// Volcar bacbuffer a la pantalla
			g.swapBuffers();

            angleX = (angleX + 2) % 360; angleY = (angleY + 3) % 360; angleZ = (angleZ + 1) % 360;
            try{Thread.sleep(16);}catch(Exception e){}
            if (Native.sys(Native.SYS_READ_KEYBOARD, 0, 0, 0, 0) == 27) break;
        }
    }
    
    public static void runCube2D() {
        g.clearScreen();
        g.setColor(0x0000FFFF);
        g.drawString("Baremetal 3D Engine - Filled Cube - ESC para salir",20,20);

        int[] cubeX = {-50,  50,  50, -50, -50,  50,  50, -50};
        int[] cubeY = {-50, -50,  50,  50, -50, -50,  50,  50};
        int[] cubeZ = {-50, -50, -50, -50,  50,  50,  50,  50};

        int[] faces = { 0, 1, 2, 3, 4, 7, 6, 5, 0, 4, 5, 1, 3, 2, 6, 7, 0, 3, 7, 4, 1, 5, 6, 2};
        int[] faceColors = {
            0x00FF0000, // rojo
            0x00000080, // azul oscuro
            0x0000AA00, // verde
            0x0000FF00, // verde brillante
            0x000000FF, // azul
            0x00FFFF00  // amarillo
        };

        int[] projX = new int[8];
        int[] projY = new int[8];

        int[] rotZ = new int[8];

        int[] faceDepth = new int[6];
        int[] faceOrder = {0, 1, 2, 3, 4, 5};

        int angleX = 0;
        int angleY = 0;
        int angleZ = 0;

        while (true) {
            g.clearScreen();
			g.setColor(Color.CYAN);
			g.drawString("Baremetal 3D Engine - Filled Cube - ESC para salir" , 20, 20);
			
            // Seno y Coseno
            int sinX = Math.sin(angleX);
            int cosX = Math.cos(angleX);

            int sinY = Math.sin(angleY);
            int cosY = Math.cos(angleY);

            int sinZ = Math.sin(angleZ);
            int cosZ = Math.cos(angleZ);

            // Rotar y proyectar los 8 vértices
            for (int i = 0; i < 8; i++) {
                int x = cubeX[i];
                int y = cubeY[i];
                int z = cubeZ[i];

                // Rotación en X
                int newY = (y * cosX - z * sinX) >> 8;
                int newZ = (y * sinX + z * cosX) >> 8;
                y = newY;
                z = newZ;

                // Rotación en Y
                int newX = (x * cosY + z * sinY) >> 8;
                newZ = (-x * sinY + z * cosY) >> 8;
                x = newX;
                z = newZ;

                // Rotación en Z
                newX = (x * cosZ - y * sinZ) >> 8;
                newY = (x * sinZ + y * cosZ) >> 8;
                x = newX;
                y = newY;
                
                // Respaldamos la profundidad
                rotZ[i] = z;

                // Perspectiva
                int zShifted = z + 200;

                // Protección contra división entre cero y geometría detrás de la cámara.
                if (zShifted < 1) {
                    zShifted = 1;
                }

                projX[i] = (x * 400) / zShifted + 512;
                projY[i] = (y * 400) / zShifted + 384;
            }

            // Calcular profundidad promedio de cada cara
            for (int i = 0; i < 6; i++) {
                int base = i << 2;

                int v0 = faces[base];
                int v1 = faces[base + 1];
                int v2 = faces[base + 2];
                int v3 = faces[base + 3];

                faceDepth[i] = rotZ[v0] + rotZ[v1] + rotZ[v2] + rotZ[v3];
            }

            // Ordenar caras
            for (int i = 0; i < 6; i++) {
                for (int j = i + 1; j < 6; j++) {
                    if (faceDepth[faceOrder[i]] > faceDepth[faceOrder[j]]) {
                        int temp = faceOrder[i];
                        faceOrder[i] = faceOrder[j];
                        faceOrder[j] = temp;
                    }
                }
            }

            // Dibujar caras
            for (int f = 0; f < 6; f++) {
                int face = faceOrder[f];
                int base = face << 2;

                int v0 = faces[base];
                int v1 = faces[base + 1];
                int v2 = faces[base + 2];
                int v3 = faces[base + 3];


                // Producto cruzado en espacio de pantalla.
                int cross = (projX[v1] - projX[v0]) * (projY[v2] - projY[v0]) - (projY[v1] - projY[v0]) * (projX[v2] - projX[v0]);

                // Si la cara apunta hacia atrás, no se dibuja.
                if (cross >= 0) {
                    continue;
                }
                // Color de la cara actual
                g.setColor(faceColors[face]);


                // CUADRILATERO -> DOS TRIANGULOS
                g.fillTriangle(projX[v0],projY[v0],projX[v1],projY[v1],projX[v2],projY[v2]);
                g.fillTriangle(projX[v0],projY[v0],projX[v2],projY[v2],projX[v3],projY[v3]);
            }


            // Dibujar los border para dar aspecto de cubo          
            g.setColor(Color.BLACK);

            for (int i = 0; i < 6; i++) {
                int face = faceOrder[i];
                int base = face << 2;

                int v0 = faces[base];
                int v1 = faces[base + 1];
                int v2 = faces[base + 2];
                int v3 = faces[base + 3];

                g.drawLine(projX[v0],projY[v0],projX[v1],projY[v1]);
                g.drawLine(projX[v1],projY[v1],projX[v2],projY[v2]);
                g.drawLine(projX[v2],projY[v2],projX[v3],projY[v3]);
                g.drawLine(projX[v3],projY[v3],projX[v0],projY[v0]);
            }
			
			// Volcar backbuffer a la pantalla
			g.swapBuffers();

            // Siguiente frame
            angleX = (angleX + 2) % 360;
            angleY = (angleY + 3) % 360;
            angleZ = (angleZ + 1) % 360;
            try {
                Thread.sleep(16);
            } catch (Exception e) {
            }

            if (Native.sys(Native.SYS_READ_KEYBOARD,0,0,0,0) == 27) {
                break;
            }
        }
    }
    
    public static void runCubeMesh3D() {
        Renderer3D renderer = new Renderer3D(g, 1024, 768);
        
        Mesh cube = MeshFactory.createCube();
        
        // Pre-asignar matrices fuera del bucle para proteger la memoria
        Matrix3D matrix = new Matrix3D();
        Matrix3D rotY = new Matrix3D();
        Matrix3D rotZ = new Matrix3D();
        
        // Inicializar ángulos independientes
        int angleX = 0;
        int angleY = 0;
        int angleZ = 0;
        
        while (true) {
            // Limpiar pantalla
            g.clearScreen();
			g.setColor(Color.CYAN);
			g.drawString("Baremetal 3D Engine - Mesh Cube - ESC para salir",20,20);

            // Calcular trigonometría para los tres ejes
            int sinX = Math.sin(angleX); 
            int cosX = Math.cos(angleX); 
            int sinY = Math.sin(angleY); 
            int cosY = Math.cos(angleY); 
            int sinZ = Math.sin(angleZ); 
            int cosZ = Math.cos(angleZ); 

            // 1. Iniciar la matriz principal con la rotación X
            matrix.setRotationX(sinX, cosX);
            
            // 2. Preparar rotación Y y multiplicar
            rotY.setRotationY(sinY, cosY);
            matrix.multiply(rotY);
            
            // 3. Preparar rotación Z y multiplicar
            rotZ.setRotationZ(sinZ, cosZ);
            matrix.multiply(rotZ);

            // Renderizar la geometría final combinada
            renderer.render(cube, matrix, 200, 385, 100);
			
			// Volcar backbuffer a la pantalla
			g.swapBuffers();
            
            // Avanzar rotaciones a distintas velocidades para un efecto más natural
            angleX = (angleX + 1) % 360;
            angleY = (angleY + 2) % 360;
            angleZ = (angleZ + 3) % 360;

            // Mantener ~60 FPS
            Native.sys(12, 16, 0, 0, 0);

            // Salir con ESC
            if (Native.sys(6, 0, 0, 0, 0) == 27) {
                break;
            }
        }
    }
}
