package ui;

import java.awt.image.BufferedImage;
import java.util.BitSet;

/** Small, bounded 216-color SIXEL encoder; no external process or library needed. */
final class SixelEncoder {
    static String encode(BufferedImage image) {
        int width = image.getWidth();
        int height = image.getHeight();
        if (width > 1024 || height > 1024) {
            throw new IllegalArgumentException("Terminal board image is too large");
        }
        int[] indexed = new int[width * height];
        BitSet used = new BitSet(216);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int rgb = image.getRGB(x, y);
                int color = ((rgb >> 16 & 255) + 25) / 51 * 36
                        + ((rgb >> 8 & 255) + 25) / 51 * 6 + ((rgb & 255) + 25) / 51;
                indexed[y * width + x] = color;
                used.set(color);
            }
        }
        StringBuilder out = new StringBuilder("\u001bP0;1;0q\"1;1;" + width + ";" + height);
        for (int color = used.nextSetBit(0); color >= 0; color = used.nextSetBit(color + 1)) {
            out.append('#').append(color).append(";2;").append(color / 36 * 20).append(';')
                    .append(color / 6 % 6 * 20).append(';').append(color % 6 * 20);
        }
        for (int y = 0; y < height; y += 6) {
            BitSet band = new BitSet(216);
            for (int row = y; row < Math.min(height, y + 6); row++) {
                for (int x = 0; x < width; x++) {
                    band.set(indexed[row * width + x]);
                }
            }
            boolean first = true;
            for (int color = band.nextSetBit(0); color >= 0; color = band.nextSetBit(color + 1)) {
                if (!first) {
                    out.append('$');
                }
                first = false;
                out.append('#').append(color);
                int previous = -1;
                int repeat = 0;
                for (int x = 0; x < width; x++) {
                    int bits = 0;
                    for (int bit = 0; bit < 6 && y + bit < height; bit++) {
                        if (indexed[(y + bit) * width + x] == color) {
                            bits |= 1 << bit;
                        }
                    }
                    if (bits != previous && repeat > 0) {
                        appendRun(out, previous, repeat);
                        repeat = 0;
                    }
                    previous = bits;
                    repeat++;
                }
                appendRun(out, previous, repeat);
            }
            if (y + 6 < height) {
                out.append('-');
            }
        }
        return out.append("\u001b\\").toString();
    }

    private static void appendRun(StringBuilder out, int bits, int repeat) {
        char value = (char) (63 + bits);
        if (repeat >= 4) {
            out.append('!').append(repeat).append(value);
        } else {
            out.append(Character.toString(value).repeat(repeat));
        }
    }
}
