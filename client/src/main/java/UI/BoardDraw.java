package UI;

import chess.ChessBoard;
import chess.ChessGame;
import chess.ChessPiece;
import chess.ChessPosition;

import java.util.Map;

import static UI.EscapeSequences.BLACK_ROOK;
import static UI.EscapeSequences.BLACK_KNIGHT;
import static UI.EscapeSequences.BLACK_BISHOP;
import static UI.EscapeSequences.BLACK_KING;
import static UI.EscapeSequences.BLACK_QUEEN;
import static UI.EscapeSequences.BLACK_PAWN;
import static UI.EscapeSequences.EMPTY;

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
    private static final String DARK_ROOK_LIGHT_BACKGROUND =   LIGHT_BACKGROUND_BLUE + BLACK_TEXT + BLACK_ROOK + RESET;
    private static final String DARK_ROOK_DARK_BACKGROUND =    DARK_BACKGROUND_BROWN + BLACK_TEXT + BLACK_ROOK + RESET;
    private static final String DARK_KNIGHT_LIGHT_BACKGROUND = LIGHT_BACKGROUND_BLUE + BLACK_TEXT + BLACK_KNIGHT + RESET;
    private static final String DARK_KNIGHT_DARK_BACKGROUND =  DARK_BACKGROUND_BROWN + BLACK_TEXT + BLACK_KNIGHT + RESET;
    private static final String DARK_BISHOP_LIGHT_BACKGROUND = LIGHT_BACKGROUND_BLUE + BLACK_TEXT + BLACK_BISHOP + RESET;
    private static final String DARK_BISHOP_DARK_BACKGROUND =  DARK_BACKGROUND_BROWN + BLACK_TEXT + BLACK_BISHOP + RESET;
    private static final String DARK_KING_LIGHT_BACKGROUND =   LIGHT_BACKGROUND_BLUE + BLACK_TEXT + BLACK_KING + RESET;
    private static final String DARK_KING_DARK_BACKGROUND =    DARK_BACKGROUND_BROWN + BLACK_TEXT + BLACK_KING + RESET;
    private static final String DARK_QUEEN_LIGHT_BACKGROUND = LIGHT_BACKGROUND_BLUE + BLACK_TEXT + BLACK_QUEEN + RESET;
    private static final String DARK_QUEEN_DARK_BACKGROUND =  DARK_BACKGROUND_BROWN + BLACK_TEXT + BLACK_QUEEN + RESET;
    private static final String DARK_PAWN_LIGHT_BACKGROUND = LIGHT_BACKGROUND_BLUE + BLACK_TEXT + BLACK_PAWN + RESET;
    private static final String DARK_PAWN_DARK_BACKGROUND =  DARK_BACKGROUND_BROWN + BLACK_TEXT + BLACK_PAWN + RESET;

    private static final String LIGHT_BACKGROUND = LIGHT_BACKGROUND_BLUE + EMPTY + RESET;
    private static final String DARK_BACKGROUND =  DARK_BACKGROUND_BROWN + EMPTY + RESET;

    private static final String THIN_SPACE = "\u2009";

    private static final String BOARD_HEADER_AND_FOOTER_WHITE =
                    THIN_SPACE + " a " + THIN_SPACE +
                    THIN_SPACE + " b " + THIN_SPACE +
                    THIN_SPACE + " c " + THIN_SPACE +
                    THIN_SPACE + " d " + THIN_SPACE +
                    THIN_SPACE + " e " + THIN_SPACE +
                    THIN_SPACE + " f " + THIN_SPACE +
                    THIN_SPACE + " g " + THIN_SPACE +
                    THIN_SPACE + " h";

    private static final String BOARD_HEADER_AND_FOOTER_BLACK =
                    THIN_SPACE + " h " + THIN_SPACE +
                    THIN_SPACE + " g " + THIN_SPACE +
                    THIN_SPACE + " f " + THIN_SPACE +
                    THIN_SPACE + " e " + THIN_SPACE +
                    THIN_SPACE + " d " + THIN_SPACE +
                    THIN_SPACE + " c " + THIN_SPACE +
                    THIN_SPACE + " b " + THIN_SPACE +
                    THIN_SPACE + " a";

    private static String getRookString(ChessGame.TeamColor teamColor, boolean whiteBackground) {
        if (teamColor == ChessGame.TeamColor.WHITE) {
            if (whiteBackground) {
                return LIGHT_ROOK_LIGHT_BACKGROUND;
            } else {
                return LIGHT_ROOK_DARK_BACKGROUND;
            }
        } else {
            if (whiteBackground) {
                return DARK_ROOK_LIGHT_BACKGROUND;
            } else {
                return DARK_ROOK_DARK_BACKGROUND;
            }
        }
    }

    private static String getKnightString(ChessGame.TeamColor teamColor, boolean whiteBackground) {
        if (teamColor == ChessGame.TeamColor.WHITE) {
            if (whiteBackground) {
                return LIGHT_KNIGHT_LIGHT_BACKGROUND;
            } else {
                return LIGHT_KNIGHT_DARK_BACKGROUND;
            }
        } else {
            if (whiteBackground) {
                return DARK_KNIGHT_LIGHT_BACKGROUND;
            } else {
                return DARK_KNIGHT_DARK_BACKGROUND;
            }
        }
    }

    private static String getBishopString(ChessGame.TeamColor teamColor, boolean whiteBackground) {
        if (teamColor == ChessGame.TeamColor.WHITE) {
            if (whiteBackground) {
                return LIGHT_BISHOP_LIGHT_BACKGROUND;
            } else {
                return LIGHT_BISHOP_DARK_BACKGROUND;
            }
        } else {
            if (whiteBackground) {
                return DARK_BISHOP_LIGHT_BACKGROUND;
            } else {
                return DARK_BISHOP_DARK_BACKGROUND;
            }
        }
    }

    private static String getKingString(ChessGame.TeamColor teamColor, boolean whiteBackground) {
        if (teamColor == ChessGame.TeamColor.WHITE) {
            if (whiteBackground) {
                return LIGHT_KING_LIGHT_BACKGROUND;
            } else {
                return LIGHT_KING_DARK_BACKGROUND;
            }
        } else {
            if (whiteBackground) {
                return DARK_KING_LIGHT_BACKGROUND;
            } else {
                return DARK_KING_DARK_BACKGROUND;
            }
        }
    }

    private static String getQueenString(ChessGame.TeamColor teamColor, boolean whiteBackground) {
        if (teamColor == ChessGame.TeamColor.WHITE) {
            if (whiteBackground) {
                return LIGHT_QUEEN_LIGHT_BACKGROUND;
            } else {
                return LIGHT_QUEEN_DARK_BACKGROUND;
            }
        } else {
            if (whiteBackground) {
                return DARK_QUEEN_LIGHT_BACKGROUND;
            } else {
                return DARK_QUEEN_DARK_BACKGROUND;
            }
        }
    }

    private static String getPawnString(ChessGame.TeamColor teamColor, boolean whiteBackground) {
        if (teamColor == ChessGame.TeamColor.WHITE) {
            if (whiteBackground) {
                return LIGHT_PAWN_LIGHT_BACKGROUND;
            } else {
                return LIGHT_PAWN_DARK_BACKGROUND;
            }
        } else {
            if (whiteBackground) {
                return DARK_PAWN_LIGHT_BACKGROUND;
            } else {
                return DARK_PAWN_DARK_BACKGROUND;
            }
        }
    }

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
        switch (pieceType) {
            case ROOK -> {
                return getRookString(teamColor, whiteBackground);
            }
            case KNIGHT -> {
                return getKnightString(teamColor, whiteBackground);
            }
            case BISHOP -> {
                return getBishopString(teamColor, whiteBackground);
            }
            case KING -> {
                return getKingString(teamColor, whiteBackground);
            }
            case QUEEN -> {
                return getQueenString(teamColor, whiteBackground);
            }
            case PAWN -> {
                return getPawnString(teamColor, whiteBackground);
            }
            default -> throw new RuntimeException("Unknown piece type: " + pieceType);
        }
    }

    private static boolean isWhiteBackground(int row, int column) {
        return (row + column) % 2 == 1;
    }

    public static void drawBoard(ChessGame chessGame, ChessGame.TeamColor teamColor) {
        if (teamColor == null || teamColor == ChessGame.TeamColor.WHITE) {
            drawBoardWhite(chessGame);
        } else {
            drawBoardBlack(chessGame);
        }
    }

    private static void drawBoardWhite(ChessGame chessGame) {
        StringBuilder builder = new StringBuilder();
        builder.append("   ").append(BOARD_HEADER_AND_FOOTER_WHITE).append("\n");
        Map<ChessPosition, ChessPiece> board = chessGame.getBoard().getBoardMap();
        for (int i = 8; i > 0; i--) {
            builder.append(" ").append(i).append(" ");
            for (int j = 1; j <= ChessBoard.BOARD_SIZE; j++) {
                boolean isWhite = isWhiteBackground(i, j);
                ChessPiece currPiece = board.get(new ChessPosition(i, j));
                builder.append(getPieceString(currPiece, isWhite));
            }
            builder.append(" ").append(i);
            builder.append("\n");
        }
        builder.append("   ").append(BOARD_HEADER_AND_FOOTER_WHITE);
        System.out.println(builder);
    }

    private static void drawBoardBlack(ChessGame chessGame) {
        StringBuilder builder = new StringBuilder();
        builder.append("   ").append(BOARD_HEADER_AND_FOOTER_BLACK).append("\n");
        Map<ChessPosition, ChessPiece> board = chessGame.getBoard().getBoardMap();
        for (int i = 1; i <= ChessBoard.BOARD_SIZE; i++) {
            builder.append(" ").append(i).append(" ");
            for (int j = 8; j > 0; j--) {
                boolean isWhite = isWhiteBackground(i, j);
                ChessPiece currPiece = board.get(new ChessPosition(i, j));
                builder.append(getPieceString(currPiece, isWhite));
            }
            builder.append(" ").append(i);
            builder.append("\n");
        }
        builder.append("   ").append(BOARD_HEADER_AND_FOOTER_BLACK);
        System.out.println(builder);
        System.out.print(EscapeSequences.RESET_BG_COLOR);
        System.out.print(EscapeSequences.RESET_TEXT_COLOR);
    }
}
