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
    public void render(Mesh mesh, Matrix3D transform) {
		if (mesh == null) {
			return;
		}
		if (mesh.triangles == null) {
			return;
		}
		int triangleCount = mesh.triangles.length;
		
		// Profundidad de cada triángulo
		int[] order = new int[triangleCount];
		int[] depth = new int[triangleCount];

		// Transformar y calcular profundidad
		for (int i = 0; i < triangleCount; i++) {
			Triangle3D t = mesh.triangles[i];
			order[i] = i;
			Vertex3D a = transform.transform(t.v0);
			Vertex3D b = transform.transform(t.v1);
			Vertex3D c = transform.transform(t.v2);
			depth[i] = a.z + b.z + c.z;
		}

		// Se dibuja primero el triángulo que está más lejos
		for (int i = 0;i < triangleCount - 1; i++) {
			for (int j = i + 1; j < triangleCount; j++) {
				if (depth[order[i]]	< depth[order[j]]) {
					int temp = order[i];
					order[i] = order[j];
					order[j] = temp;
				}
			}
		}

		// Rasterización
		for (int n = 0;n < triangleCount;n++) {
			Triangle3D t = mesh.triangles[order[n]];
			
			// Transformar los tres vértices
			Vertex3D a = transform.transform(t.v0);
			Vertex3D b = transform.transform(t.v1);
			Vertex3D c = transform.transform(t.v2);
			
			// Trasladar al espacio de cámara
			int z0 = a.z + cameraZ;
			int z1 = b.z + cameraZ;			
			int z2 = c.z + cameraZ;
			
			// Si un vértice está detrás de la cámara, descartamos el triángulo
			if (z0 <= 0 || z1 <= 0 || z2 <= 0) {
				continue;
			}

			// Proyección perspectiva
			int x0 = (a.x * projectionScale) / z0;
			int y0 = (a.y * projectionScale) / z0;
			int x1 = (b.x * projectionScale) / z1;
			int y1 = (b.y * projectionScale) / z1;
			int x2 = (c.x * projectionScale) / z2;
			int y2 = (c.y * projectionScale) / z2;
			
			// Centro de pantalla
			x0 += screenWidth / 2;
			y0 += screenHeight / 2;
			x1 += screenWidth / 2;
			y1 += screenHeight / 2;
			x2 += screenWidth / 2;
			y2 += screenHeight / 2;

			// Producto cruzado 2D: (B-A) x (C-A)
			int cross = (x1 - x0) * (y2 - y0) - (y1 - y0) * (x2 - x0);
			
			// Triángulo degenerado
			if (cross == 0) {
				continue;
			}
			// Descartar backface
			if (cross < 0) {
				continue;
			}
			
			// Activar color del triángulo
			graphics.setColor(new Color(t.color));
			
			// Entregar rasterización a Graphics2D
			graphics.fillTriangle(x0, y0,x1, y1,x2, y2);
		}
	}
}
