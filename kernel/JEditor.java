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

import java.awt.ui.JFrame;
import java.awt.ui.JButton;
import java.awt.ui.JTextArea;
import java.io.FileSystem;
import java.io.File;

// Funcionalidades extraídas de la implementación de JPad y shell en proyectos previos
public class JEditor extends JFrame {
    private FileSystem fs;
    private File currentFile;
    private int currentDirLba;
    
    // Componente visual de edición
    private JTextArea textArea;
    private JButton btnSave;

    public JEditor(FileSystem fs) {
        // Inicializa el JFrame (Padre) con título y coordenadas base
        super("JEditor - Nuevo Documento", 200, 100, 600, 400);
        this.fs = fs;
        
        // Instanciar los componentes visuales internos (Coordenadas relativas al JFrame)
        btnSave = new JButton("Guardar", 10, 30, 80, 24, () -> saveFile());
        textArea = new JTextArea(10, 60, 580, 330);
        
        // Añadir los componentes al JFrame
        this.add(btnSave);
        this.add(textArea);
    }

    public void openFile(File file, int dirLba) {
        this.currentFile = file;
        this.currentDirLba = dirLba;
        
        if (file != null && file.exists()) {
            this.setTitle("JEditor - " + file.getName()); // Asume que agregaste setTitle a JFrame
            byte[] data = fs.readFile(file);
            if (data != null) {
                // Convertir bytes a char y pasar al TextArea
                char[] chars = new char[data.length];
                for (int i = 0; i < data.length; i++) {
                    chars[i] = (char) data[i];
                }
                textArea.setText(new String(chars));
            }
        } else {
            this.setTitle("JEditor - Nuevo Documento");
            textArea.setText("");
        }
        
        // Mostrar la ventana
        this.setVisible(true);
    }

    private void saveFile() {
        if (currentFile != null) {
            String text = textArea.getText();
            byte[] data = new byte[text.length()];
            for (int i = 0; i < text.length(); i++) {
                data[i] = (byte) text.charAt(i);
            }
            fs.delete(currentFile, currentDirLba);
            fs.writeFile(currentFile.getName(), data, currentDirLba);
        }
    }
    
    @Override
    public boolean handleKey(int ascii) {
        if (!isVisible()) return false;                
        return textArea.handleKey(ascii);
    }
}
