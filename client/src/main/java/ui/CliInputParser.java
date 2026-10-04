package ui;

import chess.ChessMove;
import chess.ChessPiece;
import chess.ChessPosition;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Parsing and formatting helpers shared by the terminal client commands. */
final class CliInputParser {
    private static final Pattern POSITION_PATTERN = Pattern.compile("(?i)^([a-h])([1-8])$");
    private static final Pattern MOVE_PATTERN = Pattern.compile(
            "(?i)^([a-h][1-8])\\s*-?\\s*([a-h][1-8])(?:\\s*=?(q|r|b|n))?$");

    private CliInputParser() {
    }

    static String normalizeCommand(String command) {
        return command == null ? "" : command.trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
    }

    static ChessPosition parsePosition(String input) {
        if (input == null) {
            return null;
        }
        Matcher matcher = POSITION_PATTERN.matcher(input.trim());
        if (!matcher.matches()) {
            return null;
        }
        return new ChessPosition(
                matcher.group(2).charAt(0) - '0',
                matcher.group(1).toLowerCase(Locale.ROOT).charAt(0) - 'a' + 1);
    }

    static ChessMove parseMove(String input) {
        if (input == null) {
            return null;
        }
        Matcher matcher = MOVE_PATTERN.matcher(input.trim());
        if (!matcher.matches()) {
            return null;
        }
        ChessPosition start = parsePosition(matcher.group(1));
        ChessPosition end = parsePosition(matcher.group(2));
        return new ChessMove(start, end, parsePromotion(matcher.group(3)));
    }

    static String formatMove(ChessMove move) {
        StringBuilder formatted = new StringBuilder()
                .append(move.getStartPosition())
                .append(move.getEndPosition());
        if (move.getPromotionPiece() != null) {
            formatted.append('=').append(formatPieceType(move.getPromotionPiece()));
        }
        return formatted.toString();
    }

    private static ChessPiece.PieceType parsePromotion(String promotion) {
        if (promotion == null) {
            return null;
        }
        return switch (promotion.toLowerCase(Locale.ROOT)) {
            case "q" -> ChessPiece.PieceType.QUEEN;
            case "r" -> ChessPiece.PieceType.ROOK;
            case "b" -> ChessPiece.PieceType.BISHOP;
            case "n" -> ChessPiece.PieceType.KNIGHT;
            default -> null;
        };
    }

    private static char formatPieceType(ChessPiece.PieceType pieceType) {
        return switch (pieceType) {
            case QUEEN -> 'Q';
            case ROOK -> 'R';
            case BISHOP -> 'B';
            case KNIGHT -> 'N';
            default -> '?';
        };
    }
}
