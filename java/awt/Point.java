/*
 * The MIT License
 *
 * Copyright 2026 Slam.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
 
package java.awt;

import java.lang.Math;

public class Point {
    public float x;
    public float y;
    public long timeOffsetNano;

    public Point(float x, float y) {
        this.x = x;
        this.y = y;
        this.timeOffsetNano = 0;
    }

    public Point(float x, float y, long timeOffsetNano) {
        this.x = x;
        this.y = y;
        this.timeOffsetNano = timeOffsetNano;
    }

    public boolean equals(Point p) {
        return x == p.x && y == p.y;
    }

    public float dist(Point a) {
        return (float) Math.hypot(a.x - x, a.y - y);
    }
   
	// Calcular productos cruzados de vectores
    public float crossProduct(Point a, Point b) {
        return (a.x - x) * (b.y - y) - (a.y - y) * (b.x - x);
    }

    // Calcula el producto escalar de vec(this, a) y vec(this, b), 
	// donde vec(x, y) es el vector que va del punto x al punto y.
    public float dotProduct(Point a, Point b) {
        return (a.x - x) * (b.x - x) + (a.y - y) * (b.y - y);
    }

    // Calcula el ángulo en radianes formado por los puntos (a, this, b).
	// Si dos cualesquiera de estos puntos son iguales, el método devolverá 0.0f.    
    public float getAngle(Point a, Point b) {
        float dist1 = dist(a);
        float dist2 = dist(b);

        if (dist1 == 0.0f || dist2 == 0.0f) {
            return 0.0f;
        }

        float crossProduct = crossProduct(a, b);
        float dotProduct = dotProduct(a, b);
        float cos = Math.min(1.0f, Math.max(-1.0f, dotProduct / dist1 / dist2));
        float angle = (float) Math.acos(cos);
        if (crossProduct < 0.0) {
            angle = 2.0f * (float) Math.PI - angle;
        }
        return angle;
    }
}
