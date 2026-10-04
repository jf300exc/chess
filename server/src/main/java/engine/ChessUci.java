package engine;

import chess.*;
import static chess.ChessGame.TeamColor.*;
import static chess.ChessBoard.CastleType.*;

public final class ChessUci {
    private ChessUci() { }

    public static String square(ChessPosition position) {
        return "" + (char) ('a' + position.getColumn() - 1) + position.getRow();
    }

    public static ChessMove move(String text) {
        if (!text.matches("[a-h][1-8][a-h][1-8][qrbn]?")) {
            throw new IllegalArgumentException("Stockfish returned an invalid move");
        }
        ChessPiece.PieceType promotion = text.length() == 4 ? null : switch (text.charAt(4)) {
            case 'q' -> ChessPiece.PieceType.QUEEN;
            case 'r' -> ChessPiece.PieceType.ROOK;
            case 'b' -> ChessPiece.PieceType.BISHOP;
            default -> ChessPiece.PieceType.KNIGHT;
        };
        return new ChessMove(new ChessPosition(text.charAt(1) - '0', text.charAt(0) - 'a' + 1),
                new ChessPosition(text.charAt(3) - '0', text.charAt(2) - 'a' + 1), promotion);
    }

    public static String fen(ChessGame game) {
        ChessBoard board = game.getBoard();
        StringBuilder fen = new StringBuilder();
        for (int row = 8; row >= 1; row--) {
            int empty = 0;
            for (int col = 1; col <= 8; col++) {
                ChessPiece piece = board.getPiece(new ChessPosition(row, col));
                if (piece == null) { empty++; continue; }
                if (empty > 0) { fen.append(empty); empty = 0; }
                char symbol = switch (piece.getPieceType()) {
                    case KING -> 'k'; case QUEEN -> 'q'; case ROOK -> 'r';
                    case BISHOP -> 'b'; case KNIGHT -> 'n'; case PAWN -> 'p';
                };
                fen.append(piece.getTeamColor() == WHITE ? Character.toUpperCase(symbol) : symbol);
            }
            if (empty > 0) { fen.append(empty); }
            if (row > 1) { fen.append('/'); }
        }
        fen.append(game.getTeamTurn() == WHITE ? " w " : " b ");
        StringBuilder rights = new StringBuilder();
        for (ChessGame.TeamColor color : ChessGame.TeamColor.values()) {
            int row = color == WHITE ? 1 : 8;
            for (ChessBoard.CastleType side : ChessBoard.CastleType.values()) {
                ChessPiece king = board.getPiece(new ChessPosition(row, 5));
                ChessPiece rook = board.getPiece(new ChessPosition(row, side == KING_SIDE ? 8 : 1));
                if (board.getCastleStatus(color, side) && king != null && king.getPieceType() == ChessPiece.PieceType.KING
                        && king.getTeamColor() == color && rook != null && rook.getPieceType() == ChessPiece.PieceType.ROOK
                        && rook.getTeamColor() == color) {
                    char symbol = side == KING_SIDE ? 'k' : 'q';
                    rights.append(color == WHITE ? Character.toUpperCase(symbol) : symbol);
                }
            }
        }
        ChessPosition ep = board.getEnPassant(game.getTeamTurn());
        return fen.append(rights.isEmpty() ? "-" : rights).append(' ')
                .append(ep == null ? "-" : square(ep)).append(" 0 1").toString();
    }
}
