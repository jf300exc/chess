package ui;

import chess.ChessBoard;
import chess.ChessGame;
import chess.ChessPiece;
import chess.ChessPosition;

import java.util.AbstractMap;
import java.util.Map;

import static ui.EscapeSequences.BLACK_ROOK;
import static ui.EscapeSequences.BLACK_KNIGHT;
import static ui.EscapeSequences.BLACK_BISHOP;
import static ui.EscapeSequences.BLACK_KING;
import static ui.EscapeSequences.BLACK_QUEEN;
import static ui.EscapeSequences.BLACK_PAWN;
import static ui.EscapeSequences.EMPTY;

public class BoardDraw {

    // Background sequences
    private static final String DARK_BACKGROUND_BROWN = "\u001B[48;5;94m";
    private static final String LIGHT_BACKGROUND_BLUE = "\u001B[48;5;32m";

    // Text color sequences
    private static final String BLACK_TEXT = "\u001B[30m";

    // Reset colors
    private static final String RESET = "\u001B[0m";  // Reset Colors

    // Light Pieces with color sequences
    private static final String LIGHT_ROOK_LIGHT_BACKGROUND =      LIGHT_BACKGROUND_BLUE + BLACK_ROOK + RESET;
    private static final String LIGHT_ROOK_DARK_BACKGROUND =       DARK_BACKGROUND_BROWN + BLACK_BISHOP + RESET;
    private static final String LIGHT_KNIGHT_LIGHT_BACKGROUND =    LIGHT_BACKGROUND_BLUE + BLACK_KNIGHT + RESET;
    private static final String LIGHT_KNIGHT_DARK_BACKGROUND =     DARK_BACKGROUND_BROWN + BLACK_KNIGHT + RESET;
    private static final String LIGHT_BISHOP_LIGHT_BACKGROUND =    LIGHT_BACKGROUND_BLUE + BLACK_BISHOP + RESET;
    private static final String LIGHT_BISHOP_DARK_BACKGROUND =     DARK_BACKGROUND_BROWN + BLACK_BISHOP + RESET;
    private static final String LIGHT_KING_LIGHT_BACKGROUND =      LIGHT_BACKGROUND_BLUE + BLACK_KING + RESET;
    private static final String LIGHT_KING_DARK_BACKGROUND =       DARK_BACKGROUND_BROWN + BLACK_KING + RESET;
    private static final String LIGHT_QUEEN_LIGHT_BACKGROUND =     LIGHT_BACKGROUND_BLUE + BLACK_QUEEN + RESET;
    private static final String LIGHT_QUEEN_DARK_BACKGROUND =      DARK_BACKGROUND_BROWN + BLACK_QUEEN + RESET;
    private static final String LIGHT_PAWN_LIGHT_BACKGROUND =      LIGHT_BACKGROUND_BLUE + BLACK_PAWN + RESET;
    private static final String LIGHT_PAWN_DARK_BACKGROUND =       DARK_BACKGROUND_BROWN + BLACK_PAWN + RESET;

    // Dark Pieces with color sequences
    private static final String DARK_ROOK_LIGHT_BACKGROUND =    LIGHT_BACKGROUND_BLUE + BLACK_TEXT + BLACK_ROOK + RESET;
    private static final String DARK_ROOK_DARK_BACKGROUND =     DARK_BACKGROUND_BROWN + BLACK_TEXT + BLACK_ROOK + RESET;
    private static final String DARK_KNIGHT_LIGHT_BACKGROUND =  LIGHT_BACKGROUND_BLUE + BLACK_TEXT + BLACK_KNIGHT + RESET;
    private static final String DARK_KNIGHT_DARK_BACKGROUND =   DARK_BACKGROUND_BROWN + BLACK_TEXT + BLACK_KNIGHT + RESET;
    private static final String DARK_BISHOP_LIGHT_BACKGROUND =  LIGHT_BACKGROUND_BLUE + BLACK_TEXT + BLACK_BISHOP + RESET;
    private static final String DARK_BISHOP_DARK_BACKGROUND =   DARK_BACKGROUND_BROWN + BLACK_TEXT + BLACK_BISHOP + RESET;
    private static final String DARK_KING_LIGHT_BACKGROUND =    LIGHT_BACKGROUND_BLUE + BLACK_TEXT + BLACK_KING + RESET;
    private static final String DARK_KING_DARK_BACKGROUND =     DARK_BACKGROUND_BROWN + BLACK_TEXT + BLACK_KING + RESET;
    private static final String DARK_QUEEN_LIGHT_BACKGROUND =   LIGHT_BACKGROUND_BLUE + BLACK_TEXT + BLACK_QUEEN + RESET;
    private static final String DARK_QUEEN_DARK_BACKGROUND =    DARK_BACKGROUND_BROWN + BLACK_TEXT + BLACK_QUEEN + RESET;
    private static final String DARK_PAWN_LIGHT_BACKGROUND =    LIGHT_BACKGROUND_BLUE + BLACK_TEXT + BLACK_PAWN + RESET;
    private static final String DARK_PAWN_DARK_BACKGROUND =     DARK_BACKGROUND_BROWN + BLACK_TEXT + BLACK_PAWN + RESET;

