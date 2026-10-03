package ui;

import chess.ChessGame;
import chess.ChessPosition;
import org.jline.terminal.Attributes;
import org.jline.terminal.Terminal.MouseTracking;
import org.jline.utils.AttributedString;
import org.jline.utils.Display;
import org.jline.utils.InfoCmp.Capability;
import org.jline.utils.NonBlockingReader;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.List;
import java.util.function.BooleanSupplier;

/** Gameplay screen and input, rendered by the input thread from synchronized snapshots. */
public final class Terminal {
    private static final Object LOCK = new Object();
    private static final int BOARD_START_ROW = 2;
    private static final Deque<String> LOG = new ArrayDeque<>();
    private static ChessGame game;
    private static String gameName;
    private static ChessGame.TeamColor perspective = ChessGame.TeamColor.WHITE;
    private static ChessPosition selected;
    private static long revision;
    private static CliConsole console;
    private static org.jline.terminal.Terminal terminal;
    private static Attributes savedAttributes;
    private static Display display;
    private static boolean active;
    private static boolean mouseEnabled;
    private static BoardDraw.Layout layout;
    private static boolean boardVisible;
    private static final StringBuilder commandBuffer = new StringBuilder();
    private static int commandCursor;
    private static Thread shutdownHook;
    private static List<String> helpLines;
    private static int renderedWidth = -1;
    private static int renderedHeight = -1;

    public record Input(String text, ChessPosition square, boolean cancel) { }

    private Terminal() {
    }

    public static void start(CliConsole input, String color) {
        synchronized (LOCK) {
            console = input;
            terminal = input.terminal();
            game = null;
            gameName = "Connecting";
            perspective = ChessGame.TeamColor.valueOf(color);
            selected = null;
            LOG.clear();
            commandBuffer.setLength(0);
            commandCursor = 0;
            helpLines = null;
            renderedWidth = -1;
            renderedHeight = -1;
            active = true;
            revision++;
        }
        if (terminal != null) {
            savedAttributes = terminal.enterRawMode();
            Attributes rawAttributes = terminal.getAttributes();
            rawAttributes.setLocalFlag(Attributes.LocalFlag.ISIG, false);
            terminal.setAttributes(rawAttributes);
            terminal.puts(Capability.enter_ca_mode);
            terminal.puts(Capability.clear_screen);
            terminal.puts(Capability.cursor_address, 0, 0);
            terminal.flush();
            mouseEnabled = input.allowMouse() && terminal.hasMouseSupport() && terminal.trackMouse(MouseTracking.Normal);
            if (mouseEnabled) {
                // Prefer SGR coordinates; older terminals can still send legacy reports.
                terminal.writer().print("\u001b[?1005l\u001b[?1006h");
                terminal.flush();
            }
            display = new Display(terminal, true);
            shutdownHook = new Thread(Terminal::stop, "chess-terminal-cleanup");
            Runtime.getRuntime().addShutdownHook(shutdownHook);
        } else {
            System.out.println("Text controls: move e2e4 | moves e2 | highlight e2 | flip | status | leave");
        }
    }

    public static void stop() {
        synchronized (LOCK) {
            if (!active) {
                return;
            }
            active = false;
            selected = null;
        }
        if (terminal != null) {
            try {
                terminal.trackMouse(MouseTracking.Off);
                terminal.writer().print("\u001b[?1005l\u001b[?1006l\u001b[0m");
                terminal.puts(Capability.exit_ca_mode);
                terminal.flush();
            } finally {
                if (savedAttributes != null) {
                    terminal.setAttributes(savedAttributes);
                }
            }
        }
        if (shutdownHook != null && Thread.currentThread() != shutdownHook) {
            try {
                Runtime.getRuntime().removeShutdownHook(shutdownHook);
            } catch (IllegalStateException ignored) {
                // JVM shutdown already owns the hook.
            }
        }
        display = null;
        savedAttributes = null;
        mouseEnabled = false;
        boardVisible = false;
    }

    public static boolean notReadyForInput() {
        synchronized (LOCK) {
            return game == null;
        }
    }

    public static void refresh() {
        synchronized (LOCK) {
            revision++;
            if (display != null) {
                display.clear();
            } else if (game != null) {
                printPlainBoard();
            }
        }
    }

    public static void addNotification(String message) {
        addLogMessage(message);
    }

    public static void addLogMessage(String message) {
        if (message == null) {
            return;
        }
        synchronized (LOCK) {
            for (String line : message.split("\n")) {
                LOG.addLast(safeText(line));
            }
            while (LOG.size() > 100) {
                LOG.removeFirst();
            }
            revision++;
            if (active && terminal == null) {
                System.out.println(safeText(message));
            }
        }
    }

