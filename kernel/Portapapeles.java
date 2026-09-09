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
package kernel;

import java.io.File;

// Implementación específica para Boot.java
public class Portapapeles {

    public static final int INVALID_LBA = -1;
    private static final int MAX_TEXTOS = 32;

    private File archivo;
    private String[] textos;

    private int origenLba;
    private boolean esCortar;
    private int cantidadTexto;


    public Portapapeles() {
        this.archivo = null;
        this.textos = new String[MAX_TEXTOS];

        this.origenLba = INVALID_LBA;
        this.esCortar = false;
        this.cantidadTexto = 0;
    }

    // Captura un texto en el portapapeles.
    public boolean capturarTexto(String texto) {
        if (texto == null) {
            return false;
        }
        if (cantidadTexto >= textos.length) {
            return false;
        }
        // Eliminar estado de archivo anterior
        limpiarArchivo();
        textos[cantidadTexto++] = texto;
        return true;
    }

    // Captura un archivo.
    public boolean capturarArchivo(File f, int lba, boolean cortar) {
        if (f == null) {
            return false;
        }
        // Eliminar textos anteriores
        limpiarTextos();
        this.archivo = f;
        this.origenLba = lba;
        this.esCortar = cortar;
        return true;
    }

    public File obtenerArchivo() {
        return archivo;
    }

    public String obtenerTexto(int pos) {
        if (pos < 0 || pos >= cantidadTexto) {
            return "";
        }
        return textos[pos];
    }

    public int getCantidadTextos() { return cantidadTexto; }
    public int obtenerLba() { return origenLba; }
    public boolean esCortar() { return archivo != null && esCortar; }
    public boolean tieneArchivo() { return archivo != null; }
    public boolean tieneTexto() { return cantidadTexto > 0; }

    // Elimina únicamente el archivo almacenado.
    public void limpiarArchivo() {
        archivo = null;
        origenLba = INVALID_LBA;
        esCortar = false;
    }

    // Elimina todos los textos almacenados.
    public void limpiarTextos() {
        for (int i = 0; i < cantidadTexto; i++) {
            textos[i] = null;
        }
        cantidadTexto = 0;
    }

    // Limpia completamente el portapapeles.
    public void limpiar() {
        limpiarArchivo();
        limpiarTextos();
    }
}
