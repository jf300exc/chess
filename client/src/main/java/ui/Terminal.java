package ui;

import chess.ChessGame;

import java.sql.Array;
import java.util.concurrent.*;
import java.util.*;

public class Terminal {
    private static final ConcurrentLinkedQueue<String> notifications = new ConcurrentLinkedQueue<>();
    private static final String[] currentNotificationMessages = new String[2];
    private static final int[] currentNotificationCounters = {0, 0};
    private static final ConcurrentLinkedQueue<String> logMessages = new ConcurrentLinkedQueue<>();
    private static final Deque<String> currentLogMessages = new ArrayDeque<>(); // Only accessed by the render thread
    private static final int lastLogMessageIndex = 0;
//    private static final int currentLogLength = 0;
    private static ChessGame.TeamColor currentTeamColor = null;
    private static final Object boardLock = new Object();
    private static int deleteI = 0;

    private static final Scanner scanner = new Scanner(System.in);
    private static volatile ChessGame currentGameState = null;
    private static volatile boolean running = false;
    private static volatile boolean renderThread = false;
    private static volatile boolean inputThread = false;

    public static void start(String teamColor) {
        setPlayerColor(teamColor);
        new Thread(Terminal::waitForGameData, "Pre-Game Thread").start();
    }

    public static void waitForGameData() {
        int waitTime = 0;
        System.out.print("Waiting for game data... ");
        while(currentGameState == null) {
            try { Thread.sleep(200); } catch (InterruptedException ignored) {}
            if (++waitTime > 100) {
                System.out.println("Timed out waiting for game data");
                return;
            } else if (waitTime % 5 == 0) {
                System.out.print(". ");
            }
        }
        System.out.println("Received game data");
        System.out.print(EscapeSequences.ERASE_SCROLL_BACK);
        System.out.print(EscapeSequences.ERASE_SCREEN);
        System.out.println(currentTeamColor);
        running = true;
        startInterface();
    }

    private static void startInterface() {
        // Rendering thread for the terminal
        new Thread(() -> {
            renderThread = true;
            while (running) {
                render();
                try { Thread.sleep(300); } catch (InterruptedException ignored) {}
            }
            renderThread = false;
        }, "Renderer").start();
        // Thread to get user input
        new Thread(() -> {
            inputThread = true;
            while (running) {
                getInput();
            }
            inputThread = false;
        }, "Terminal-Input").start();
    }

    public static void stop() {
        running = false;

        // Return cursor
        StringBuilder sb = new StringBuilder();
        returnCursor(sb);

        while (renderThread || inputThread) {
            Thread.onSpinWait();
        }
        System.out.println(sb);
    }

    public static void addNotification(String notification) {
        notifications.add(notification);
    }

    public static void addLogMessage(String message) {
        logMessages.add(message);
    }

    public static void setChessGame(ChessGame chessGame) {
        System.out.println("Setting chess game");
        System.out.println(chessGame);
        synchronized (boardLock) {
            currentGameState = chessGame;
        }
        System.out.println("Set Game");
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
        sb.append("\033[s");

        // Erase Notification and GameBoard
        addEraseLines(sb, 0, 11);
        addSwitchLine(sb, 0);

        // Notifications
        for (int i = 0; i < 2; i++) {
//            sb.append("Line1").append(i).append('\n');
//            sb.append("Line2").append(i++).append('\n');
            String notification = notifications.poll();
            prepareNotifications(notification);
        }
        addNotifications(sb);

        // Game
        sb.append(BoardDraw.drawBoard(currentGameState, currentTeamColor));

        addLogLines(sb);

        // Restore cursor position
        sb.append("\n\033[u");

        // Output in single call to terminal
        System.out.print(sb);
        System.out.flush();
    }

    private static void getInput() {
        StringBuilder sb = new StringBuilder();
        addSwitchToInputLine(sb);
        addPrompt(sb);
        System.out.print(sb.toString());
        String input = scanner.nextLine().trim();
        System.out.print("Got input: " + input);
        GamePlay.addUserInput(input);
        try { Thread.sleep(500); } catch (InterruptedException ignored) {}

        // TODO: Handle input

        sb = new StringBuilder();
        addEraseLine(sb);
        addSwitchEraseLine(sb, 15);
        addSwitchEraseLine(sb, 14);
        System.out.print(sb);
        System.out.flush();
    }

    private static void returnCursor(StringBuilder sb) {
        addSwitchLine(sb, 26);
        sb.append("\n");
    }

    private static void addSwitchLine(StringBuilder sb, int line) {
        sb.append(EscapeSequences.moveCursorToLocation(0, line));
    }

    private static void addEraseLine(StringBuilder sb) {
        sb.append(EscapeSequences.ERASE_LINE);
    }

    private static void addSwitchEraseLine(StringBuilder sb, int line) {
        addSwitchLine(sb, line);
        addEraseLine(sb);
    }

    private static void addEraseLines(StringBuilder sb, int startLine, int stopLine) {
        for (int i = startLine; i <= stopLine; i++) {
            addSwitchEraseLine(sb, i);
        }
    }

    private static void addSwitchToInputLine(StringBuilder sb) {
        addSwitchLine(sb, 14);
    }

    private static void addPrompt(StringBuilder sb) {
        sb.append(">>> ");
    }

    private static void addLogLines(StringBuilder sb) {
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
        int startLine = 16;
        int endLine = 26;
        addEraseLines(sb, startLine, endLine);
        int lineInc = 0;
        for (String logLine : currentLogMessages) {
            addSwitchLine(sb, startLine + lineInc++);
            sb.append(logLine);
        }
    }

    private static void addNotifications(StringBuilder sb) {
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
}
