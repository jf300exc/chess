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
    private static final String orangeBackGround = "\u001B[48;5;214m";
    private static final String darkBlueBackground = "\u001B[48;5;17m";

    // Text color sequences
    private static final String blackText = "\u001B[30m";

    // Reset colors
    private static final String reset = "\u001B[0m";  // Reset Colors

    // Light Pieces with color sequences
    private static final String lightRookLightBackground =      orangeBackGround + blackRook + reset;
    private static final String lightRookDarkBackground =       darkBlueBackground + blackRook + reset;
    private static final String lightKnightLightBackground =    orangeBackGround + blackKnight + reset;
    private static final String lightKnightDarkBackground =     darkBlueBackground + blackKnight + reset;
    private static final String lightBishopLightBackground =    orangeBackGround  + blackBishop + reset;
    private static final String lightBishopDarkBackground =     darkBlueBackground + blackBishop + reset;
    private static final String lightKingLightBackground =      orangeBackGround + blackKing + reset;
    private static final String lightKingDarkBackground =       darkBlueBackground + blackKing + reset;
    private static final String lightQueenLightBackground =     orangeBackGround + blackQueen + reset;
    private static final String lightQueenDarkBackground =      darkBlueBackground + blackQueen + reset;
    private static final String lightPawnLightBackground =      orangeBackGround + blackPawn + reset;
    private static final String lightPawnDarkBackground =       darkBlueBackground + blackPawn + reset;

    // Dark Pieces with color sequences
    private static final String darkRookLightBackground =   orangeBackGround + blackText + blackRook + reset;
    private static final String darkRookDarkBackground =    darkBlueBackground + blackText + blackRook + reset;
    private static final String darkKnightLightBackground = orangeBackGround + blackText + blackKnight + reset;
    private static final String darkKnightDarkBackground =  darkBlueBackground + blackText + blackKnight + reset;
    private static final String darkBishopLightBackground = orangeBackGround + blackText + blackBishop + reset;
    private static final String darkBishopDarkBackground =  darkBlueBackground + blackText + blackBishop + reset;
    private static final String darkKingLightBackground =   orangeBackGround + blackText + blackKing + reset;
    private static final String darkKingDarkBackground =    darkBlueBackground + blackText + blackKing + reset;
    private static final String darkQueenLightBackground = orangeBackGround + blackText + blackQueen + reset;
    private static final String darkQueenDarkBackground =  darkBlueBackground + blackText + blackQueen + reset;
    private static final String darkPawnLightBackground = orangeBackGround + blackText + blackPawn + reset;
    private static final String darkPawnDarkBackground =  darkBlueBackground + blackText + blackPawn + reset;

    private static final String lightBackground = orangeBackGround + wideSpace + reset;
    private static final String darkBackground =  darkBlueBackground + wideSpace + reset;

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
        Map<ChessPosition, ChessPiece> board = chessGame.getBoard().getBoardMap();
        for (int i = 8; i >= 0; i--) {
            for (int j = 1; j < ChessBoard.BOARD_SIZE + 1; j++) {
                boolean isWhite = isWhiteBackground(i, j);
                ChessPiece currPiece = board.get(new ChessPosition(i, j));
                builder.append(getPieceString(currPiece, isWhite));
            }
        }
        System.out.println(builder);
    }

    private static void drawBoardBlack(ChessGame chessGame) {
        return;
    }
}
