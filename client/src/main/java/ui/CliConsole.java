package ui;

import org.jline.reader.EndOfFileException;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.UserInterruptException;
import org.jline.terminal.TerminalBuilder;
import org.jline.utils.InfoCmp.Capability;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;

/** One input owner for the lobby and gameplay; no competing stdin scanners. */
public final class CliConsole implements AutoCloseable {
    private final org.jline.terminal.Terminal terminal;
    private final LineReader lineReader;
    private final BufferedReader plainReader;
    private final boolean allowMouse;
    private final PieceSymbols pieceSymbols;
    private boolean ended;

    private CliConsole(org.jline.terminal.Terminal terminal, boolean allowMouse, PieceSymbols pieceSymbols) {
        this.terminal = terminal;
        this.allowMouse = allowMouse;
        this.pieceSymbols = pieceSymbols;
        lineReader = terminal == null ? null : LineReaderBuilder.builder().terminal(terminal).build();
        plainReader = terminal == null
                ? new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8)) : null;
    }

    public static CliConsole open(boolean textOnly, boolean noMouse) {
        return open(textOnly, noMouse, PieceSymbols.UNICODE);
    }

    public static CliConsole open(boolean textOnly, boolean noMouse, PieceSymbols pieceSymbols) {
        String term = System.getenv("TERM");
        if (!textOnly && System.console() != null && !"dumb".equals(term)) {
            org.jline.terminal.Terminal candidate = null;
            try {
                candidate = TerminalBuilder.builder().system(true).dumb(false).build();
                if (candidate.getStringCapability(Capability.cursor_address) != null
                        && candidate.getStringCapability(Capability.enter_ca_mode) != null) {
                    return new CliConsole(candidate, !noMouse, pieceSymbols);
                }
            } catch (IOException | RuntimeException | LinkageError e) {
                System.err.println("Terminal controls unavailable; using text commands.");
            }
            if (candidate != null) {
                try {
                    candidate.close();
                } catch (IOException ignored) {
                    // The fallback below still uses ordinary standard input.
                }
            }
        }
        return new CliConsole(null, false, pieceSymbols);
    }

    org.jline.terminal.Terminal terminal() {
        return terminal;
    }

    boolean allowMouse() {
        return allowMouse;
    }

    PieceSymbols pieceSymbols() {
        return pieceSymbols;
    }

    /** null denotes EOF or Ctrl-C, including during a follow-up prompt. */
    public String readLine(String prompt) {
        if (ended) {
            return null;
        }
        if (lineReader != null) {
            try {
                return lineReader.readLine(prompt);
            } catch (EndOfFileException | UserInterruptException e) {
                ended = true;
                return null;
            }
        }
        System.out.print(prompt);
        System.out.flush();
        try {
            String input = plainReader.readLine();
            ended = input == null;
            return input;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    void endInput() {
        ended = true;
    }

    @Override
    public void close() throws IOException {
        if (terminal != null) {
            terminal.close();
        }
    }
}
