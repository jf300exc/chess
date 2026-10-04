package ui;

import org.jline.utils.AttributedString;
import org.jline.utils.AttributedStringBuilder;
import org.jline.utils.InfoCmp.Capability;

import java.util.ArrayList;
import java.util.List;

/** Fixed-position damage updates: never scroll, insert lines, or erase unchanged graphics. */
final class TerminalScreen {
    record Change(int row, int column, AttributedString text) { }
    record ImageArea(int row, int column, int rows, int columns) { }
    private List<AttributedString> previous = List.of();

    void clear() {
        previous = List.of();
    }

    List<Change> changes(List<AttributedString> lines, int columns) {
        List<Change> changes = new ArrayList<>();
        List<AttributedString> padded = new ArrayList<>();
        for (int row = 0; row < lines.size(); row++) {
            AttributedString clipped = lines.get(row).columnSubSequence(0, columns);
            AttributedString next = new AttributedStringBuilder().append(clipped)
                    .append(" ".repeat(Math.max(0, columns - clipped.columnLength()))).toAttributedString();
            padded.add(next);
            AttributedString old = row < previous.size() ? previous.get(row) : AttributedString.EMPTY;
            if (old.equals(next)) {
                continue;
            }
            int prefix = 0;
            while (prefix < Math.min(old.length(), next.length()) && same(old, prefix, next, prefix)) {
                prefix++;
            }
            // Never split a surrogate pair or leave a combining mark's base unpainted.
            if (prefix > 0) {
                prefix = Character.offsetByCodePoints(next, prefix, -1);
                if (prefix > 0 && Character.isLowSurrogate(next.charAt(prefix))) {
                    prefix--;
                }
            }
            int oldEnd = old.length();
            int nextEnd = next.length();
            while (oldEnd > prefix && nextEnd > prefix && same(old, oldEnd - 1, next, nextEnd - 1)) {
                oldEnd--;
                nextEnd--;
            }
            if (nextEnd < next.length() && Character.isLowSurrogate(next.charAt(nextEnd))) {
                nextEnd++;
            }
            changes.add(new Change(row, next.subSequence(0, prefix).columnLength(), next.subSequence(prefix, nextEnd)));
        }
        previous = padded;
        return changes;
    }

    private static boolean same(AttributedString a, int ai, AttributedString b, int bi) {
        return a.charAt(ai) == b.charAt(bi) && a.styleAt(ai).equals(b.styleAt(bi));
    }

    void update(org.jline.terminal.Terminal terminal, List<AttributedString> lines, int width,
                int cursorRow, int cursorColumn, ImageArea image) {
        for (Change change : changes(lines, Math.max(1, width - 1))) {
            if (image != null && change.row() >= image.row() && change.row() < image.row() + image.rows()) {
                int left = Math.max(0, image.column() - change.column());
                int right = Math.max(0, image.column() + image.columns() - change.column());
                write(terminal, change.row(), change.column(), change.text().columnSubSequence(0,
                        Math.min(left, change.text().columnLength())));
                if (right < change.text().columnLength()) {
                    write(terminal, change.row(), change.column() + right,
                            change.text().columnSubSequence(right, change.text().columnLength()));
                }
            } else {
                write(terminal, change.row(), change.column(), change.text());
            }
        }
        terminal.puts(Capability.cursor_address, cursorRow, cursorColumn);
    }

    private static void write(org.jline.terminal.Terminal terminal, int row, int column, AttributedString text) {
        if (text.length() > 0) {
            terminal.puts(Capability.cursor_address, row, column);
            terminal.writer().print(text.toAnsi(terminal));
        }
    }
}
