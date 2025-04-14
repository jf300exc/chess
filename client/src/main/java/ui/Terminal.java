package ui;

import chess.ChessGame;
import chess.ChessPosition;

import java.util.concurrent.*;
import java.util.*;

public class Terminal {
    private static final int FIRST_LINE_NUM = 1;

    // Number of lines for each part of display
    private static final int NOTIFICATION_NUM_LINES = 2;
    private static final int GAME_NUM_LINES = 10;
    private static final int LOG_NUM_LINES = 10;

    // Line Spacing
    private static final int LOG_GAME_SPACING_LINES = 1;
    private static final int INPUT_LOG_SPACING_LINES = 1;
    private static final int INPUT_RETURN_TERMINAL_SPACING_LINES = 0;

    // Line number settings
    private static final int NOTIFICATION_START_LINE = FIRST_LINE_NUM;
    private static final int NOTIFICATION_END_LINE = NOTIFICATION_START_LINE + NOTIFICATION_NUM_LINES - 1;
    private static final int GAME_START_LINE = NOTIFICATION_END_LINE + 1;
    private static final int GAME_END_LINE = GAME_START_LINE + GAME_NUM_LINES - 1;
    private static final int LOG_START_LINE = GAME_END_LINE + 1 + LOG_GAME_SPACING_LINES;
    private static final int LOG_END_LINE = LOG_START_LINE + LOG_NUM_LINES - 1;
    private static final int USER_INPUT_LINE = LOG_END_LINE + 1 + INPUT_LOG_SPACING_LINES;
    private static final int RETURN_TERMINAL_LINE = USER_INPUT_LINE + 1 + INPUT_RETURN_TERMINAL_SPACING_LINES;

    // TIMEOUT settings for connecting to a game
    private static final int TIMEOUT_CHECK_DELAY_MS = 200;
    private static final int TIMEOUT_LIMIT_SECONDS = 5;
    private static final int TIMEOUT_COUNTER_LIMIT = (int) (TIMEOUT_LIMIT_SECONDS / ((float) TIMEOUT_CHECK_DELAY_MS / 1000));
    private static final int TIMEOUT_UPDATE_STATUS_MS = 1000;
    private static final int TIMEOUT_UPDATE_STATUS_INTERVAL = TIMEOUT_UPDATE_STATUS_MS / TIMEOUT_CHECK_DELAY_MS;

    // How long to wait before checking for updates
    private static final int RENDER_DELAY_MS = 300;

    // How long to wait on refresh
    private static final int REFRESH_DELAY_MS = 500;

    // Notifications / Errors
    private static final int NOTIFICATION_DISPLAY_TIME_SECONDS = 15;
    private static final int NOTIFICATION_DISPLAY_TIME_INTERVAL = (int) (NOTIFICATION_DISPLAY_TIME_SECONDS / ((float) RENDER_DELAY_MS / 1000));
    private static final ConcurrentLinkedQueue<String> NOTIFICATIONS = new ConcurrentLinkedQueue<>();
    private static final String[] CURRENT_NOTIFICATION_MESSAGES = new String[2];
    private static final int[] CURRENT_NOTIFICATION_COUNTERS = {0, 0};
    private static volatile boolean updateNotificationMessages = false;

    // Log Messages
    private static final ConcurrentLinkedQueue<String> LOG_MESSAGES = new ConcurrentLinkedQueue<>();
    private static final Deque<String> CURRENT_LOG_MESSAGES = new ArrayDeque<>(); // Only accessed by the render thread
    private static volatile boolean updateLogMessages = false;

    // Game State for Drawing
    private static final int HIGHLIGHT_DISPLAY_TIME_SECONDS = 10;
    private static final int HIGHLIGHT_DISPLAY_TIME_INTERVAL = (int) (HIGHLIGHT_DISPLAY_TIME_SECONDS / ((float) RENDER_DELAY_MS / 1000));
    private static String currentGameName;
    private static ChessGame.TeamColor currentTeamColor = null;
    private static volatile ChessGame currentGameState = null;
    private static volatile boolean gameChangedFlag = false;
    private static volatile ChessPosition highLightStartPosition = null;
    private static int currentHighLightCounter = 0;
    private static final Object GAME_STATE_LOCK = new Object();

    // IO
    private static final Scanner SCANNER = new Scanner(System.in);
    private static volatile boolean running = false;
    private static volatile boolean renderThread = false;
    private static volatile boolean readyForInput = false;

    public static void start(String teamColor) {
        setPlayerColor(teamColor);
        LOG_MESSAGES.clear();
        CURRENT_LOG_MESSAGES.clear();
        NOTIFICATIONS.clear();
        CURRENT_NOTIFICATION_MESSAGES[0] = null;
        CURRENT_NOTIFICATION_COUNTERS[0] = 0;
        CURRENT_NOTIFICATION_MESSAGES[1] = null;
        CURRENT_NOTIFICATION_COUNTERS[1] = 0;
        synchronized (GAME_STATE_LOCK) {
            currentGameState = null;
        }
        new Thread(Terminal::waitForGameData, "Pre-Game Thread").start();
    }

