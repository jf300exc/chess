package websocket.messages;

import model.GameData;
import chess.ChessGame;
import chess.ChessMove;
import chess.ChessPosition;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class LoadGameMessage extends ServerMessage {
    private final GameData game;
    // Additive wire fields let browser clients use the authoritative Java rules.
    private final List<ChessMove> legalMoves = new ArrayList<>();
    private final boolean inCheck;
    private final boolean checkmate;
    private final boolean stalemate;

    public LoadGameMessage(ServerMessageType type, GameData game) {
        super(type);
        this.game = game;
        ChessGame snapshot = game.game().copy();
        var turn = snapshot.getTeamTurn();
        inCheck = snapshot.isInCheck(turn);
        checkmate = snapshot.isInCheckmate(turn);
        stalemate = snapshot.isInStalemate(turn);
        if (!snapshot.isGameOver() && !checkmate && !stalemate) {
            for (int row = 1; row <= 8; row++) {
                for (int col = 1; col <= 8; col++) {
                    var moves = snapshot.validMoves(new ChessPosition(row, col));
                    if (moves != null) {
                        legalMoves.addAll(moves);
                    }
                }
            }
        }
    }

    public GameData getGame() {
        return game;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }
        LoadGameMessage that = (LoadGameMessage) o;
        return Objects.equals(game, that.game);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), game);
    }
}