    private static final String LIGHT_BACKGROUND = LIGHT_BACKGROUND_BLUE + EMPTY + RESET;
    private static final String DARK_BACKGROUND =  DARK_BACKGROUND_BROWN + EMPTY + RESET;

    private static final String THIN_SPACE = "\u2009";

    private static final String BOARD_HEADER_AND_FOOTER_WHITE = " a  b  c  d  e  f  g  h";
//                    THIN_SPACE + " a " + THIN_SPACE +
//                    THIN_SPACE + " b " + THIN_SPACE +
//                    THIN_SPACE + " c " + THIN_SPACE +
//                    THIN_SPACE + " d " + THIN_SPACE +
//                    THIN_SPACE + " e " + THIN_SPACE +
//                    THIN_SPACE + " f " + THIN_SPACE +
//                    THIN_SPACE + " g " + THIN_SPACE +
//                    THIN_SPACE + " h";

    private static final String BOARD_HEADER_AND_FOOTER_BLACK = " h  g  f  e  d  c  b  a";
//                    THIN_SPACE + " h " + THIN_SPACE +
//                    THIN_SPACE + " g " + THIN_SPACE +
//                    THIN_SPACE + " f " + THIN_SPACE +
//                    THIN_SPACE + " e " + THIN_SPACE +
//                    THIN_SPACE + " d " + THIN_SPACE +
//                    THIN_SPACE + " c " + THIN_SPACE +
//                    THIN_SPACE + " b " + THIN_SPACE +
//                    THIN_SPACE + " a";

    private static final Map<String, String> PIECE_BACKGROUND_MAP = Map.ofEntries(
            new AbstractMap.SimpleEntry<>("ROOK_WHITE_true", LIGHT_ROOK_LIGHT_BACKGROUND),
            new AbstractMap.SimpleEntry<>("ROOK_WHITE_false", LIGHT_ROOK_DARK_BACKGROUND),
            new AbstractMap.SimpleEntry<>("ROOK_BLACK_true", DARK_ROOK_LIGHT_BACKGROUND),
            new AbstractMap.SimpleEntry<>("ROOK_BLACK_false", DARK_ROOK_DARK_BACKGROUND),

            new AbstractMap.SimpleEntry<>("KNIGHT_WHITE_true", LIGHT_KNIGHT_LIGHT_BACKGROUND),
            new AbstractMap.SimpleEntry<>("KNIGHT_WHITE_false", LIGHT_KNIGHT_DARK_BACKGROUND),
            new AbstractMap.SimpleEntry<>("KNIGHT_BLACK_true", DARK_KNIGHT_LIGHT_BACKGROUND),
            new AbstractMap.SimpleEntry<>("KNIGHT_BLACK_false", DARK_KNIGHT_DARK_BACKGROUND),

            new AbstractMap.SimpleEntry<>("BISHOP_WHITE_true", LIGHT_BISHOP_LIGHT_BACKGROUND),
            new AbstractMap.SimpleEntry<>("BISHOP_WHITE_false", LIGHT_BISHOP_DARK_BACKGROUND),
            new AbstractMap.SimpleEntry<>("BISHOP_BLACK_true", DARK_BISHOP_LIGHT_BACKGROUND),
            new AbstractMap.SimpleEntry<>("BISHOP_BLACK_false", DARK_BISHOP_DARK_BACKGROUND),

            new AbstractMap.SimpleEntry<>("KING_WHITE_true", LIGHT_KING_LIGHT_BACKGROUND),
            new AbstractMap.SimpleEntry<>("KING_WHITE_false", LIGHT_KING_DARK_BACKGROUND),
            new AbstractMap.SimpleEntry<>("KING_BLACK_true", DARK_KING_LIGHT_BACKGROUND),
            new AbstractMap.SimpleEntry<>("KING_BLACK_false", DARK_KING_DARK_BACKGROUND),

            new AbstractMap.SimpleEntry<>("QUEEN_WHITE_true", LIGHT_QUEEN_LIGHT_BACKGROUND),
            new AbstractMap.SimpleEntry<>("QUEEN_WHITE_false", LIGHT_QUEEN_DARK_BACKGROUND),
            new AbstractMap.SimpleEntry<>("QUEEN_BLACK_true", DARK_QUEEN_LIGHT_BACKGROUND),
            new AbstractMap.SimpleEntry<>("QUEEN_BLACK_false", DARK_QUEEN_DARK_BACKGROUND),

            new AbstractMap.SimpleEntry<>("PAWN_WHITE_true", LIGHT_PAWN_LIGHT_BACKGROUND),
            new AbstractMap.SimpleEntry<>("PAWN_WHITE_false", LIGHT_PAWN_DARK_BACKGROUND),
            new AbstractMap.SimpleEntry<>("PAWN_BLACK_true", DARK_PAWN_LIGHT_BACKGROUND),
            new AbstractMap.SimpleEntry<>("PAWN_BLACK_false", DARK_PAWN_DARK_BACKGROUND)
    );

