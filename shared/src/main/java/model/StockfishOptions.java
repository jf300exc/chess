package model;

import chess.ChessGame.TeamColor;

/** Null Elo means match the human's current rating when the game is created. */
public record StockfishOptions(TeamColor color, Mode mode, Integer elo, Integer skillLevel) {
    public enum Mode { ELO, SKILL }

    public void validate() {
        if (color == null || mode == null
                || (mode == Mode.ELO && (skillLevel != null || (elo != null && (elo < 100 || elo > 4000))))
                || (mode == Mode.SKILL && (elo != null || skillLevel == null || skillLevel < 0 || skillLevel > 20))) {
            throw new IllegalArgumentException("Choose Elo difficulty or a skill level from 0 to 20");
        }
    }
}
