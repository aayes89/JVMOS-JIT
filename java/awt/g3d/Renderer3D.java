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
	private static final int MAX_TRIANGLES = 256;
	
	// Pre-asignación de arreglos (memoria máxima 256 triángulos)
    private int[] order = new int[MAX_TRIANGLES];
    private int[] depth = new int[MAX_TRIANGLES];
    private Vertex3D[] projA = new Vertex3D[MAX_TRIANGLES];
    private Vertex3D[] projB = new Vertex3D[MAX_TRIANGLES];
    private Vertex3D[] projC = new Vertex3D[MAX_TRIANGLES];

	// Constructor 
    public Renderer3D(Graphics2D graphics,int screenWidth,int screenHeight) {
        this.graphics = graphics;
        this.screenWidth = screenWidth;
        this.screenHeight = screenHeight;
        cameraZ = 150;
        projectionScale = 400;
		
		for(int i=0;i<MAX_TRIANGLES;i++){
			projA[i] = new Vertex3D();
			projB[i] = new Vertex3D();
			projC[i] = new Vertex3D();
		}
    }

    public void setCameraZ(int z) {
        cameraZ = z;
    }

    public void setProjectionScale(int scale) {
        projectionScale = scale;
    }

	// versión mejorada de render para establecer posición en X, Y y tamaño en porcierto (%)
	public void render(Mesh mesh, Matrix3D mt, int screenX, int screenY, int scalePercent) {
        if (mesh == null || mesh.getTriangleCount() == 0) return;
        
        int triangleCount = mesh.getTriangleCount();
        int currentProjScale = (projectionScale * scalePercent) / 100;

        for (int i = 0; i < triangleCount; i++) {
            Triangle3D t = mesh.getTriangle(i);
            order[i] = i;
            
            mt.transform(t.getV0(), projA[i]);
            mt.transform(t.getV1(), projB[i]);
            mt.transform(t.getV2(), projC[i]);
            
            depth[i] = projA[i].getZ() + projB[i].getZ() + projC[i].getZ();
        }

        for (int i = 0; i < triangleCount - 1; i++) {
            for (int j = i + 1; j < triangleCount; j++) {
                if (depth[order[i]] < depth[order[j]]) {
                    int temp = order[i];
                    order[i] = order[j];
                    order[j] = temp;
                }
            }
        }

        for (int n = 0; n < triangleCount; n++) {
            int idx = order[n];
            Triangle3D t = mesh.getTriangle(idx);
            
            Vertex3D a = projA[idx];
            Vertex3D b = projB[idx];
            Vertex3D c = projC[idx]; 
            
            int z0 = a.getZ() + cameraZ;
            int z1 = b.getZ() + cameraZ;            
            int z2 = c.getZ() + cameraZ;
            
            if (z0 <= 0 || z1 <= 0 || z2 <= 0) continue;

            // Proyectamos directamente a la coordenada X, Y deseada
            int x0 = (a.getX() * currentProjScale) / z0 + screenX;
            int y0 = (a.getY() * currentProjScale) / z0 + screenY;
            int x1 = (b.getX() * currentProjScale) / z1 + screenX;
            int y1 = (b.getY() * currentProjScale) / z1 + screenY;
            int x2 = (c.getX() * currentProjScale) / z2 + screenX;
            int y2 = (c.getY() * currentProjScale) / z2 + screenY;            

            int cross = (x1 - x0) * (y2 - y0) - (y1 - y0) * (x2 - x0);
            
            if (cross <= 0) continue;
            
            graphics.setColor(t.getColor());
            graphics.fillTriangle(x0, y0, x1, y1, x2, y2);
        }
    }
	
	public void render_old(Mesh mesh, Matrix3D mt) {
		if (mesh == null ||mesh.triangles == null) {
			return;
		}
		
		int triangleCount = mesh.getTriangleCount();

		// Transformar y calcular profundidad
		for (int i = 0; i < triangleCount; i++) {
			Triangle3D t = mesh.getTriangle(i);
			order[i] = i;
			
			mt.transform(t.getV0(), projA[i]);
			mt.transform(t.getV1(), projB[i]);
			mt.transform(t.getV2(), projC[i]);
			
			depth[i] = projA[i].getZ() + projB[i].getZ() + projC[i].getZ();
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
			int idx = order[n];
			Triangle3D t = mesh.getTriangle(idx);
			
			// Transformar los tres vértices
			Vertex3D a = projA[idx];
			Vertex3D b = projB[idx];
			Vertex3D c = projC[idx]; 
			
			// Trasladar al espacio de cámara
			int z0 = a.getZ() + cameraZ;
			int z1 = b.getZ() + cameraZ;			
			int z2 = c.getZ() + cameraZ;
			
			// Si un vértice está detrás de la cámara, descartamos el triángulo
			if (z0 <= 0 || z1 <= 0 || z2 <= 0) {
				continue;
			}

			// Proyección perspectiva y centro de pantalla
			int x0 = (a.getX() * projectionScale) / z0 + screenWidth / 2;
			int y0 = (a.getY() * projectionScale) / z0 + screenHeight / 2;
			int x1 = (b.getX() * projectionScale) / z1 + screenWidth / 2;
			int y1 = (b.getY() * projectionScale) / z1 + screenHeight / 2;
			int x2 = (c.getX() * projectionScale) / z2 + screenWidth / 2;
			int y2 = (c.getY() * projectionScale) / z2 + screenHeight / 2;			

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
			graphics.setColor(t.getColor());
			
			// Entregar rasterización a Graphics2D
			graphics.fillTriangle(x0, y0, x1, y1, x2, y2);
		}
	}
}
