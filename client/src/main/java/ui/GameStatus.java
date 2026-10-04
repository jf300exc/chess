package ui;

import chess.ChessGame;

/** Outcome and turn cues do not depend on the board's viewing direction. */
final class GameStatus {
    record Status(String text, String color, boolean waiting) { }

    static Status describe(ChessGame game, ChessGame.TeamColor player, String ending) {
        if (game == null) {
            return new Status("Waiting for game data", "36", true);
        }
        var turn = game.getTeamTurn();
        if (game.isInCheckmate(turn)) {
            var winner = turn == ChessGame.TeamColor.WHITE ? ChessGame.TeamColor.BLACK : ChessGame.TeamColor.WHITE;
            return new Status("GAME OVER  /  " + winner + " wins by checkmate", "33", false);
        }
        if (game.isInStalemate(turn)) {
            return new Status("GAME OVER  /  Draw by stalemate", "33", false);
        }
        if (game.isGameOver()) {
            return new Status("GAME OVER" + (ending == null ? "" : "  /  " + ending), "33", false);
        }
        boolean ownTurn = player == turn;
        String text = player == null ? turn + " to move" : ownTurn ? "YOUR TURN  /  " + turn + " to move"
                : "Waiting for " + turn;
        boolean check = game.isInCheck(turn);
        // Normal ANSI colors avoid bold-to-bright mapping washing out text on light themes.
        return new Status(text + (check ? "  /  CHECK!" : ""), check ? "31" : ownTurn ? "32" : "36",
                player != null && !ownTurn);
    }

    static String paint(String text, String color, boolean enabled) {
        return enabled ? "\u001b[" + color + "m" + text + "\u001b[0m" : text;
    }
}
