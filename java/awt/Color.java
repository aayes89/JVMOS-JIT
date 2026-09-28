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

package java.awt;

public class Color {
    public static final int BLACK       = 0xFF000000;
    public static final int WHITE       = 0xFFFFFFFF;
    public static final int RED         = 0xFFFF0000;
    public static final int GREEN       = 0xFF008000;
    public static final int BLUE        = 0xFF0000FF;
    public static final int YELLOW      = 0xFFFFFF00;
    public static final int GRAY        = 0xFF808080;
    public static final int LIGHT_GRAY  = 0xFFC0C0C0;
    public static final int DARK_GRAY   = 0xFF404040;
	public static final int CYAN 		= 0xFF00FFFF;
	public static final int MAGENT      = 0xFFFF00FF;
	public static final int WIN_BG 		= 0xFFE0E0E0; 
	public static final int WIN_TITLE 	= 0xFF000080;
    public static final int SEL 		= 0xFFD0D0FF;
	public static final int FOLDER 		= 0xFFF0C000; 
	public static final int	FILE		= 0xFFA0A0A0;
    public static final int TRANSPARENT = 0x00000000; // Dependiendo de tu alfa

    private final int rgb;
	
	// constructores	
    public Color(int rgb) {
        this.rgb = rgb;
    }

    public Color(int r, int g, int b) {
        this.rgb = (0xFF << 24) | ((r & 0xFF) << 16) | ((g & 0xFF) << 8) | (b & 0xFF);
    }

    public int getRGB() {
        return rgb;
    }
}
