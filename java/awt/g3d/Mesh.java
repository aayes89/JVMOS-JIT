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

public class Mesh {

    public Vertex3D[] vertices;
    public Triangle3D[] triangles;

    public Mesh(Vertex3D[] vertices, Triangle3D[] triangles) {
        this.vertices = vertices;
        this.triangles = triangles;
    }

    public int getVertexCount() {
        if (vertices == null) {
            return 0;
        }
        return vertices.length;
    }

    public int getTriangleCount() {
        if (triangles == null) {
            return 0;
        }
        return triangles.length;
    }

    public Vertex3D getVertex(int index) {
        return vertices[index];
    }

    public Triangle3D getTriangle(int index) {
        return triangles[index];
    }
}
