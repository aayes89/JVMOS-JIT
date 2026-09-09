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

    // Multiplica esta matriz por otra. (Gracias Algebralineal 1 y 2!)
    public void multiply(Matrix3D other) {
        int n00 = ((m00 * other.m00) + (m01 * other.m10) + (m02 * other.m20)) >> 8;
        int n01 = ((m00 * other.m01) + (m01 * other.m11) + (m02 * other.m21)) >> 8;
        int n02 = ((m00 * other.m02) + (m01 * other.m12) + (m02 * other.m22)) >> 8;
        int n10 = ((m10 * other.m00) + (m11 * other.m10) + (m12 * other.m20)) >> 8;
        int n11 = ((m10 * other.m01) + (m11 * other.m11) + (m12 * other.m21)) >> 8;
        int n12 = ((m10 * other.m02) + (m11 * other.m12) + (m12 * other.m22)) >> 8;
        int n20 = ((m20 * other.m00) + (m21 * other.m10) + (m22 * other.m20)) >> 8;
        int n21 = ((m20 * other.m01) + (m21 * other.m11) + (m22 * other.m21)) >> 8;
        int n22 = ((m20 * other.m02) + (m21 * other.m12) + (m22 * other.m22)) >> 8;
        int ntx = (( tx * other.m00) + ( ty * other.m01) + ( tz * other.m02)) >> 8;
        int nty = (( tx * other.m10) + ( ty * other.m11) + ( tz * other.m12)) >> 8;
        int ntz = (( tx * other.m20) + ( ty * other.m21) + ( tz * other.m22)) >> 8;
        ntx += other.tx;
        nty += other.ty;
        ntz += other.tz;
        m00 = n00;
        m01 = n01;
        m02 = n02;
        tx = ntx;		
        m10 = n10;
        m11 = n11;
        m12 = n12;
        ty = nty;
        m20 = n20;
        m21 = n21;
        m22 = n22;
        tz = ntz;
    }
}
