package ui;

import chess.*;
import org.junit.jupiter.api.Test;
import websocket.commands.MakeMoveCommand;
import websocket.commands.UserGameCommand;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static websocket.commands.UserGameCommand.CommandType.*;

class GamePlayCliTests {
    @Test
    void textFallbackUsesInlineCommandsAndReturnsToLobbyWithoutCompetingReaders() throws Exception {
        var original = System.in;
        try {
            System.setIn(new ByteArrayInputStream("highlight e2\nmove nonsense\nmove e2e4\nleave\nlobby input\n".getBytes(StandardCharsets.UTF_8)));
            try (CliConsole console = CliConsole.open(true, true)) {
                GamePlay play = new GamePlay(console);
                FakeSocket ws = new FakeSocket(new ChessGame());
                play.setWebSocket(ws);
                play.playGame(new UserGameCommand(CONNECT, "token", 42), "WHITE");
                assertEquals(1, ws.moves.size());
                assertEquals(CliInputParser.parseMove("e2e4"), ws.moves.getFirst().getMove());
                assertFalse(ws.open);
                assertEquals("lobby input", console.readLine("Lobby > "));
            }
        } finally {
            System.setIn(original);
        }
    }

    @Test
    void promotionsAreValidatedWithoutMutatingSnapshotDuringDetection() throws Exception {
        ChessGame game = new ChessGame();
        ChessBoard board = new ChessBoard();
        board.addPiece(new ChessPosition(1, 5), new ChessPiece(ChessGame.TeamColor.WHITE, ChessPiece.PieceType.KING));
        board.addPiece(new ChessPosition(8, 5), new ChessPiece(ChessGame.TeamColor.BLACK, ChessPiece.PieceType.KING));
        board.addPiece(new ChessPosition(7, 1), new ChessPiece(ChessGame.TeamColor.WHITE, ChessPiece.PieceType.PAWN));
        game.setBoard(board);
        var original = System.in;
        try {
            System.setIn(new ByteArrayInputStream("move a7a8\nn\nleave\n".getBytes(StandardCharsets.UTF_8)));
            try (CliConsole console = CliConsole.open(true, true)) {
                GamePlay play = new GamePlay(console);
                FakeSocket ws = new FakeSocket(game);
                play.setWebSocket(ws);
                play.playGame(new UserGameCommand(CONNECT, "token", 42), "WHITE");
                assertEquals(1, ws.moves.size());
                assertEquals(ChessPiece.PieceType.KNIGHT, ws.moves.getFirst().getMove().getPromotionPiece());
                assertEquals(ChessPiece.PieceType.PAWN, game.getBoard().getPiece(new ChessPosition(7, 1)).getPieceType());
            }
        } finally {
            System.setIn(original);
        }
    }

    @Test
    void observerCommandsAndEofCannotSendMoves() throws Exception {
        var original = System.in;
        try {
            System.setIn(new ByteArrayInputStream("move e2e4\n".getBytes(StandardCharsets.UTF_8)));
            try (CliConsole console = CliConsole.open(true, true)) {
                GamePlay play = new GamePlay(console);
                FakeSocket ws = new FakeSocket(new ChessGame());
                play.setWebSocket(ws);
                play.observeGame(new UserGameCommand(CONNECT, "token", 42));
                assertTrue(ws.moves.isEmpty());
                assertFalse(ws.open);
            }
        } finally {
            System.setIn(original);
        }
    }

    private static class FakeSocket extends WebSocketClient {
        boolean open;
        final ChessGame position;
        final List<MakeMoveCommand> moves = new ArrayList<>();

        FakeSocket(ChessGame game) {
            super(8080, message -> { });
            position = game;
        }

        @Override public void connectClient() { open = true; }
        @Override public boolean isSessionOpen() { return open; }
        @Override public void closeClient() { open = false; }
        @Override public void sendCommand(Object command) {
            if (command instanceof MakeMoveCommand move) {
                moves.add(move);
            } else if (((UserGameCommand) command).getCommandType() == CONNECT) {
                Terminal.setChessGame(position, "CLI test");
            }
        }
    }
}
