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

package java.awt.g3d;

import java.awt.Graphics2D;
import java.awt.Color;
import java.lang.Math;
import kernel.Native;

public class Demo3D {
    // liberado código de Boot.java para limpiar código y permitir ejecución independiente.
    // antigua función runShapes3D ahora run(Graphics2D)

    public static void run(Graphics2D g) {
        Renderer3D renderer = new Renderer3D(g, 1024, 768);
        
        Mesh cube = MeshFactory.createCube();
        Mesh pyramid = MeshFactory.createPyramid();
        Mesh sphere = MeshFactory.createSphere();
        
        Matrix3D matPyramid = new Matrix3D();
        Matrix3D matCube = new Matrix3D();
        Matrix3D matSphere = new Matrix3D();
        
        Matrix3D rotX = new Matrix3D();
        Matrix3D rotY = new Matrix3D();
        Matrix3D rotZ = new Matrix3D();
        
        int angle = 0;
        
        while (true) {
            g.setColor(Color.BLACK);
            g.fillRect(0, 0, 1024, 768);

            int sinA = Math.sin(angle);
            int cosA = Math.cos(angle);
            int sinB = Math.sin(angle / 2);
            int cosB = Math.cos(angle / 2);

            rotX.setRotationX(sinA, cosA);
            rotY.setRotationY(sinA, cosA);
            rotZ.setRotationZ(sinB, cosB);

            // --- 1. PIRÁMIDE (Izquierda) ---
            matPyramid.setRotationY(sinA, cosA); 
            matPyramid.tx = -160;   // Alineado a la izquierda
            matPyramid.tz = 250;    // Distancia ideal para escala 25
            renderer.render(pyramid, matPyramid);

            // --- 2. CUBO (Centro) ---
            matCube.setIdentity();
            matCube.multiply(rotX); 
            matCube.multiply(rotY);
            matCube.tx = 0;         // Centro exacto
            matCube.tz = 250;
            renderer.render(cube, matCube);

            // --- 3. ESFERA (Derecha) ---
            matSphere.setIdentity();
            matSphere.multiply(rotX);
            matSphere.multiply(rotZ);
            matSphere.tx = 160;     // Alineado a la derecha
            matSphere.tz = 250;
            renderer.render(sphere, matSphere);

            g.setColor(Color.WHITE);
            g.drawString("Demo Bare-Metal 3D: Piramide, Cubo y Esfera. ESC para salir.", 20, 20);

            angle = (angle + 2) % 360;

            Native.sys(12, 16, 0, 0, 0); 
            if (Native.sys(6, 0, 0, 0, 0) == 27) break;
        }
    }
}
