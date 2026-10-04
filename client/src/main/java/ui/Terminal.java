package ui;

import chess.ChessGame;
import chess.ChessPosition;
import org.jline.terminal.Attributes;
import org.jline.terminal.Terminal.MouseTracking;
import org.jline.utils.AttributedString;
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
import java.awt.image.BufferedImage;
import java.util.Objects;

/** Gameplay screen and input, rendered by the input thread from synchronized snapshots. */
public final class Terminal {
    private static final Object LOCK = new Object();
    private static final int BOARD_START_ROW = 2;
    private record LogEntry(String text, String color) { }
    private static final Deque<LogEntry> LOG = new ArrayDeque<>();
    private static ChessGame game;
    private static String gameName;
    private static ChessGame.TeamColor perspective = ChessGame.TeamColor.WHITE;
    private static ChessPosition selected;
    private static long revision;
    private static CliConsole console;
    private static org.jline.terminal.Terminal terminal;
    private static Attributes savedAttributes;
    private static TerminalScreen display;
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
    private static TerminalGraphics.CellPixels cellPixels;
    private static final Deque<Integer> pendingInput = new ArrayDeque<>();
    private static long imageRevision = -1;
    private static BoardDraw.Layout imageLayout;
    private static boolean imageVisible;
    private static BufferedImage boardImage;
    private static long boardRevision;
    private static ChessGame.TeamColor playerTeam;
    private static String gameEnding;
    private static boolean restoreSixelDisplayMode;

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
            cellPixels = null;
            pendingInput.clear();
            imageRevision = -1;
            imageLayout = null;
            imageVisible = false;
            boardImage = null;
            boardRevision++;
            playerTeam = null;
            gameEnding = null;
            restoreSixelDisplayMode = false;
            active = true;
            revision++;
        }
        if (terminal != null) {
            savedAttributes = terminal.enterRawMode();
            Attributes rawAttributes = terminal.getAttributes();
            rawAttributes.setLocalFlag(Attributes.LocalFlag.ISIG, false);
            terminal.setAttributes(rawAttributes);
            if (input.allowGraphics()) {
                try {
                    TerminalGraphics.Probe probe = TerminalGraphics.probe(terminal);
                    cellPixels = probe.pixels();
                    pendingInput.addAll(probe.pendingInput());
                    restoreSixelDisplayMode = probe.restoreDisplayMode();
                    if (restoreSixelDisplayMode) {
                        terminal.writer().print("\u001b[?80l");
                        terminal.flush();
                    }
                } catch (IOException | RuntimeException e) {
                    LOG.add(new LogEntry("Terminal graphics unavailable; using chess symbols.", "33"));
                }
            }
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
            display = new TerminalScreen();
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
                if (restoreSixelDisplayMode) {
                    terminal.writer().print("\u001b[?80h");
                }
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
        imageVisible = false;
        boardImage = null;
        cellPixels = null;
        pendingInput.clear();
        restoreSixelDisplayMode = false;
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
                terminal.puts(Capability.clear_screen);
                display.clear();
                boardImage = null;
                imageRevision = -1;
            } else if (game != null) {
                printPlainBoard();
            }
        }
    }

    public static void addNotification(String message) {
        addLogMessage(message, "36");
    }

    public static void addError(String message) {
        addLogMessage(message, "31");
    }

    public static void addLogMessage(String message) {
        String warning = message == null ? "" : message.toLowerCase(java.util.Locale.ROOT);
        String color = warning.startsWith("invalid") || warning.startsWith("illegal") || warning.startsWith("cannot")
                || warning.startsWith("unknown") ? "33" : "0";
        addLogMessage(message, color);
    }

    private static void addLogMessage(String message, String color) {
        if (message == null) {
            return;
        }
        synchronized (LOCK) {
            for (String line : message.split("\n")) {
                LOG.addLast(new LogEntry(safeText(line), color));
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
            if (game == null || !game.isGameOver()) {
                gameEnding = null;
            }
            if (name != null) {
                gameName = safeText(name);
            }
            selected = null;
            revision++;
            boardRevision++;
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
            if (Objects.equals(selected, position)) {
                return;
            }
            selected = position;
            revision++;
            boardRevision++;
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
            boardRevision++;
            if (active && terminal == null && game != null) {
                printPlainBoard();
            }
        }
    }

    public static void setPlayerTeam(ChessGame.TeamColor team) {
        synchronized (LOCK) {
            playerTeam = team;
            revision++;
        }
    }

    public static void endGame(String reason) {
        synchronized (LOCK) {
            if (game != null) {
                game.setGameOver(true);
                gameEnding = reason == null ? null : safeText(reason);
                selected = null;
                revision++;
                boardRevision++;
                if (active && terminal == null) {
                    printPlainBoard();
                }
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
        boolean queuedSequence = false;
        long renderedRevision = -1;
        int previousWidth = renderedWidth;
        int previousHeight = renderedHeight;
        boolean inputChanged = true;
        long renderedAnimation = -1;
        try {
            for (;;) {
                if (!connected.getAsBoolean()) {
                    throw new IOException("The live game connection is closed.");
                }
                int width = Math.max(1, terminal.getWidth());
                int height = Math.max(1, terminal.getHeight());
                synchronized (LOCK) {
                    GameStatus.Status status = GameStatus.describe(game, playerTeam, gameEnding);
                    long animation = console.allowAnimation() && helpLines == null && status.waiting()
                            ? System.nanoTime() / 500_000_000L : -1;
                    if (inputChanged || revision != renderedRevision || width != previousWidth || height != previousHeight
                            || animation != renderedAnimation) {
                        if (width != previousWidth || height != previousHeight) {
                            queuedSequence |= decoder.sequencePending();
                            display.clear();
                            if (previousWidth > 0) {
                                terminal.puts(Capability.clear_screen);
                            }
                            boardImage = null;
                            imageRevision = -1;
                            if (cellPixels != null && previousWidth > 0) {
                                // Re-query pixels after resize, including terminal font-size changes.
                                TerminalGraphics.Probe probe = TerminalGraphics.probe(terminal);
                                cellPixels = probe.pixels();
                                pendingInput.addAll(probe.pendingInput());
                            }
                        }
                        render(prompt, buffer.toString(), cursor, width, height, animation);
                        renderedAnimation = animation;
                        renderedRevision = revision;
                        previousWidth = width;
                        previousHeight = height;
                        renderedWidth = width;
                        renderedHeight = height;
                        inputChanged = false;
                    }
                }
                boolean queuedInput = !pendingInput.isEmpty();
                int character = queuedInput ? pendingInput.removeFirst() : terminal.reader().read(100);
                queuedSequence |= queuedInput;
                TerminalEventDecoder.Event event = character == NonBlockingReader.READ_EXPIRED
                        ? decoder.idle() : decoder.feed(character);
                boolean queuedEvent = queuedSequence;
                if (!decoder.sequencePending()) {
                    queuedSequence = false;
                }
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
                        // Input captured while probing predates the newly painted hit map.
                        if (queuedEvent || !mouseEnabled || !acceptClicks) {
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

    private static void render(String prompt, String input, int cursor, int width, int height, long animation) {
        layout = cellPixels == null ? new BoardDraw.Layout(3, 1) : BoardDraw.Layout.forSize(width, height);
        List<String> lines = new ArrayList<>();
        if (helpLines != null) {
            lines.addAll(helpLines);
            boardVisible = false;
        } else {
            lines.add(GameStatus.paint("CHESS  /  " + gameName, "1", console.allowColor()));
            GameStatus.Status status = GameStatus.describe(game, playerTeam, gameEnding);
            String indicator = animation < 0 ? "" : "  " + "|/-\\".charAt((int) (animation % 4));
            lines.add(GameStatus.paint(status.text() + indicator, status.color(), console.allowColor())
                    + "  |  View: " + perspective);
            boardVisible = game != null && width >= layout.width() && height >= layout.height() + 6;
            if (boardVisible) {
                String board = cellPixels == null ? BoardDraw.draw(game.copy(), perspective, selected, layout,
                        console.allowColor(), console.pieceSymbols()) : BoardDraw.imageFrame(layout, perspective);
                lines.addAll(Arrays.asList(board.split("\n")));
            } else {
                lines.add("Board needs 30 columns x 16 rows. Resize or use text commands.");
            }
            lines.add(cellPixels == null ? console.pieceSymbols().forRendering(console.allowColor()).legend()
                    : "White: light pieces | Black: dark pieces | enlarged board");
            lines.add(game != null && (game.isGameOver() || game.isInCheckmate(game.getTeamTurn())
                    || game.isInStalemate(game.getTeamTurn())) ? "Final board | flip to inspect | help | leave to return to lobby"
                    : mouseEnabled ? "Click piece -> destination | right-click/Esc cancels | help"
                    : "Text controls | move e2e4 | highlight e2 | flip | help");
            int logSpace = Math.max(0, height - lines.size() - 1);
            List<LogEntry> messages = new ArrayList<>(LOG);
            for (LogEntry message : messages.subList(Math.max(0, messages.size() - logSpace), messages.size())) {
                lines.add(GameStatus.paint(message.text(), message.color(), console.allowColor()));
            }
        }
        boolean showImage = boardVisible && cellPixels != null && helpLines == null;
        boolean repaintImage = showImage && (imageRevision != boardRevision || !layout.equals(imageLayout));
        List<BoardDamage.Patch> patches = List.of();
        if (repaintImage) {
            try {
                BufferedImage next = BoardImage.render(game.copy(), perspective, selected,
                        layout, cellPixels, console.pieceSymbols());
                patches = BoardDamage.between(layout.equals(imageLayout) ? boardImage : null, next);
                boardImage = next;
                imageRevision = boardRevision;
                imageLayout = layout;
            } catch (RuntimeException | LinkageError e) {
                cellPixels = null;
                boardImage = null;
                imageRevision = -1;
                LOG.add(new LogEntry("Board graphics unavailable; using chess symbols.", "33"));
                render(prompt, input, cursor, width, height, animation);
                return;
            }
        }
        if (imageVisible && !showImage) {
            // Erase old image layers as well as text, including help and resized layouts.
            terminal.puts(Capability.clear_screen);
            display.clear();
            boardImage = null;
            imageRevision = -1;
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
        List<AttributedString> attributed = new ArrayList<>(lines.stream()
                .map(AttributedString::fromAnsi)
                .map(line -> line.columnSubSequence(0, Math.max(1, width - 1)))
                .toList());
        display.update(terminal, attributed, width, height - 1,
                Math.max(0, Math.min(width - 2, prompt.length() + cursor - offset)),
                showImage ? new TerminalScreen.ImageArea(BOARD_START_ROW + 1, 3,
                        8 * layout.cellHeight(), 8 * layout.cellWidth()) : null);
        if (showImage && !patches.isEmpty()) {
            terminal.writer().print("\u001b7");
            for (BoardDamage.Patch patch : patches) {
                terminal.puts(Capability.cursor_address, BOARD_START_ROW + 1 + patch.row() * layout.cellHeight(),
                        3 + patch.column() * layout.cellWidth());
                terminal.writer().print(SixelEncoder.encode(patch.image()));
            }
            terminal.writer().print("\u001b8");
        }
        terminal.flush();
        imageVisible = showImage;
    }

    static String statusLine(ChessGame snapshot) {
        return GameStatus.describe(snapshot, null, null).text();
    }

    private static void printPlainBoard() {
        System.out.println(gameName + " | " + GameStatus.describe(game, playerTeam, gameEnding).text());
        PieceSymbols symbols = console.pieceSymbols().forRendering(false);
        System.out.println(BoardDraw.draw(game.copy(), perspective, selected, new BoardDraw.Layout(3, 1), false, symbols));
        System.out.println(symbols.legend());
    }

    private static String safeText(String text) {
        return text.replaceAll("[\\p{Cntrl}\\u007f-\\u009f]", " ");
    }
}
