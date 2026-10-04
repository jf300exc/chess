package ui;

import chess.ChessGame.TeamColor;
import chess.ChessPiece.PieceType;

import java.util.Locale;

/** Glyph choice is explicit: terminal capabilities cannot identify the user's font. */
public enum PieceSymbols {
    UNICODE, NERD, ASCII;

    public static PieceSymbols fromArgs(String[] args) {
        String choice = "unicode";
        for (int i = 0; i < args.length; i++) {
            if (args[i].startsWith("--pieces=")) {
                choice = args[i].substring("--pieces=".length());
            } else if (args[i].equals("--pieces")) {
                if (++i >= args.length || args[i].startsWith("--")) {
                    throw new IllegalArgumentException("--pieces requires unicode, nerd, or ascii.");
                }
                choice = args[i];
            }
        }
        try {
            return valueOf(choice.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Unknown piece style '" + choice + "'; use unicode, nerd, or ascii.");
        }
    }

    String glyph(PieceType type, TeamColor team) {
        int index = switch (type) {
            case KING -> 0;
            case QUEEN -> 1;
            case ROOK -> 2;
            case BISHOP -> 3;
            case KNIGHT -> 4;
            case PAWN -> 5;
        };
        return switch (this) {
            case UNICODE -> Character.toString((team == TeamColor.WHITE ? 0x2654 : 0x265A) + index);
            // Nerd Fonts v3 Material Design chess icons (glyphnames.json, md-chess_*).
            case NERD -> Character.toString(new int[]{0xF0857, 0xF085A, 0xF085B, 0xF085C, 0xF0858, 0xF0859}[index]);
            case ASCII -> Character.toString((team == TeamColor.WHITE ? "KQRBNP" : "kqrbnp").charAt(index));
        };
    }

    PieceSymbols forRendering(boolean color) {
        // Nerd icons have one silhouette per piece. Without color, Unicode's
        // distinct white/black shapes keep teams identifiable in plain output.
        return this == NERD && !color ? UNICODE : this;
    }

    String legend() {
        return switch (this) {
            case UNICODE -> "White: ♔♕♖♗♘♙ | Black: ♚♛♜♝♞♟ | + legal destination";
            case NERD -> "Light pieces = White | dark pieces = Black | + legal destination";
            case ASCII -> "White: KQRBNP | Black: kqrbnp | N/n knight | + legal destination";
        };
    }
}
