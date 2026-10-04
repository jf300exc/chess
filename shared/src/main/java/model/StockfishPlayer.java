package model;

import chess.ChessGame.TeamColor;

/** Persisted engine seat and actual difficulty, fixed for the duration of a match. */
public record StockfishPlayer(TeamColor color, StockfishOptions.Mode mode, int elo, Integer skillLevel) {
    public static final String USERNAME = "Stockfish";

    public String description() {
        return mode == StockfishOptions.Mode.ELO ? "Stockfish · " + elo + " Elo" : "Stockfish · level " + skillLevel;
    }
}