    private static String getSpaceString(boolean whiteBackground) {
        if (whiteBackground) {
            return LIGHT_BACKGROUND;
        } else {
            return DARK_BACKGROUND;
        }
    }

    private static String getPieceString(ChessPiece chessPiece, boolean whiteBackground) {
        if (chessPiece == null) {
            return getSpaceString(whiteBackground);
        }
        ChessPiece.PieceType pieceType = chessPiece.getPieceType();
        ChessGame.TeamColor teamColor = chessPiece.getTeamColor();
        String key = pieceType.name() + "_" + teamColor + "_" + whiteBackground;
        return PIECE_BACKGROUND_MAP.getOrDefault(key, "INVALID");
    }

    private static boolean isWhiteBackground(int row, int column) {
        return (row + column) % 2 == 1;
    }

    public static String drawBoard(ChessGame chessGame, ChessGame.TeamColor teamColor) {
        if (teamColor == null || teamColor == ChessGame.TeamColor.WHITE) {
            return drawBoardWhite(chessGame);
        } else {
            return drawBoardBlack(chessGame);
        }
    }

    private static String drawBoardWhite(ChessGame chessGame) {
        StringBuilder builder = new StringBuilder();
        builder.append("   ").append(BOARD_HEADER_AND_FOOTER_WHITE).append("\n");
        Map<ChessPosition, ChessPiece> board = chessGame.getBoard().getBoardMap();
        for (int i = 8; i > 0; i--) {
            builder.append(" ").append(i).append(" ");
            for (int j = 1; j <= ChessBoard.BOARD_SIZE; j++) {
                appendPieceString(i, j, board, builder);
            }
            builder.append(" ").append(i);
            builder.append("\n");
        }
        builder.append("   ").append(BOARD_HEADER_AND_FOOTER_WHITE);
        return builder.toString();
    }

    private static String drawBoardBlack(ChessGame chessGame) {
        StringBuilder builder = new StringBuilder();
        builder.append("   ").append(BOARD_HEADER_AND_FOOTER_BLACK).append("\n");
        Map<ChessPosition, ChessPiece> board = chessGame.getBoard().getBoardMap();
        for (int i = 1; i <= ChessBoard.BOARD_SIZE; i++) {
            builder.append(" ").append(i).append(" ");
            for (int j = 8; j > 0; j--) {
                appendPieceString(i, j, board, builder);
            }
            builder.append(" ").append(i);
            builder.append("\n");
        }
        builder.append("   ").append(BOARD_HEADER_AND_FOOTER_BLACK);
        builder.append(EscapeSequences.RESET_BG_COLOR);
        builder.append(EscapeSequences.RESET_TEXT_COLOR);
        return builder.toString();
    }

    private static void appendPieceString(int row, int col, Map<ChessPosition, ChessPiece> boardMap, StringBuilder stringBuilder) {
        boolean isWhite = isWhiteBackground(row, col);
        ChessPiece currPiece = boardMap.get(new ChessPosition(row, col));
        stringBuilder.append(getPieceString(currPiece, isWhite));
    }
}
