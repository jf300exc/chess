package ui;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

/** Compare the painted pixels, so even duplicate server snapshots produce no board output. */
final class BoardDamage {
    record Patch(int row, int column, BufferedImage image) { }

    static List<Patch> between(BufferedImage old, BufferedImage next) {
        if (old == null || old.getWidth() != next.getWidth() || old.getHeight() != next.getHeight()) {
            return List.of(new Patch(0, 0, next));
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
                    patches.add(new Patch(row, column, next.getSubimage(column * width, row * height, width, height)));
                }
            }
        }
        return patches;
    }
}
