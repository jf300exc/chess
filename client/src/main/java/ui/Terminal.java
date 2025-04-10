package ui;

import chess.ChessBoard;
import chess.ChessGame;
import ui.EscapeSequences;

import java.util.concurrent.*;
import java.util.*;

public class Terminal {
    private static final ConcurrentLinkedQueue<String> notifications = new ConcurrentLinkedQueue<>();
    private static ChessGame currentGameState = null;
    private static ChessGame.TeamColor currentTeamColor = null;
    private static final Object boardLock = new Object();
    private static final Object teamColorLock = new Object();
    private static int i = 0;

    private static final Scanner scanner = new Scanner(System.in);
    private static volatile boolean running = false;

    public static void start(String teamColor) {
        setPlayerColor(teamColor);
        new Thread(Terminal::waitForGameData, "Pre-Game Thread").start();
    }

    private static void startInterface() {
        // Rendering thread for the terminal
        new Thread(() -> {
            while (running) {
                render();
                try { Thread.sleep(200); } catch (InterruptedException ignored) {}
            }
        }, "Renderer").start();
        new Thread(() -> {
            while (running) {
                getInput();
            }
        }, "Terminal-Input").start();
    }

    public static void stop() {
        running = false;
    }

    public static void addNotification(String notification) {
        notifications.add(notification);
    }

    public static void waitForGameData() {
        System.out.print("Waiting for game data...");
        while(currentGameState == null) {
            try { Thread.sleep(200); } catch (InterruptedException ignored) {}
        }
//        StringBuilder sb = new StringBuilder();
//        addEraseLine(sb);
//        System.out.print(sb.toString());
        System.out.println(currentTeamColor);
        running = true;
        startInterface();
    }

    public static void setChessGame(ChessGame chessGame) {
//        synchronized (boardLock) {
            currentGameState = chessGame;
//        }
    }

    public static void setPlayerColor(String playerColor) {
        if (playerColor.equals("WHITE")) {
            setPlayerColor(ChessGame.TeamColor.WHITE);
        } else {
            setPlayerColor(ChessGame.TeamColor.BLACK);
        }
    }

    public static void setPlayerColor(ChessGame.TeamColor color) {
        synchronized (teamColorLock) {
            currentTeamColor = color;
        }
    }

    private static void render() {
        StringBuilder sb = new StringBuilder();

        // Save cursor position
        sb.append("\033[s");

        // Erase Notification and GameBoard
        addEraseLines(sb, 0, 11);
        addSwitchLine(sb, 0);

        // Notifications
        sb.append("Line1").append(i).append('\n');
        sb.append("Line2").append(i++).append('\n');

        // Game
        sb.append(BoardDraw.drawBoard(currentGameState, currentTeamColor));

        // Restore cursor position
        sb.append("\n\033[u");

        // Output in single call to terminal
        System.out.print(sb.toString());
        System.out.flush();
    }

    private static void getInput() {
        StringBuilder sb = new StringBuilder();
        addSwitchToInputLine(sb);
        addPrompt(sb);
        System.out.print(sb.toString());
        String input = scanner.nextLine().trim();
        System.out.print("Got input: " + input);
        try { Thread.sleep(500); } catch (InterruptedException ignored) {}

        // TODO: Handle input

        sb = new StringBuilder();
        addEraseLine(sb);
        addSwitchEraseLine(sb, 15);
        addSwitchEraseLine(sb, 14);
        System.out.print(sb.toString());
        System.out.flush();
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
}
