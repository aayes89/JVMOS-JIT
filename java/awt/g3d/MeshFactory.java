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

import java.awt.Color;
import java.util.List;
import java.util.ArrayList;
import kernel.Native;

public class MeshFactory {    

    // Primitivas 3D
    public static Mesh createCube() {
        Vertex3D v0 = new Vertex3D(-25, -25, -25);
        Vertex3D v1 = new Vertex3D( 25, -25, -25);
        Vertex3D v2 = new Vertex3D( 25,  25, -25);
        Vertex3D v3 = new Vertex3D(-25,  25, -25);
        Vertex3D v4 = new Vertex3D(-25, -25,  25);
        Vertex3D v5 = new Vertex3D( 25, -25,  25);
        Vertex3D v6 = new Vertex3D( 25,  25,  25);
        Vertex3D v7 = new Vertex3D(-25,  25,  25);
                
        Triangle3D t0 = new Triangle3D(v0, v1, v2);        
        Triangle3D t1 = new Triangle3D(v0, v2, v3);        
        Triangle3D t2 = new Triangle3D(v1, v5, v6);        
        Triangle3D t3 = new Triangle3D(v1, v6, v2);
        Triangle3D t4 = new Triangle3D(v5, v4, v7);
        Triangle3D t5 = new Triangle3D(v5, v7, v6);
        Triangle3D t6 = new Triangle3D(v4, v0, v3);
        Triangle3D t7 = new Triangle3D(v4, v3, v7);
        Triangle3D t8 = new Triangle3D(v3, v2, v6);
        Triangle3D t9 = new Triangle3D(v3, v6, v7);
        Triangle3D t10 = new Triangle3D(v4, v5, v1);
        Triangle3D t11 = new Triangle3D(v4, v1, v0);
        
        t0.setColor(Color.RED); 
        t1.setColor(Color.RED);
        t2.setColor(Color.GREEN);
        t3.setColor(Color.GREEN);
        t4.setColor(Color.BLUE); 
        t5.setColor(Color.BLUE);
        t6.setColor(Color.YELLOW);
        t7.setColor(Color.YELLOW);
        t8.setColor(Color.CYAN); 
        t9.setColor(Color.CYAN);
        t10.setColor(Color.MAGENT);
        t11.setColor(Color.MAGENT);    
        
        Vertex3D[] vertices = {v0, v1, v2, v3, v4, v5, v6, v7};
        Triangle3D[] triangles = {t0, t1, t2, t3, t4, t5, t6, t7, t8, t9, t10, t11};		
        return new Mesh(vertices, triangles, new Line3D[0]);
    }

    public static Mesh createPyramid() {
        Vertex3D v0 = new Vertex3D(-25, -25, -25);
        Vertex3D v1 = new Vertex3D( 25, -25, -25);
        Vertex3D v2 = new Vertex3D( 25, -25,  25);
        Vertex3D v3 = new Vertex3D(-25, -25,  25);
        Vertex3D v4 = new Vertex3D(  0,  25,   0);

        Triangle3D t0 = new Triangle3D(v0, v2, v1);
        Triangle3D t1 = new Triangle3D(v0, v3, v2);
        Triangle3D t2 = new Triangle3D(v0, v1, v4);
        Triangle3D t3 = new Triangle3D(v1, v2, v4);
        Triangle3D t4 = new Triangle3D(v2, v3, v4);
        Triangle3D t5 = new Triangle3D(v3, v0, v4);

        t0.setColor(Color.GREEN); 
        t1.setColor(Color.GREEN);
        t2.setColor(Color.YELLOW);
        t3.setColor(Color.RED);
        t4.setColor(Color.CYAN);
        t5.setColor(Color.BLUE);

        Vertex3D[] vertices = {v0, v1, v2, v3, v4};
        Triangle3D[] triangles = {t0, t1, t2, t3, t4, t5};
        return new Mesh(vertices, triangles, new Line3D[0]);
    }
    
    public static Mesh createSphere() {
        int radius = 25;
        int rings = 8;
        int sectors = 8;
        
        int numVertices = (rings - 1) * sectors + 2;
        Vertex3D[] vertices = new Vertex3D[numVertices];
        
        vertices[0] = new Vertex3D(0, radius, 0);
        int vIdx = 1;
        
        for (int r = 1; r < rings; r++) {
            int theta = (r * 180) / rings;
            int sinTheta = Math.sin(theta);
            int cosTheta = Math.cos(theta);
            
            for (int s = 0; s < sectors; s++) {
                int phi = (s * 360) / sectors;
                int sinPhi = Math.sin(phi);
                int cosPhi = Math.cos(phi);

                int rs = (radius * sinTheta) >> 8;
                int x = (rs * cosPhi) >> 8;
                int y = (radius * cosTheta) >> 8;
                int z = (rs * sinPhi) >> 8;
                
                vertices[vIdx++] = new Vertex3D(x, y, z);
            }
        }
        
        int bottomIdx = vIdx;
        vertices[bottomIdx] = new Vertex3D(0, -radius, 0);

        int numTriangles = sectors * 2 + (rings - 2) * sectors * 2;
        Triangle3D[] triangles = new Triangle3D[numTriangles];
        int tIdx = 0;

        for (int s = 0; s < sectors; s++) {
            int sNext = (s + 1) % sectors;
            triangles[tIdx] = new Triangle3D(vertices[0], vertices[1 + s], vertices[1 + sNext]);
            triangles[tIdx].setColor(Color.MAGENT);
            tIdx++;
        }

        for (int r = 0; r < rings - 2; r++) {
            int rowStart = 1 + r * sectors;
            int nextRowStart = 1 + (r + 1) * sectors;
            for (int s = 0; s < sectors; s++) {
                int sNext = (s + 1) % sectors;
                Vertex3D v00 = vertices[rowStart + s];
                Vertex3D v01 = vertices[rowStart + sNext];
                Vertex3D v10 = vertices[nextRowStart + s];
                Vertex3D v11 = vertices[nextRowStart + sNext];

                triangles[tIdx] = new Triangle3D(v00, v10, v01);
                triangles[tIdx].setColor((s % 2 == 0) ? Color.BLUE : Color.CYAN);
                tIdx++;
                triangles[tIdx] = new Triangle3D(v10, v11, v01);
                triangles[tIdx].setColor((s % 2 == 0) ? Color.CYAN : Color.BLUE);
                tIdx++;
            }
        }

        int lastRowStart = 1 + (rings - 2) * sectors;
        for (int s = 0; s < sectors; s++) {
            int sNext = (s + 1) % sectors;
            triangles[tIdx] = new Triangle3D(vertices[bottomIdx], vertices[lastRowStart + sNext], vertices[lastRowStart + s]);
            triangles[tIdx].setColor(Color.MAGENT);
            tIdx++;
        }

        return new Mesh(vertices, triangles, new Line3D[0]);
    }
	
