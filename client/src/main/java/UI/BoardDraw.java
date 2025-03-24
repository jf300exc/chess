package UI;

import chess.ChessBoard;
import chess.ChessGame;
import chess.ChessPiece;
import chess.ChessPosition;

import java.util.Map;

public class BoardDraw {
    // Light piece sequences
    private static final String whiteRook = "♖"; // Rook: ♖
    private static final String whiteKnight = "♘"; // Knight: ♘
    private static final String whiteBishop = "♗"; // Bishop: ♗
    private static final String whiteKing = "♔"; // King: ♔
    private static final String whiteQueen = "♕"; // Queen: ♕
    private static final String whitePawn = "♙"; // Pawn: ♙

    // Dark piece sequences
    private static final String blackRook =  "♜";
    private static final String blackKnight = "♞";
    private static final String blackBishop = "♝";
    private static final String blackKing = "♚";
    private static final String blackQueen = "♛";
    private static final String blackPawn = "♟";

    // For empty positions
    private static final String wideSpace = "\u2003";

    // Background sequences
    private static final String darkBackgroundBrown = "\u001B[48;5;94m";
    private static final String lightBackgroundBlue = "\u001B[48;5;32m";


    // Text color sequences
    private static final String blackText = "\u001B[30m";

    // Reset colors
    private static final String reset = "\u001B[0m";  // Reset Colors

    // Light Pieces with color sequences
    private static final String lightRookLightBackground =      lightBackgroundBlue + blackRook + reset;
    private static final String lightRookDarkBackground =       darkBackgroundBrown + blackRook + reset;
    private static final String lightKnightLightBackground =    lightBackgroundBlue + blackKnight + reset;
    private static final String lightKnightDarkBackground =     darkBackgroundBrown + blackKnight + reset;
    private static final String lightBishopLightBackground =    lightBackgroundBlue + blackBishop + reset;
    private static final String lightBishopDarkBackground =     darkBackgroundBrown + blackBishop + reset;
    private static final String lightKingLightBackground =      lightBackgroundBlue + blackKing + reset;
    private static final String lightKingDarkBackground =       darkBackgroundBrown + blackKing + reset;
    private static final String lightQueenLightBackground =     lightBackgroundBlue + blackQueen + reset;
    private static final String lightQueenDarkBackground =      darkBackgroundBrown + blackQueen + reset;
    private static final String lightPawnLightBackground =      lightBackgroundBlue + blackPawn + reset;
    private static final String lightPawnDarkBackground =       darkBackgroundBrown + blackPawn + reset;

    // Dark Pieces with color sequences
    private static final String darkRookLightBackground =   lightBackgroundBlue + blackText + blackRook + reset;
    private static final String darkRookDarkBackground =    darkBackgroundBrown + blackText + blackRook + reset;
    private static final String darkKnightLightBackground = lightBackgroundBlue + blackText + blackKnight + reset;
    private static final String darkKnightDarkBackground =  darkBackgroundBrown + blackText + blackKnight + reset;
    private static final String darkBishopLightBackground = lightBackgroundBlue + blackText + blackBishop + reset;
    private static final String darkBishopDarkBackground =  darkBackgroundBrown + blackText + blackBishop + reset;
    private static final String darkKingLightBackground =   lightBackgroundBlue + blackText + blackKing + reset;
    private static final String darkKingDarkBackground =    darkBackgroundBrown + blackText + blackKing + reset;
    private static final String darkQueenLightBackground = lightBackgroundBlue + blackText + blackQueen + reset;
    private static final String darkQueenDarkBackground =  darkBackgroundBrown + blackText + blackQueen + reset;
    private static final String darkPawnLightBackground = lightBackgroundBlue + blackText + blackPawn + reset;
    private static final String darkPawnDarkBackground =  darkBackgroundBrown + blackText + blackPawn + reset;

    private static final String lightBackground = lightBackgroundBlue + wideSpace + reset;
    private static final String darkBackground =  darkBackgroundBrown + wideSpace + reset;