    public static void setChessGame(ChessGame newGame, String name) {
        synchronized (LOCK) {
            game = newGame == null ? null : newGame.copy();
            if (name != null) {
                gameName = safeText(name);
            }
            selected = null;
            revision++;
            if (active && terminal == null && game != null) {
                printPlainBoard();
            }
        }
    }

    public static ChessGame getChessGame() {
        synchronized (LOCK) {
            return game == null ? null : game.copy();
        }
    }

    public static void drawHighlights(ChessPosition position) {
        synchronized (LOCK) {
            selected = position;
            revision++;
            if (active && terminal == null && game != null) {
                printPlainBoard();
            }
        }
    }

    public static ChessGame.TeamColor getPlayerColor() {
        synchronized (LOCK) {
            return perspective;
        }
    }

    public static void flipBoard() {
        synchronized (LOCK) {
            perspective = perspective == ChessGame.TeamColor.WHITE ? ChessGame.TeamColor.BLACK : ChessGame.TeamColor.WHITE;
            selected = null;
            revision++;
            if (active && terminal == null && game != null) {
                printPlainBoard();
            }
        }
    }

    public static String getInput(String prompt) {
        String text = readInput(prompt, false, () -> true).text();
        if (text == null) {
            console.endInput();
            throw new org.jline.reader.EndOfFileException();
        }
        return text;
    }

    public static Input getGameInput(String prompt, BooleanSupplier connected) {
        return readInput(prompt, true, connected);
    }

    public static void showHelp(String help) {
        if (terminal == null) {
            System.out.println(help);
            return;
        }
        List<String> allLines = Arrays.asList(help.split("\n"));
        int offset = 0;
        try {
            while (offset < allLines.size()) {
                int pageSize = Math.max(1, terminal.getHeight() - 2);
                int end = Math.min(allLines.size(), offset + pageSize);
                synchronized (LOCK) {
                    helpLines = new ArrayList<>(allLines.subList(offset, end));
                    revision++;
                }
                String reply = getInput(end < allLines.size() ? "Enter: next page | q: close > " : "Enter: return to game > ");
                if (!reply.isBlank()) {
                    break;
                }
                offset = end;
            }
        } finally {
            synchronized (LOCK) {
                helpLines = null;
                revision++;
            }
        }
    }

