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

//Basado en librería de OpenGL
public class Renderer3D {
    private Graphics2D graphics;
    private int screenWidth;
    private int screenHeight;
    private int cameraZ;
    private int projectionScale;

	  // Constructor 
    public Renderer3D(Graphics2D graphics,int screenWidth,int screenHeight) {
        this.graphics = graphics;
        this.screenWidth = screenWidth;
        this.screenHeight = screenHeight;

        cameraZ = 150;
        projectionScale = 400;
    }

    public void setCameraZ(int z) {
        cameraZ = z;
    }

    public void setProjectionScale(int scale) {
        projectionScale = scale;
    }

	// Adaptado de implementación en OpenGL	a JVMOS-JIT g3d
    public void render(Mesh mesh,Matrix3D transform) {
        if (mesh == null) {
            return;
        }
        if (mesh.vertices == null) {
            return;
        }
        if (mesh.triangles == null) {
            return;
        }
        int vertexCount = mesh.vertices.length;
        
		// Vértices transformados
        Vertex3D[] transformed = new Vertex3D[vertexCount];
        
		// Coordenadas proyectadas
        int[] screenX = new int[vertexCount];
        int[] screenY = new int[vertexCount];

        // Transformación
        for (int i = 0; i < vertexCount; i++) {
            transformed[i] = transform.transform(mesh.vertices[i]);
        }
        
		// Proyección
        for (int i = 0; i < vertexCount; i++) {
            Vertex3D v = transformed[i];
            int z = v.z + cameraZ;
            // Evitar división por cero y valores detrás de cámara
            if (z <= 0) {
                screenX[i] = 0;
                screenY[i] = 0;
                continue;
            }
            screenX[i] = (v.x * projectionScale) / z + (screenWidth / 2);
            screenY[i] = (v.y * projectionScale) / z + (screenHeight / 2);
		}

        // Algoritmo para pintar en pantalla (versión simple con triangulos por ahora)
		// Si alguien sabe hacerlo más profesional, acepto sugerencias
        int triangleCount = mesh.triangles.length;
        int[] order = new int[triangleCount];
        int[] depth = new int[triangleCount];

        for (int i = 0; i < triangleCount; i++) {
            Triangle3D t = mesh.triangles[i];
            order[i] = i;
            Vertex3D a = transformed[t.v0];
            Vertex3D b = transformed[t.v1];
            Vertex3D c = transformed[t.v2];
            depth[i] = a.z + b.z + c.z;
        }

        // Ordenamiento simple. (El triángulo con mayor Z se dibuja primero)
        for (int i = 0; i < triangleCount - 1; i++) {
            for (int j = i + 1;j < triangleCount;j++) {
                if (depth[order[i]] < depth[order[j]]) {
                    int temp = order[i];
                    order[i] = order[j];
                    order[j] = temp;
                }
            }
        }

        // Rasterización
        for (int n = 0;n < triangleCount;n++) {
            Triangle3D t = mesh.triangles[order[n]];
            Vertex3D a = transformed[t.v0];
            Vertex3D b = transformed[t.v1];
            Vertex3D c = transformed[t.v2];
            
			int z0 = a.z + cameraZ;
            int z1 = b.z + cameraZ;
            int z2 = c.z + cameraZ;

            // Si algún vértice está detrás de la cámara, no dibujamos el triángulo.
            if (z0 <= 0 || z1 <= 0 || z2 <= 0) {
                continue;
            }
            // Productos cruzados: (B-A) x (C-A)
            int ax = screenX[t.v0];
            int ay = screenY[t.v0];
            int bx = screenX[t.v1];
            int by = screenY[t.v1];
            int cx = screenX[t.v2];
            int cy = screenY[t.v2];
            int cross = (bx - ax) * (cy - ay) - (by - ay) * (cx - ax);

            // Triángulo degenerado
            if (cross == 0) {
                continue;
            }
            // Sólo se dibuja una orientación
            if (cross < 0) {
                continue;
            }
            // Color del triángulo
            graphics.setColor(
                new Color(t.color)
            );
            // Rasterizador 2D
            graphics.fillTriangle(ax, ay, bx, by, cx, cy);
        }
    }
}
