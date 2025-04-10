package ui;

import chess.ChessBoard;
import chess.ChessGame;
import ui.EscapeSequences;

import java.util.concurrent.*;
import java.util.*;

public class Terminal {
    private static final ConcurrentLinkedQueue<String> notifications = new ConcurrentLinkedQueue<>();
    private static ChessBoard currentBoard = null;
    private static final Object boardLock = new Object();
    private static int i = 0;

    private static final Scanner scanner = new Scanner(System.in);
    private static volatile boolean running = true;

    public static void start() {
        // Rendering thread for the terminal
        new Thread(() -> {
            while (running) {
                render();
                try { Thread.sleep(200); } catch (InterruptedException ignored) {}
            }
        }, "Renderer").start();
        new Thread(() -> {
            System.out.println('\n');
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

    public static void setChessBoard(ChessBoard chessBoard) {
        synchronized (boardLock) {
            currentBoard = chessBoard;
        }
    }

    private static void render() {
        StringBuilder sb = new StringBuilder();
        sb.append("\033[s");  // Save cursor position
//        sb.append(EscapeSequences.ERASE_SCROLL_BACK);
        sb.append(EscapeSequences.moveCursorToLocation(0, 0));
        sb.append(EscapeSequences.ERASE_LINE);
        sb.append(EscapeSequences.moveCursorToLocation(0, 1));
        sb.append(EscapeSequences.ERASE_LINE);
        sb.append("Line1").append(i).append('\n');
        sb.append("Line2").append(i++);

        var game = new ChessGame();
        sb.append(BoardDraw.drawBoard(game, ChessGame.TeamColor.WHITE));


        sb.append("\033[u");  // Restore cursor position


        System.out.print(sb.toString());
        System.out.flush();
    }

    private static void getInput() {
        System.out.print("\n>>> ");
        String input = scanner.nextLine().trim();
        System.out.print("Got input: " + input);
        try { Thread.sleep(500); } catch (InterruptedException ignored) {}

        // TODO: Handle input

        StringBuilder sb = new StringBuilder();
        sb.append(EscapeSequences.ERASE_LINE);
        sb.append(EscapeSequences.moveCursorToLocation(0, 4));
        sb.append(EscapeSequences.ERASE_LINE);
        sb.append(EscapeSequences.moveCursorToLocation(0, 3));
        sb.append(EscapeSequences.ERASE_LINE);
        System.out.print(sb.toString());
        System.out.flush();
    }
}