    private static Input readInput(String prompt, boolean acceptClicks, BooleanSupplier connected) {
        if (terminal == null) {
            String line = console.readLine(prompt);
            return new Input(line == null ? null : line.trim(), null, false);
        }
        StringBuilder buffer = acceptClicks ? commandBuffer : new StringBuilder();
        int cursor = acceptClicks ? commandCursor : 0;
        TerminalEventDecoder decoder = new TerminalEventDecoder();
        long renderedRevision = -1;
        int previousWidth = renderedWidth;
        int previousHeight = renderedHeight;
        boolean inputChanged = true;
        try {
            for (;;) {
                if (!connected.getAsBoolean()) {
                    throw new IOException("The live game connection is closed.");
                }
                int width = Math.max(1, terminal.getWidth());
                int height = Math.max(1, terminal.getHeight());
                synchronized (LOCK) {
                    if (inputChanged || revision != renderedRevision || width != previousWidth || height != previousHeight) {
                        if (width != previousWidth || height != previousHeight) {
                            display.clear();
                        }
                        render(prompt, buffer.toString(), cursor, width, height);
                        renderedRevision = revision;
                        previousWidth = width;
                        previousHeight = height;
                        renderedWidth = width;
                        renderedHeight = height;
                        inputChanged = false;
                    }
                }
                int character = terminal.reader().read(100);
                TerminalEventDecoder.Event event = character == NonBlockingReader.READ_EXPIRED
                        ? decoder.idle() : decoder.feed(character);
                if (event == null) {
                    continue;
                }
                switch (event.key()) {
                    case EXIT -> {
                        console.endInput();
                        return new Input(null, null, false);
                    }
                    case ENTER -> {
                        String text = buffer.toString().trim();
                        buffer.setLength(0);
                        commandCursor = 0;
                        return new Input(text, null, false);
                    }
                    case CANCEL -> {
                        buffer.setLength(0);
                        cursor = 0;
                        if (acceptClicks) {
                            commandCursor = 0;
                            return new Input(null, null, true);
                        }
                        return new Input("", null, false);
                    }
                    case MOUSE -> {
                        if (!mouseEnabled || !acceptClicks) {
                            continue;
                        }
                        if (event.rightButton()) {
                            commandCursor = cursor;
                            return new Input(null, null, true);
                        }
                        // A resize between paint and input invalidates the old hit map.
                        if (boardVisible && previousWidth == terminal.getWidth() && previousHeight == terminal.getHeight()) {
                            ChessPosition square = layout.positionAt(event.x(), event.y() - BOARD_START_ROW, getPlayerColor());
                            if (square != null) {
                                commandCursor = cursor;
                                return new Input(null, square, false);
                            }
                        }
                    }
                    case BACKSPACE -> {
                        if (cursor > 0) {
                            buffer.deleteCharAt(--cursor);
                        }
                    }
                    case DELETE -> {
                        if (cursor < buffer.length()) {
                            buffer.deleteCharAt(cursor);
                        }
                    }
                    case LEFT -> cursor = Math.max(0, cursor - 1);
                    case RIGHT -> cursor = Math.min(buffer.length(), cursor + 1);
                    case HOME -> cursor = 0;
                    case END -> cursor = buffer.length();
                    case TEXT -> {
                        if (buffer.length() < 256) {
                            buffer.insert(cursor++, (char) event.character());
                        }
                    }
                }
                inputChanged = true;
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static void render(String prompt, String input, int cursor, int width, int height) {
        layout = BoardDraw.Layout.forSize(width, height);
        List<String> lines = new ArrayList<>();
        if (helpLines != null) {
            lines.addAll(helpLines);
            boardVisible = false;
        } else {
            lines.add("\u001b[1mCHESS  /  " + gameName + "\u001b[0m");
            lines.add(game == null ? "Waiting for game data..." : statusLine(game) + "  |  View: " + perspective);
            boardVisible = game != null && width >= layout.width() && height >= layout.height() + 6;
            if (boardVisible) {
                lines.addAll(Arrays.asList(BoardDraw.draw(game.copy(), perspective, selected, layout, true).split("\n")));
            } else {
                lines.add("Board needs 38 columns x 16 rows. Resize or use text commands.");
            }
            lines.add("w White / b Black | K king Q queen R rook B bishop N knight P pawn");
            lines.add(mouseEnabled ? "Click piece -> destination | right-click/Esc cancels | help"
                    : "Text controls | move e2e4 | highlight e2 | flip | help");
            int logSpace = Math.max(0, height - lines.size() - 1);
            List<String> messages = new ArrayList<>(LOG);
            for (String message : messages.subList(Math.max(0, messages.size() - logSpace), messages.size())) {
                lines.add(message);
            }
        }
        while (lines.size() < height - 1) {
            lines.add("");
        }
        if (lines.size() >= height) {
            lines = new ArrayList<>(lines.subList(0, Math.max(0, height - 1)));
        }
        String typed = prompt + input;
        int offset = Math.max(0, prompt.length() + cursor - width + 2);
        lines.add(typed.substring(Math.min(offset, typed.length())));
        // Display may insert/remove lines while diffing a resized screen.
        List<AttributedString> attributed = new ArrayList<>(lines.stream()
                .map(AttributedString::fromAnsi)
                .map(line -> line.columnSubSequence(0, Math.max(1, width - 1)))
                .toList());
        display.resize(height, width);
        display.update(attributed, (height - 1) * (width + 1)
                + Math.max(0, Math.min(width - 2, prompt.length() + cursor - offset)));
    }

    static String statusLine(ChessGame snapshot) {
        ChessGame.TeamColor turn = snapshot.getTeamTurn();
        if (snapshot.isInCheckmate(turn)) {
            return "CHECKMATE  /  " + turn + " loses";
        }
        if (snapshot.isInStalemate(turn)) {
            return "STALEMATE  /  Draw";
        }
        if (snapshot.isGameOver()) {
            return "GAME OVER";
        }
        return turn + " to move" + (snapshot.isInCheck(turn) ? "  /  CHECK" : "");
    }

    private static void printPlainBoard() {
        System.out.println(gameName + " | " + statusLine(game.copy()));
        System.out.println(BoardDraw.draw(game.copy(), perspective, selected, new BoardDraw.Layout(4, 1), false));
        System.out.println("w = White, b = Black | K king Q queen R rook B bishop N knight P pawn | + legal");
    }

    private static String safeText(String text) {
        return text.replaceAll("[\\p{Cntrl}\\u007f-\\u009f]", " ");
    }
}
