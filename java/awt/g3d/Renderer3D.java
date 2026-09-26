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
import kernel.Native;

//Basado en librería de OpenGL
public class Renderer3D {
	
    private Graphics2D graphics;
    private int screenWidth;
    private int screenHeight;
    private int cameraZ;
    private int projectionScale;
	private static final int MAX_TRIANGLES = 1024;
	
	// Pre-asignación de arreglos
    private int[] order = new int[MAX_TRIANGLES];
    private int[] depth = new int[MAX_TRIANGLES];
    private Vertex3D[] projA = new Vertex3D[MAX_TRIANGLES];
    private Vertex3D[] projB = new Vertex3D[MAX_TRIANGLES];
    private Vertex3D[] projC = new Vertex3D[MAX_TRIANGLES];
	
	private final int[] staticX = new int[3];
    private final int[] staticY = new int[3];

    // Búfer estático de 4 vértices para borrado en rectángulo
    private final int[] staticRectX = new int[4];
    private final int[] staticRectY = new int[4];
	private Vertex3D projA_L;
	private Vertex3D projB_L;
	
	// Constructor 
    public Renderer3D(Graphics2D graphics,int screenWidth,int screenHeight) {
        this.graphics = graphics;
        this.screenWidth = screenWidth;
        this.screenHeight = screenHeight;
        cameraZ = 150;
        projectionScale = 400;
		
		projA_L = new Vertex3D();
        projB_L = new Vertex3D();
		
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
	
	public void renderTriangle(int x0, int y0, int x1, int y1, int x2, int y2, int color) {
        // Sobreescribir las mismas posiciones de memoria
        staticX[0] = x0; staticX[1] = x1; staticX[2] = x2;
        staticY[0] = y0; staticY[1] = y1; staticY[2] = y2;		
        graphics.setColor(color);
        // Transmite la dirección de memoria fija al HAL en ensamblador
        graphics.fillPolygon(staticX, staticY, 3);
    }
	
	public void render(Mesh mesh, Matrix3D mt, int screenX, int screenY, int scalePercent) {
        if (mesh == null) return;

        // Borrado con raster
        int oldX = mesh.getOldX();
        int oldY = mesh.getOldY();
        int oldW = mesh.getOldW();
        int oldH = mesh.getOldH();

        if (oldW > 0 && oldH > 0 && oldX >= 0 && oldY >= 0) {
            // Formar los 4 vértices del rectángulo delimitador anterior
            staticRectX[0] = oldX;        staticRectY[0] = oldY;
            staticRectX[1] = oldX + oldW; staticRectY[1] = oldY;
            staticRectX[2] = oldX + oldW; staticRectY[2] = oldY + oldH;
            staticRectX[3] = oldX;        staticRectY[3] = oldY + oldH;

            // Establece color NEGRO absoluto (0x00000000)
            graphics.setColor(Color.BLACK);
            
            // Reutiliza SYS_FILL_POLYGON con 4 puntos para limpiar en VRAM
            graphics.fillPolygon(staticRectX, staticRectY, 4);
        }

        int minX = 1024, minY = 768, maxX = 0, maxY = 0;
        boolean paintedAny = false;

        int currentProjScale = (projectionScale * scalePercent) / 100;
        int triangleCount = mesh.getTriangleCount();

        if (triangleCount > 0) {
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
                        int temp = order[i]; order[i] = order[j]; order[j] = temp;
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
                
                if (z0 <= 15 || z1 <= 15 || z2 <= 15) continue;

                int x0 = (a.getX() * currentProjScale) / z0 + screenX;
                int y0 = (a.getY() * currentProjScale) / z0 + screenY;
                int x1 = (b.getX() * currentProjScale) / z1 + screenX;
                int y1 = (b.getY() * currentProjScale) / z1 + screenY;
                int x2 = (c.getX() * currentProjScale) / z2 + screenX;
                int y2 = (c.getY() * currentProjScale) / z2 + screenY;            

                int cross = (x1 - x0) * (y2 - y0) - (y1 - y0) * (x2 - x0);
                if (cross <= 0) continue;

                if (x0 < minX) minX = x0; if (x0 > maxX) maxX = x0;
                if (x1 < minX) minX = x1; if (x1 > maxX) maxX = x1;
                if (x2 < minX) minX = x2; if (x2 > maxX) maxX = x2;
                if (y0 < minY) minY = y0; if (y0 > maxY) maxY = y0;
                if (y1 < minY) minY = y1; if (y1 > maxY) maxY = y1;
                if (y2 < minY) minY = y2; if (y2 > maxY) maxY = y2;
                paintedAny = true;

                renderTriangle(x0, y0, x1, y1, x2, y2, t.getColor());
            }
        }

        // GUARDAR LÍMITES EN MESH (+10px padding)
        if (paintedAny) {
            int pad = 10;
            int newX = minX - pad; if (newX < 0) newX = 0;
            int newY = minY - pad; if (newY < 0) newY = 0;
            int newMaxX = maxX + pad; if (newMaxX > 1024) newMaxX = 1024;
            int newMaxY = maxY + pad; if (newMaxY > 768) newMaxY = 768;

            mesh.update(newX, newY, newMaxX - newX, newMaxY - newY);
        } else {
            mesh.update(-1, -1, 0, 0);
        }
    }    
}