    private static final String thinSpace = "\u2009";
    private static final String boardHeaderAndFooter =
                    "h" + thinSpace + thinSpace +
                    "g" + thinSpace + thinSpace +
                    "f" + thinSpace + thinSpace +
                    "e" + thinSpace + thinSpace +
                    "d" + thinSpace + thinSpace +
                    "c" + thinSpace + thinSpace +
                    "b" + thinSpace + thinSpace +
                    "a";

    private static String getRookString(ChessGame.TeamColor teamColor, boolean whiteBackground) {
        if (teamColor == ChessGame.TeamColor.WHITE) {
            if (whiteBackground) {
                return lightRookLightBackground;
            } else {
                return lightRookDarkBackground;
            }
        } else {
            if (whiteBackground) {
                return darkRookLightBackground;
            } else {
                return darkRookDarkBackground;
            }
        }
    }

    private static String getKnightString(ChessGame.TeamColor teamColor, boolean whiteBackground) {
        if (teamColor == ChessGame.TeamColor.WHITE) {
            if (whiteBackground) {
                return lightKnightLightBackground;
            } else {
                return lightKnightDarkBackground;
            }
        } else {
            if (whiteBackground) {
                return darkKnightLightBackground;
            } else {
                return darkKnightDarkBackground;
            }
        }
    }

    private static String getBishopString(ChessGame.TeamColor teamColor, boolean whiteBackground) {
        if (teamColor == ChessGame.TeamColor.WHITE) {
            if (whiteBackground) {
                return lightBishopLightBackground;
            } else {
                return lightBishopDarkBackground;
            }
        } else {
            if (whiteBackground) {
                return darkBishopLightBackground;
            } else {
                return darkBishopDarkBackground;
            }
        }
    }

    private static String getKingString(ChessGame.TeamColor teamColor, boolean whiteBackground) {
        if (teamColor == ChessGame.TeamColor.WHITE) {
            if (whiteBackground) {
                return lightKingLightBackground;
            } else {
                return lightKingDarkBackground;
            }
        } else {
            if (whiteBackground) {
                return darkKingLightBackground;
            } else {
                return darkKingDarkBackground;
            }
        }
    }

    private static String getQueenString(ChessGame.TeamColor teamColor, boolean whiteBackground) {
        if (teamColor == ChessGame.TeamColor.WHITE) {
            if (whiteBackground) {
                return lightQueenLightBackground;
            } else {
                return lightQueenDarkBackground;
            }
        } else {
            if (whiteBackground) {
                return darkQueenLightBackground;
            } else {
                return darkQueenDarkBackground;
            }
        }
    }

    private static String getPawnString(ChessGame.TeamColor teamColor, boolean whiteBackground) {
        if (teamColor == ChessGame.TeamColor.WHITE) {
            if (whiteBackground) {
                return lightPawnLightBackground;
            } else {
                return lightPawnDarkBackground;
            }
        } else {
            if (whiteBackground) {
                return darkPawnLightBackground;
            } else {
                return darkPawnDarkBackground;
            }
        }
    }

    private static String getSpaceString(boolean whiteBackground) {
        if (whiteBackground) {
            return lightBackground;
        } else {
            return darkBackground;
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
        builder.append(boardHeaderAndFooter + "\n");
        Map<ChessPosition, ChessPiece> board = chessGame.getBoard().getBoardMap();
        for (int i = 8; i > 0; i--) {
            for (int j = 1; j < ChessBoard.BOARD_SIZE + 1; j++) {
                boolean isWhite = isWhiteBackground(i, j);
                ChessPiece currPiece = board.get(new ChessPosition(i, j));
                builder.append(getPieceString(currPiece, isWhite));
            }
            builder.append("\n");
        }
        System.out.println(builder);
    }

    private static void drawBoardBlack(ChessGame chessGame) {
        return;
    }
}
