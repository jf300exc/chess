package ui;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

/** Compare square pixels and emit cursor-aligned patches containing complete SIXEL bands. */
final class BoardDamage {
    record Patch(int pixelRow, int pixelColumn, BufferedImage image) { }

    static List<Patch> between(BufferedImage old, BufferedImage next, int terminalCellHeight) {
        if (terminalCellHeight < 1 || next.getHeight() % terminalCellHeight != 0) {
            throw new IllegalArgumentException("Image must occupy complete terminal rows");
        }
        int bandRows = 6 / gcd(6, terminalCellHeight);
        int rows = next.getHeight() / terminalCellHeight;
        if (rows < bandRows) {
            throw new IllegalArgumentException("Image is too short for a cursor-aligned SIXEL band");
        }
        if (old == null || old.getWidth() != next.getWidth() || old.getHeight() != next.getHeight()) {
            int firstHeight = rows / bandRows * bandRows * terminalCellHeight;
            if (firstHeight == next.getHeight()) {
                return List.of(new Patch(0, 0, next));
            }
            // Overlap the final band inside the board, never spill into the file-label row.
            int tailHeight = bandRows * terminalCellHeight;
            int tailStart = next.getHeight() - tailHeight;
            return List.of(new Patch(0, 0, next.getSubimage(0, 0, next.getWidth(), firstHeight)),
                    new Patch(tailStart, 0, next.getSubimage(0, tailStart, next.getWidth(), tailHeight)));
        }
        int width = next.getWidth() / 8;
        int height = next.getHeight() / 8;
        List<Patch> patches = new ArrayList<>();
        for (int row = 0; row < 8; row++) {
            for (int column = 0; column < 8; column++) {
                boolean changed = false;
                for (int y = row * height; y < (row + 1) * height && !changed; y++) {
                    for (int x = column * width; x < (column + 1) * width; x++) {
                        if (old.getRGB(x, y) != next.getRGB(x, y)) {
                            changed = true;
                            break;
                        }
                    }
                }
                if (changed) {
                    int squareRows = height / terminalCellHeight;
                    int patchRows = (squareRows + bandRows - 1) / bandRows * bandRows;
                    int startRow = Math.min(row * squareRows, rows - patchRows);
                    int y = startRow * terminalCellHeight;
                    int x = column * width;
                    // Some terminals ignore transparent tail bits and expand to the full final band.
                    // Supply real neighboring pixels instead, shifting the bottom patch upward.
                    patches.add(new Patch(y, x, next.getSubimage(x, y, width, patchRows * terminalCellHeight)));
                }
            }
        }
        return patches;
    }

    private static int gcd(int a, int b) {
        while (b != 0) {
            int remainder = a % b;
            a = b;
            b = remainder;
        }
        return a;
    }
}
