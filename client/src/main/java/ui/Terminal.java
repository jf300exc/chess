package ui;

import chess.ChessBoard;

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
                // TODO: Render board
                try { Thread.sleep(100); } catch (InterruptedException ignored) {}
            }
        }, "Renderer").start();

        new Thread(() -> {
            while (running) {
                System.out.print("\n> ");
                String input = scanner.nextLine();
                // TODO: Handle input
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
}