	// Obtener Mesh de un arreglo de Strings
    public static Mesh createFromOBJ(String[] objContent, float scale, int defaultColor) {
        if (objContent == null || objContent.length == 0) {            
            return new Mesh(new Vertex3D[0], new Triangle3D[0], new Line3D[0]); 
        }

        ArrayList<Vertex3D> vertexList = new ArrayList<>();
        ArrayList<Triangle3D> triangleList = new ArrayList<>();
        ArrayList<Line3D> lineList = new ArrayList<>();		
        
        for (int i = 0; i < objContent.length; i++) {
            String line = objContent[i].trim(); 

            if (line.isEmpty() || line.startsWith("#") || line.startsWith("vt ") || line.startsWith("vn ")) {
                continue;
            }		

            // Procesar vértices						
            if (line.startsWith("v ")) {
                String[] p = line.split(" ");				
                String[] tokens = getValidTokens(p);				
				
                if (tokens.length >= 4) {
					float fx = Float.parseFloat(tokens[1]);					
					float fy = Float.parseFloat(tokens[2]);										
					float fz = Float.parseFloat(tokens[3]);										
                    
                    int ix = (int) (fx * scale);
                    int iy = (int) (fy * scale);
                    int iz = (int) (fz * scale);
                    
                    vertexList.add(new Vertex3D(ix, iy, iz));
					paso = true;
                }				
            }
            // Procesar caras y triangularlass
            else if (line.startsWith("f ")) {
                String[] p = line.split(" ");
				Native.sys(5, 120, 50, "Paso split de f", 0);
                String[] tokens = getValidTokens(p);
				Native.sys(5, 400, 50, "Paso valid tokens f", 0);
                
                int[] vIndices = new int[tokens.length - 1];
                for (int j = 1; j < tokens.length; j++) {
                    String[] f = tokens[j].split("/");
                    vIndices[j - 1] = Integer.parseInt(f[0]) - 1; 
                }
                
                for (int k = 1; k < vIndices.length - 1; k++) {
                    Vertex3D v0 = vertexList.get(vIndices[0]);
                    Vertex3D v1 = vertexList.get(vIndices[k]);
                    Vertex3D v2 = vertexList.get(vIndices[k + 1]);
                    
                    Triangle3D tri = new Triangle3D(v0, v1, v2);
                    tri.setColor(defaultColor);
                    
                    triangleList.add(tri);
                }
            }
            // Procesar líneas ('l')
            else if (line.startsWith("l ")) {
                String[] p = line.split(" ");
                String[] tokens = getValidTokens(p);
                
                // Conectar múltiples puntos de la línea en pares continuos
                for (int k = 1; k < tokens.length - 1; k++) {
                    int idx0 = Integer.parseInt(tokens[k].split("/")[0]) - 1;
                    int idx1 = Integer.parseInt(tokens[k + 1].split("/")[0]) - 1;
                    
                    Vertex3D v0 = vertexList.get(idx0);
                    Vertex3D v1 = vertexList.get(idx1);
                    
                    Line3D line3D = new Line3D(v0, v1);
                    line3D.setColor(defaultColor);
                    lineList.add(line3D);
                }
            }
        }       

        // Extracción manual de arreglos
        Vertex3D[] finalVertices = new Vertex3D[vertexList.size()];
        for (int i = 0; i < vertexList.size(); i++) {
            finalVertices[i] = vertexList.get(i);
        }

        Triangle3D[] finalTriangles = new Triangle3D[triangleList.size()];
        for (int i = 0; i < triangleList.size(); i++) {
            finalTriangles[i] = triangleList.get(i);
        }

        // Extracción manual para líneas
        Line3D[] finalLines = new Line3D[lineList.size()];
        for (int i = 0; i < lineList.size(); i++) {
            finalLines[i] = lineList.get(i);
        }

        return new Mesh(finalVertices, finalTriangles, finalLines);
    }
    
    // Función de limpieza para descartar cadenas vacías generadas por dobles espacios
    private static String[] getValidTokens(String[] raw) {
        int count = 0;
        for (int i = 0; i < raw.length; i++) {
            if (!raw[i].trim().isEmpty()) {
                count++;
            }
        }
        
        String[] valid = new String[count];
        int idx = 0;
        for (int i = 0; i < raw.length; i++) {
            if (!raw[i].trim().isEmpty()) {
                valid[idx++] = raw[i].trim();
            }
        }
        return valid;
    }
}
