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

public class Matrix3D {
  // Inspirado en la implementación de OpenGL
    // Fixed point 8.8 (256 = 1.0)
    public static final int FIXED_ONE = 256;

    public int m00;
    public int m01;
    public int m02;
    public int tx;

    public int m10;
    public int m11;
    public int m12;
    public int ty;

    public int m20;
    public int m21;
    public int m22;
    public int tz;


    public Matrix3D() {
        setIdentity();
    }

    // Inicializar vértices
    public void setIdentity() {
        m00 = FIXED_ONE;
        m01 = 0;
        m02 = 0;
        tx = 0;

        m10 = 0;
        m11 = FIXED_ONE;
        m12 = 0;
        ty = 0;

        m20 = 0;
        m21 = 0;
        m22 = FIXED_ONE;
        tz = 0;
    }

    // Movimiento en ejes
    public void setTranslation(int x,int y,int z) {
        setIdentity();
        tx = x;
        ty = y;
        tz = z;
    }

    // Escalas
    public void setScale(int sx,int sy,int sz) {
        setIdentity();
        m00 = sx * FIXED_ONE;
        m11 = sy * FIXED_ONE;
        m22 = sz * FIXED_ONE;
    }

    // Rotaciones en X e Y e Z
    public void setRotationX(int sin, int cos) {
        setIdentity();
        m11 = cos;
        m12 = -sin;
        m21 = sin;
        m22 = cos;
    }

    public void setRotationY(int sin, int cos) {
        setIdentity();
        m00 = cos;
        m02 = sin;
        m20 = -sin;
        m22 = cos;
    }

    public void setRotationZ(int sin, int cos) {
        setIdentity();
        m00 = cos;
        m01 = -sin;
        m10 = sin;
        m11 = cos;
    }

    // Transforma un Vertex3D. (Basado en OpenGL)
    public Vertex3D transform(Vertex3D v) {
        int x;
        int y;
        int z;
        x = ((v.x * m00) + (v.y * m01) + (v.z * m02)) >> 8;
        y = ((v.x * m10) + (v.y * m11) + (v.z * m12)) >> 8;
        z = ((v.x * m20) + (v.y * m21) + (v.z * m22)) >> 8;

        x += tx;
        y += ty;
        z += tz;

        return new Vertex3D(x, y, z);
    }
}
