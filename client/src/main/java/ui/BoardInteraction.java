package ui;

import chess.ChessGame;
import chess.ChessMove;
import chess.ChessPiece;
import chess.ChessPosition;

/** Selection state is independent of board orientation and cleared by authoritative updates. */
final class BoardInteraction {
    record Result(ChessPosition selected, ChessMove move, String message) { }
    private ChessPosition selected;
    private boolean awaitingServer;

    synchronized void clear() {
        selected = null;
        awaitingServer = false;
    }

    synchronized Result click(ChessGame game, ChessPosition square, ChessGame.TeamColor player) {
        if (awaitingServer) {
            return new Result(null, null, "Waiting for the server to confirm the move.");
        }
        if (game == null) {
            return new Result(null, null, "Waiting for game data.");
        }
        if (game.isGameOver()) {
            selected = null;
            return new Result(null, null, "Game over.");
        }
        if (square.equals(selected)) {
            selected = null;
            return new Result(null, null, "Selection cleared.");
        }
        ChessPiece piece = game.getBoard().getPiece(square);
        if (player == null) {
            selected = piece == null ? null : square;
            return new Result(selected, null, "Observing: click a piece to inspect its legal moves.");
        }
        if (game.getTeamTurn() != player) {
            selected = null;
            return new Result(null, null, "It is " + game.getTeamTurn() + "'s turn.");
        }
        if (piece != null && piece.getTeamColor() == player) {
            selected = square;
            return new Result(selected, null, "Selected " + square + ". Click a highlighted destination.");
        }
        if (selected != null) {
            var moves = game.validMoves(selected);
            if (moves != null && moves.stream().anyMatch(move -> square.equals(move.getEndPosition()))) {
                ChessMove move = new ChessMove(selected, square, null);
                selected = null;
                awaitingServer = true;
                return new Result(null, move, null);
            }
            return new Result(selected, null, "Illegal destination. Choose a highlighted square or right-click to cancel.");
        }
        return new Result(null, null, "Click one of your pieces first.");
    }
}