    public static void refresh() {
        running = false;
        readyForInput = false;
        while (renderThread) {
            Thread.onSpinWait();
        }
        ChessGame game;
        synchronized (GAME_STATE_LOCK) {
            game = currentGameState;
            currentGameState = null;
        }
        new Thread(Terminal::waitForGameData, "Pre-Game Thread").start();
        try {Thread.sleep(REFRESH_DELAY_MS); } catch (InterruptedException ignored) { }
        setChessGame(game, currentGameName);
    }

    private static void waitForGameData() {
        int waitTime = 0;
        System.out.print("Waiting for game data... ");
        while(currentGameState == null) {
            try { Thread.sleep(TIMEOUT_CHECK_DELAY_MS); } catch (InterruptedException ignored) {}
            if (++waitTime > TIMEOUT_COUNTER_LIMIT) {
                System.out.println("Timed out waiting for game data");
                return;
            } else if (waitTime % TIMEOUT_UPDATE_STATUS_INTERVAL == 0) {
                System.out.print(". ");
            }
        }
        System.out.println("Received game data");
        System.out.print(EscapeSequences.ERASE_SCROLL_BACK);
        System.out.print(EscapeSequences.ERASE_SCREEN);
        running = true;
        startInterface();
    }

    private static void startInterface() {
        // Rendering thread for the terminal
        new Thread(() -> {
            renderThread = true;
            addLogMessage("Joining Game: " + currentGameName);
            while (running) {
                render();
                try { Thread.sleep(RENDER_DELAY_MS); } catch (InterruptedException ignored) {}
            }
            renderThread = false;
        }, "Renderer").start();

        readyForInput = true; // Don't prompt the user until starting the interface
    }

    public static void stop() {
        running = false;
        readyForInput = false;

        // Return cursor
        StringBuilder sb = new StringBuilder();
        returnCursor(sb);

        while (renderThread) {
            Thread.onSpinWait();
        }
        System.out.print(sb);
    }

    public static void addNotification(String notification) {
        NOTIFICATIONS.add(notification);
        updateNotificationMessages = true;
    }

    public static void addLogMessage(String message) {
        LOG_MESSAGES.add(message);
        updateLogMessages = true;
    }

    public static void setChessGame(ChessGame chessGame, String gameName) {
        synchronized (GAME_STATE_LOCK) {
            currentGameState = chessGame;
            currentGameName = gameName;
            gameChangedFlag = true;
        }
    }

    public static ChessGame getChessGame() {
        synchronized (GAME_STATE_LOCK) {
            if (currentGameState == null) {
                return null;
            } else {
                return currentGameState.copy();
            }
        }
    }

    public static void drawHighlights(ChessPosition position) {
        highLightStartPosition = position;
        gameChangedFlag = true;
        currentHighLightCounter = 0;
    }

    public static void setPlayerColor(String playerColor) {
        if (playerColor.equals("WHITE")) {
            setPlayerColor(ChessGame.TeamColor.WHITE);
        } else {
            setPlayerColor(ChessGame.TeamColor.BLACK);
        }
    }

    public static void setPlayerColor(ChessGame.TeamColor color) {
        currentTeamColor = color;
    }

    private static void render() {
        boolean any = false;
        StringBuilder sb = new StringBuilder();
        // Save cursor position
        sb.append(EscapeSequences.SAVE_CURSOR_POSITION);

        // Notifications
        if (updateNotificationMessages) {
            updateNotificationMessages = false;
            any = true;
            renderNotifications(sb);
        }

        // Game
        if (gameChangedFlag) {
            gameChangedFlag = false;
            any = true;
            renderChessGame(sb);
        }

        // Log Messages
        if (updateLogMessages) {
            updateLogMessages = false;
            any = true;
            renderLog(sb);
        }

        // Tick notifications
        tickNotificationMessageCounts();
        tickGameHighlightCount();

        // Restore cursor position
        if (any) {
            sb.append(EscapeSequences.RETURN_TO_SAVED_CURSOR_POSITION);
            System.out.print(sb); // Output in single call to terminal
            System.out.flush();
        }
    }

    public static boolean notReadyForInput() {
        return !readyForInput;
    }

    public static String getInput(String prompt) {
        StringBuilder sb = new StringBuilder();
        addSwitchToInputLine(sb);
        addEraseCurrentLine(sb);
        sb.append(prompt);
        System.out.print(sb);
        String input = SCANNER.nextLine().trim();
        addLogMessage(">>> " + input);
        return input;
    }

    private static void returnCursor(StringBuilder sb) {
        addSwitchLine(sb, RETURN_TERMINAL_LINE);
    }

    private static void addSwitchLine(StringBuilder sb, int line) {
        sb.append(EscapeSequences.moveCursorToLocation(0, line));
    }

    private static void addEraseCurrentLine(StringBuilder sb) {
        sb.append(EscapeSequences.ERASE_LINE);
    }

