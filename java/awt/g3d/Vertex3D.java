
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

public class Vertex3D {
    public int x;
    public int y;
    public int z;

    public Vertex3D() {
		this.x = 0;
		this.y = 0;
		this.z = 0;
    }

    public Vertex3D(int x, int y, int z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }
	// getters y setters
    public int getX() {
		return x; 
	}
    public int getY() { 
		return y; 
	}
    public int getZ() { 
		return z; 
	}
    public void setX(int nx) {
		x = nx; 
	}
    public void setY(int ny) {
		y = ny; 
	}
    public void setZ(int nz) { 
		z = nz; 
	}
}
