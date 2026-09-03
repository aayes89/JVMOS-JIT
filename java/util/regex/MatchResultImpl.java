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

package java.util.regex;

class MatchResultImpl implements MatchResult {
    
    private String text;
    private int[] offsets;

    MatchResultImpl(String text, int[] offsets) {
        this.text = text;
        this.offsets = offsets.clone();
    }

    public int end() {
        return end(0);
    }

    public int end(int group) {
        return offsets[2 * group + 1];
    }

    public String group() {
        return text.substring(start(), end());
    }

    public String group(int group) {
        int from = offsets[group * 2];
        int to = offsets[(group * 2) + 1];
        if (from == -1 || to == -1) {
            return null;
        } else {
            return text.substring(from, to);
        }
    }

    public int groupCount() {
        return (offsets.length / 2) - 1;
    }

    public int start() {
        return start(0);
    }

    public int start(int group) {
        return offsets[2 * group];
    }

}