    private static void addSwitchEraseLine(StringBuilder sb, int line) {
        addSwitchLine(sb, line);
        addEraseCurrentLine(sb);
    }

    private static void addEraseLines(StringBuilder sb, int startLine, int stopLine) {
        for (int i = startLine; i <= stopLine; i++) {
            addSwitchEraseLine(sb, i);
        }
    }

    private static void addSwitchToInputLine(StringBuilder sb) {
        addSwitchLine(sb, USER_INPUT_LINE);
    }

    private static void renderLog(StringBuilder sb) {
        // Look for all messages
        String logMessage = LOG_MESSAGES.poll();
        while (logMessage != null) {
            if (CURRENT_LOG_MESSAGES.size() >= 10) {
                CURRENT_LOG_MESSAGES.poll();
            }
            CURRENT_LOG_MESSAGES.add(logMessage);
            logMessage = LOG_MESSAGES.poll();
        }
        displayLogMessages(sb);
    }

    private static void displayLogMessages(StringBuilder sb) {
        // Cut off the oldest log messages if needed
        addEraseLines(sb, LOG_START_LINE, LOG_END_LINE);
        int messageLineIndexStart = LOG_END_LINE - (CURRENT_LOG_MESSAGES.size() - 1);
        int lineInc = 0;
        for (String logLine : CURRENT_LOG_MESSAGES) {
            addSwitchLine(sb, messageLineIndexStart + lineInc++);
            sb.append(logLine);
        }
    }

    private static void renderNotifications(StringBuilder sb) {
        // Erase Notifications
        addEraseLines(sb, NOTIFICATION_START_LINE, NOTIFICATION_END_LINE);
        addSwitchLine(sb, NOTIFICATION_START_LINE);

        // Get next two notifications
        prepareNotification(NOTIFICATIONS.poll());
        prepareNotification(NOTIFICATIONS.poll());

        if (CURRENT_NOTIFICATION_MESSAGES[0] != null) {
            sb.append(CURRENT_NOTIFICATION_MESSAGES[0]).append("\n");
            if (CURRENT_NOTIFICATION_MESSAGES[1] != null) {
                sb.append(CURRENT_NOTIFICATION_MESSAGES[1]);
            }
        }
    }

    private static void prepareNotification(String notificationMessage) {
        if (notificationMessage != null) {
            shiftNotifications(notificationMessage);
        }
    }

    private static void shiftNotifications(String notification) {
        if (CURRENT_NOTIFICATION_MESSAGES[0] == null) {
            CURRENT_NOTIFICATION_MESSAGES[0] = notification;
            CURRENT_NOTIFICATION_COUNTERS[0] = 0;
        } else {
            if (CURRENT_NOTIFICATION_MESSAGES[1] != null) {
                CURRENT_NOTIFICATION_MESSAGES[0] = CURRENT_NOTIFICATION_MESSAGES[1];
                CURRENT_NOTIFICATION_COUNTERS[0] = CURRENT_NOTIFICATION_COUNTERS[1];
            }
            CURRENT_NOTIFICATION_MESSAGES[1] = notification;
            CURRENT_NOTIFICATION_COUNTERS[1] = 0;
        }
    }

    private static void tickNotificationMessageCounts() {
        if (CURRENT_NOTIFICATION_MESSAGES[0] != null) {
            CURRENT_NOTIFICATION_COUNTERS[0]++;
        }
        if (CURRENT_NOTIFICATION_MESSAGES[1] != null) {
            CURRENT_NOTIFICATION_COUNTERS[1]++;
        }
        removeOldNotifications();
    }

    private static void removeOldNotifications() {
        while (CURRENT_NOTIFICATION_MESSAGES[0] != null && CURRENT_NOTIFICATION_COUNTERS[0] >= NOTIFICATION_DISPLAY_TIME_INTERVAL) {
            CURRENT_NOTIFICATION_MESSAGES[0] = CURRENT_NOTIFICATION_MESSAGES[1];
            CURRENT_NOTIFICATION_COUNTERS[0] = CURRENT_NOTIFICATION_COUNTERS[1];
            CURRENT_NOTIFICATION_MESSAGES[1] = null;
            updateNotificationMessages = true;
        }
    }

    private static void renderChessGame(StringBuilder sb) {
        addEraseLines(sb, GAME_START_LINE, GAME_END_LINE);
        addSwitchLine(sb, GAME_START_LINE);
        if (highLightStartPosition != null) {
            sb.append(BoardDraw.drawBoardWithValidMoves(currentGameState, currentTeamColor, highLightStartPosition));
        } else {
            sb.append(BoardDraw.drawBoard(currentGameState, currentTeamColor));
        }
    }

    private static void tickGameHighlightCount() {
        if (highLightStartPosition != null) {
            if (currentHighLightCounter >= HIGHLIGHT_DISPLAY_TIME_INTERVAL) {
                highLightStartPosition = null;
                gameChangedFlag = true;
            }
            currentHighLightCounter++;
        }
    }
}
