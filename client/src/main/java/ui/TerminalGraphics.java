package ui;

import org.jline.utils.NonBlockingReader;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Bounded, read-only terminal queries. Only advertise graphics after a positive reply. */
final class TerminalGraphics {
    record CellPixels(int width, int height) {
        CellPixels {
            if (width < 1 || width > 64 || height < 1 || height > 128) {
                throw new IllegalArgumentException("Invalid terminal pixel geometry");
            }
        }
    }

    record Probe(CellPixels pixels, Deque<Integer> pendingInput, boolean restoreDisplayMode) { }

    private static final Pattern DEVICE = Pattern.compile("\u001b\\[\\?([0-9;]+)c");
    private static final Pattern SIZE = Pattern.compile("\u001b\\[([46]);([0-9]+);([0-9]+)t");
    private static final Pattern DISPLAY_MODE = Pattern.compile("\u001b\\[\\?80;([0-4])\\$y");

    static Probe probe(org.jline.terminal.Terminal terminal) throws IOException {
        terminal.writer().print("\u001b[?80$p\u001b[c\u001b[16t\u001b[14t");
        terminal.flush();
        StringBuilder replies = new StringBuilder();
        Deque<Integer> pending = new ArrayDeque<>();
        StringBuilder sequence = new StringBuilder();
        long deadline = System.nanoTime() + 350_000_000L;
        while (System.nanoTime() < deadline && replies.length() < 1024 && pending.size() < 256) {
            int ch = terminal.reader().read(25);
            if (ch == NonBlockingReader.READ_EXPIRED) {
                continue;
            }
            if (ch < 0) {
                pending.add(ch);
                break;
            }
            if (sequence.isEmpty() && ch != 27) {
                pending.add(ch);
                continue;
            }
            sequence.append((char) ch);
            boolean complete = sequence.length() >= 3 && ch >= 0x40 && ch <= 0x7e;
            if (complete || sequence.length() >= 128 || sequence.length() == 2 && ch != '[') {
                String value = sequence.toString();
                if (DEVICE.matcher(value).matches() || SIZE.matcher(value).matches() || DISPLAY_MODE.matcher(value).matches()) {
                    replies.append(value);
                } else {
                    value.chars().forEach(pending::add);
                }
                sequence.setLength(0);
                if (parse(replies.toString(), terminal.getWidth(), terminal.getHeight()) != null) {
                    break;
                }
            }
        }
        sequence.chars().forEach(pending::add);
        CellPixels pixels = parse(replies.toString(), terminal.getWidth(), terminal.getHeight());
        Matcher mode = DISPLAY_MODE.matcher(replies);
        boolean restoreMode = false;
        if (mode.find()) {
            // Mode 80 puts graphics at the page origin rather than the cursor.
            // Permanent-set mode cannot be temporarily disabled safely.
            if (mode.group(1).equals("3")) {
                pixels = null;
            }
            restoreMode = pixels != null && mode.group(1).equals("1");
        }
        return new Probe(pixels, pending, restoreMode);
    }

    static CellPixels parse(String replies, int columns, int rows) {
        Matcher device = DEVICE.matcher(replies);
        boolean sixel = false;
        while (device.find()) {
            // The first parameter is a terminal model, not a feature flag.
            String[] features = device.group(1).split(";");
            sixel |= Arrays.stream(features).skip(1).anyMatch("4"::equals);
        }
        if (!sixel) {
            return null;
        }
        CellPixels windowPixels = null;
        Matcher size = SIZE.matcher(replies);
        while (size.find()) {
            try {
                int height = Integer.parseInt(size.group(2));
                int width = Integer.parseInt(size.group(3));
                if (size.group(1).equals("6")) {
                    return new CellPixels(width, height);
                }
                if (columns > 0 && rows > 0 && width % columns == 0 && height % rows == 0) {
                    windowPixels = new CellPixels(width / columns, height / rows);
                }
            } catch (IllegalArgumentException ignored) {
                // Incomplete, invalid or excessive sizes cannot enable image output.
            }
        }
        return windowPixels;
    }
}
