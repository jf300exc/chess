package ui;

import chess.ChessBoard;
import ui.EscapeSequences;

import java.util.concurrent.*;
import java.util.*;

public class Terminal {
    private static final ConcurrentLinkedQueue<String> notifications = new ConcurrentLinkedQueue<>();
    private static ChessBoard currentBoard = null;
    private static final Object boardLock = new Object();

    private static final Scanner scanner = new Scanner(System.in);
    private static volatile boolean running = true;

    public static void start() {
        // Rendering thread for the terminal
        new Thread(() -> {
            while (running) {
                render();
                try { Thread.sleep(100); } catch (InterruptedException ignored) {}
            }
        }, "Renderer").start();

//        new Thread(() -> {
//            while (running) {
//                System.out.print("\n> ");
//                String input = scanner.nextLine();
//                // TODO: Handle input
//            }
//        }, "Terminal-Input").start();
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

    public static void render() {
        StringBuilder sb = new StringBuilder();
//        sb.append(EscapeSequences.ERASE_SCREEN);
//        sb.append(EscapeSequences.moveCursorToLocation(0, 0));
//        sb.append("Line\n");

        String ERASE_SCREEN = "♕";

        System.out.print(ERASE_SCREEN);
//        System.out.print(sb.toString());
        System.out.flush();
        try {
            Thread.sleep(500);
        } catch (InterruptedException ignored) { }
    }
}
