package websocket.messages;

import adapters.ChessPositionAdapter;
import chess.*;
import com.google.gson.*;
import model.GameData;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BrowserGameStateTests {
    private final Gson gson = new GsonBuilder()
            .registerTypeAdapter(ChessPosition.class, new ChessPositionAdapter()).create();

    private JsonObject message(ChessGame game) {
        return gson.toJsonTree(new LoadGameMessage(ServerMessage.ServerMessageType.LOAD_GAME,
                new GameData(7, "white", "black", "Browser match", game))).getAsJsonObject();
    }

    @Test
    void sendsLegalMovesWithWireCoordinatesWithoutChangingBoard() {
        ChessGame game = new ChessGame();
        ChessGame before = game.copy();
        JsonObject json = message(game);
        assertEquals(before, game, "Computing the browser moves must not mutate the live game");
        assertEquals(40, json.getAsJsonArray("legalMoves").size());
        assertTrue(json.getAsJsonArray("legalMoves").asList().stream().anyMatch(element -> {
            var move = element.getAsJsonObject();
            var start = move.getAsJsonObject("startPosition");
            var end = move.getAsJsonObject("endPosition");
            return start.get("row").getAsInt() == 2 && start.get("col").getAsInt() == 5
                    && end.get("row").getAsInt() == 4 && end.get("col").getAsInt() == 5;
        }));
        assertFalse(json.get("inCheck").getAsBoolean());
    }

    @Test
    void reportsCheckmateAndStopsOfferingMoves() throws InvalidMoveException {
        ChessGame game = new ChessGame();
        game.makeMove(new ChessMove(new ChessPosition(2, 6), new ChessPosition(3, 6), null));
        game.makeMove(new ChessMove(new ChessPosition(7, 5), new ChessPosition(5, 5), null));
        game.makeMove(new ChessMove(new ChessPosition(2, 7), new ChessPosition(4, 7), null));
        game.makeMove(new ChessMove(new ChessPosition(8, 4), new ChessPosition(4, 8), null));
        JsonObject json = message(game);
        assertTrue(json.get("checkmate").getAsBoolean());
        assertTrue(json.get("inCheck").getAsBoolean());
        assertEquals(0, json.getAsJsonArray("legalMoves").size());
    }

    @Test
    void resignationStopsOfferingMoves() {
        ChessGame game = new ChessGame();
        game.setGameOver(true);
        assertEquals(0, message(game).getAsJsonArray("legalMoves").size());
    }

    @Test
    void reportsStalemateWithoutCheckOrMoves() {
        ChessGame game = new ChessGame();
        ChessBoard board = new ChessBoard();
        board.addPiece(new ChessPosition(6, 3), new ChessPiece(ChessGame.TeamColor.WHITE, ChessPiece.PieceType.KING));
        board.addPiece(new ChessPosition(8, 1), new ChessPiece(ChessGame.TeamColor.BLACK, ChessPiece.PieceType.KING));
        board.addPiece(new ChessPosition(6, 2), new ChessPiece(ChessGame.TeamColor.WHITE, ChessPiece.PieceType.QUEEN));
        game.setBoard(board);
        game.setTeamTurn(ChessGame.TeamColor.BLACK);
        JsonObject json = message(game);
        assertTrue(json.get("stalemate").getAsBoolean());
        assertFalse(json.get("inCheck").getAsBoolean());
        assertEquals(0, json.getAsJsonArray("legalMoves").size());
    }

    @Test
    void promotionIncludesEveryChoice() {
        ChessGame game = new ChessGame();
        ChessBoard board = new ChessBoard();
        board.addPiece(new ChessPosition(1, 5), new ChessPiece(ChessGame.TeamColor.WHITE, ChessPiece.PieceType.KING));
        board.addPiece(new ChessPosition(8, 5), new ChessPiece(ChessGame.TeamColor.BLACK, ChessPiece.PieceType.KING));
        board.addPiece(new ChessPosition(7, 1), new ChessPiece(ChessGame.TeamColor.WHITE, ChessPiece.PieceType.PAWN));
        game.setBoard(board);
        long promotions = message(game).getAsJsonArray("legalMoves").asList().stream()
                .filter(element -> element.getAsJsonObject().has("promotionPiece")).count();
        assertEquals(4, promotions);
    }
}
