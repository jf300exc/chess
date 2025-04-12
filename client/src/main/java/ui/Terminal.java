package ui;

import chess.ChessGame;

import java.util.concurrent.*;
import java.util.*;

public class Terminal {
    private static final int FIRST_LINE_NUM = 1;
    private static final int NOTIFICATION_NUM_LINES = 2;
    private static final int GAME_NUM_LINES = 10;
    private static final int LOG_NUM_LINES = 10;
    private static final int LOG_GAME_SPACING_LINES = 1;
    private static final int INPUT_LOG_SPACING_LINES = 1;
    private static final int INPUT_RETURN_TERMINAL_SPACING_LINES = 0;

    private static final int NOTIFICATION_START_LINE = FIRST_LINE_NUM;
    private static final int NOTIFICATION_END_LINE = NOTIFICATION_START_LINE + NOTIFICATION_NUM_LINES - 1;
    private static final int GAME_START_LINE = NOTIFICATION_END_LINE + 1;
    private static final int GAME_END_LINE = GAME_START_LINE + GAME_NUM_LINES - 1;
    private static final int LOG_START_LINE = GAME_END_LINE + 1 + LOG_GAME_SPACING_LINES;
    private static final int LOG_END_LINE = LOG_START_LINE + LOG_NUM_LINES - 1;
    private static final int USER_INPUT_LINE = LOG_END_LINE + 1 + INPUT_LOG_SPACING_LINES;
    private static final int RETURN_TERMINAL_LINE = USER_INPUT_LINE + 1 + INPUT_RETURN_TERMINAL_SPACING_LINES;

    private static final int TIMEOUT_CHECK_DELAY_MS = 200;
    private static final int TIMEOUT_LIMIT_SECONDS = 5;
    private static final int TIMEOUT_COUNTER_LIMIT = (int) (TIMEOUT_LIMIT_SECONDS / ((float) TIMEOUT_CHECK_DELAY_MS / 1000));
    private static final int TIMEOUT_UPDATE_STATUS_MS = 1000;
    private static final int TIMEOUT_UPDATE_STATUS_INTERVAL = TIMEOUT_UPDATE_STATUS_MS / TIMEOUT_CHECK_DELAY_MS;

    private static final int RENDER_DELAY_MS = 300;

    private static final ConcurrentLinkedQueue<String> notifications = new ConcurrentLinkedQueue<>();
    private static final String[] currentNotificationMessages = new String[2];
    private static final int[] currentNotificationCounters = {0, 0};

    private static final ConcurrentLinkedQueue<String> logMessages = new ConcurrentLinkedQueue<>();
    private static final Deque<String> currentLogMessages = new ArrayDeque<>(); // Only accessed by the render thread

    private static ChessGame.TeamColor currentTeamColor = null;
    private static volatile ChessGame currentGameState = null;
    private static volatile boolean gameChangedFlag = false;

    private static final Object gameStateLock = new Object();

    private static final Scanner scanner = new Scanner(System.in);
    private static volatile boolean running = false;
    private static volatile boolean renderThread = false;
    private static volatile boolean readyForInput = false;

