package ui;

import chess.ChessGame;
import chess.ChessBoard;
import chess.ChessPiece;
import chess.ChessPosition;
import websocket.commands.MakeMoveCommand;
import websocket.commands.UserGameCommand;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** No-network fixture used by scripts/test-cli-terminal.py in a real pseudo-terminal. */
public final class TerminalSmokeMain {
    public static void main(String[] args) throws Exception {
        System.setProperty("java.awt.headless", "true");
        List<String> options = Arrays.asList(args);
        try (CliConsole console = CliConsole.open(options.contains("--text"), options.contains("--no-mouse"),
                options.contains("--no-graphics"),
                PieceSymbols.fromArgs(args))) {
            var tty = console.terminal();
            var attributes = tty == null ? null : tty.getAttributes();
            System.out.println("SMOKE mode=" + (tty == null ? "text" : "screen")
                    + " mousecap=" + (tty != null && tty.hasMouseSupport()));
            GamePlay gameplay = new GamePlay(console);
            ChessGame position = new ChessGame();
            if (options.contains("--promotion")) {
                ChessBoard board = new ChessBoard();
                board.addPiece(new ChessPosition(1, 5), new ChessPiece(ChessGame.TeamColor.WHITE, ChessPiece.PieceType.KING));
                board.addPiece(new ChessPosition(8, 5), new ChessPiece(ChessGame.TeamColor.BLACK, ChessPiece.PieceType.KING));
                board.addPiece(new ChessPosition(7, 1), new ChessPiece(ChessGame.TeamColor.WHITE, ChessPiece.PieceType.PAWN));
                position.setBoard(board);
            }
            List<String> moves = new ArrayList<>();
            long previewDeadline = System.nanoTime() + 12_000_000_000L;
            WebSocketClient socket = new WebSocketClient(8080, message -> { }) {
                boolean open;
                @Override public void connectClient() { open = true; }
                @Override public boolean isSessionOpen() {
                    return open && (!options.contains("--preview") || System.nanoTime() < previewDeadline);
                }
                @Override public void closeClient() { open = false; }
                @Override public void sendCommand(Object command) throws Exception {
                    if (command instanceof MakeMoveCommand move) {
                        moves.add(CliInputParser.formatMove(move.getMove()));
                        Terminal.addLogMessage("Confirmed move: " + moves.getLast());
                    } else if (((UserGameCommand) command).getCommandType() == UserGameCommand.CommandType.CONNECT) {
                        Terminal.setChessGame(position, "Terminal smoke match");
                        if (options.contains("--preview")) {
                            Terminal.drawHighlights(new ChessPosition(2, 5));
                        }
                    }
                }
            };
            gameplay.setWebSocket(socket);
            var connect = new UserGameCommand(UserGameCommand.CommandType.CONNECT, "test-token", 42);
            if (options.contains("--observer")) {
                gameplay.observeGame(connect);
            } else {
                gameplay.playGame(connect, "WHITE");
            }
            System.out.println("SMOKE moves=" + moves.size() + " first=" + (moves.isEmpty() ? "none" : moves.getFirst())
                    + " restored=" + (tty == null || attributes.toString().equals(tty.getAttributes().toString())));
        }
    }
}