    public static void start(String teamColor) {
        setPlayerColor(teamColor);
        logMessages.clear();
        currentLogMessages.clear();
        notifications.clear();
        currentNotificationMessages[0] = null;
        currentNotificationCounters[0] = 0;
        currentNotificationMessages[1] = null;
        currentNotificationCounters[1] = 0;
        synchronized (gameStateLock) {
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
        synchronized (gameStateLock) {
            game = currentGameState;
            currentGameState = null;
        }
        new Thread(Terminal::waitForGameData, "Pre-Game Thread").start();
        try {Thread.sleep(500); } catch (InterruptedException ignored) { }
        setChessGame(game);
    }

    private static void waitForGameData() {
        int waitTime = 0;
        System.out.print("Waiting for game data... ");
        while(currentGameState == null) {
//            Thread.onSpinWait();
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
        notifications.add(notification);
    }

    public static void addLogMessage(String message) {
        logMessages.add(message);
    }

    public static void setChessGame(ChessGame chessGame) {
        synchronized (gameStateLock) {
            currentGameState = chessGame;
            gameChangedFlag = true;
        }
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
        StringBuilder sb = new StringBuilder();

        // Save cursor position
        sb.append(EscapeSequences.SAVE_CURSOR_POSITION);

        // Notifications
        renderNotifications(sb);

        // Game
        renderChessGame(sb);

        // Log Messages
        renderLog(sb);

        // Restore cursor position
        sb.append("\n" + EscapeSequences.RETURN_TO_SAVED_CURSOR_POSITION);

        // Output in single call to terminal
        System.out.print(sb);
        System.out.flush();
    }

    public static boolean notReadyForInput() {
        return !readyForInput;
    }

    public static String getInput() {
        StringBuilder sb = new StringBuilder();
        addSwitchToInputLine(sb);
        addEraseCurrentLine(sb);
        addPrompt(sb);
        System.out.print(sb);
        String input = scanner.nextLine().trim();
        addLogMessage(input);
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

    private static void addPrompt(StringBuilder sb) {
        sb.append(">>> ");
    }

    private static void renderLog(StringBuilder sb) {
        // Look for 5 messages at once
        boolean any = false;
        for (int i = 0; i < 10; i++) {
            String logMessage = logMessages.poll();
            if (logMessage != null) {
                if (currentLogMessages.size() >= 10) {
                    currentLogMessages.poll();
                }
                currentLogMessages.add(logMessage);
                any = true;
            }
        }
        if (any) {
            displayLogMessages(sb);
        }
    }

    private static void displayLogMessages(StringBuilder sb) {
        // Cut off the oldest log messages if needed
        addEraseLines(sb, LOG_START_LINE, LOG_END_LINE);
        int lineInc = 0;
        for (String logLine : currentLogMessages) {
            addSwitchLine(sb, LOG_START_LINE + lineInc++);
            sb.append(logLine);
        }
    }

    private static void renderNotifications(StringBuilder sb) {
        // Erase Notifications
        addEraseLines(sb, NOTIFICATION_START_LINE, NOTIFICATION_END_LINE);
        addSwitchLine(sb, NOTIFICATION_START_LINE);

        // Get next two notifications
        String notification = notifications.poll();
        prepareNotifications(notification);
        notification = notifications.poll();
        prepareNotifications(notification);

        if (currentNotificationMessages[0] != null) {
            sb.append(currentNotificationMessages[0]).append("\n");
        } else {
            sb.append("Incr: ").append(currentNotificationCounters[0]).append("\n");
        }
        if (currentNotificationMessages[1] != null) {
            sb.append(currentNotificationMessages[1]).append("\n");
        } else {
            sb.append("Incr: ").append(currentNotificationCounters[1]).append("\n");
        }
        currentNotificationCounters[0]++;
        currentNotificationCounters[1]++;
    }

    private static void prepareNotifications(String notificationMessage) {
        if (notificationMessage != null) {
            shiftNotifications(notificationMessage);
        }
        removeOldNotifications();
    }

    private static void shiftNotifications(String notification) {
        if (currentNotificationMessages[1] != null) {
            currentNotificationMessages[0] = currentNotificationMessages[1];
            currentNotificationMessages[1] = notification;
            currentNotificationCounters[1] = 0;
        } else {
            currentNotificationMessages[0] = notification;
            currentNotificationCounters[0] = 0;
        }
    }

    private static void removeOldNotifications() {
        while (currentNotificationCounters[0] >= 25 && currentNotificationMessages[0] != null) {
            currentNotificationMessages[0] = currentNotificationMessages[1];
            currentNotificationCounters[0] = currentNotificationCounters[1];
            currentNotificationMessages[1] = null;
        }
    }

    private static void renderChessGame(StringBuilder sb) {
        if (gameChangedFlag) {
            gameChangedFlag = false;
            addEraseLines(sb, GAME_START_LINE, GAME_END_LINE);
            addSwitchLine(sb, GAME_START_LINE);
            sb.append(BoardDraw.drawBoard(currentGameState, currentTeamColor));
        }
    }
}
