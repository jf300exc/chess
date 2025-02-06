package chess;

import java.util.Collection;

import chess.ChessPiece.PieceType;

/**
 * For a class that can manage a chess game, making moves on a board
 * <p>
 * Note: You can add to this class, but you may not alter
 * signature of the existing methods.
 */
public class ChessGame {
    private ChessBoard gameBoard = new ChessBoard();
    private TeamColor teamTurn = TeamColor.WHITE;

    public ChessGame() { }

    /**
     * @return Which team's turn it is
     */
    public TeamColor getTeamTurn() {
        return teamTurn;
    }

    /**
     * Set's which teams turn it is
     *
     * @param team the team whose turn it is
     */
    public void setTeamTurn(TeamColor team) {
        teamTurn = team;
    }

    /**
     * Enum identifying the 2 possible teams in a chess game
     */
    public enum TeamColor {
        WHITE,
        BLACK
    }

    /**
     * Gets a valid moves for a piece at the given location
     *
     * @param startPosition the piece to get valid moves for
     * @return Set of valid moves for requested piece, or null if no piece at
     * startPosition
     */
    public Collection<ChessMove> validMoves(ChessPosition startPosition) {
        throw new RuntimeException("Not implemented");
    }

    /**
     * Makes a move in a chess game
     *
     * @param move chess move to preform
     * @throws InvalidMoveException if move is invalid
     */
    public void makeMove(ChessMove move) throws InvalidMoveException {
        throw new RuntimeException("Not implemented");
    }

    /**
     * Determines if a horizontally attacking threat has the given king in check
     * 
     * @param kingPos The position of the king to check
     * @param teamColor The team of the given king
     * @return True if the king is in check from a piece horizontal to it
     */
    private boolean checkThreatHorizontal(ChessPosition kingPos, TeamColor teamColor) {
        int row = kingPos.getRow();
        for (int h = -1; h <= 1; h += 2) {
            for (int col = kingPos.getColumn() + h; col > 0 && col <= ChessBoard.BOARD_SIZE; col += h) {
                ChessPosition horizPosition = new ChessPosition(row, col);
                ChessPiece potentialThreat = gameBoard.getPiece(horizPosition);
                // Either return true or break
                if (potentialThreat != null) {
                    if (potentialThreat.getTeamColor() != teamColor) {
                        PieceType threatPieceType = potentialThreat.getPieceType();
                        if (threatPieceType == PieceType.ROOK || threatPieceType == PieceType.QUEEN) {
                            return true;
                        }
                    }
                    break;
                }
            }
        }
        return false;
    }

    /**
     * Determines if a vertically attacking threat has the given king in check
     * 
     * @param kingPos The position of the king to check
     * @param teamColor The team of the given king
     * @return True if the king is in check from a piece vertical to it
     */
    private boolean checkThreatVertical(ChessPosition kingPos, TeamColor teamColor) {
        int col = kingPos.getColumn();
        for (int v = -1; v <= 1; v += 2) {
            for (int row = kingPos.getRow() + v; row > 0 && row <= ChessBoard.BOARD_SIZE; row += v) {
                ChessPosition verPosition = new ChessPosition(row, col);
                ChessPiece potentialThreat = gameBoard.getPiece(verPosition);
                // Either return true or break
                if (potentialThreat != null) {
                    if (potentialThreat.getTeamColor() != teamColor) {
                        PieceType threatPieceType = potentialThreat.getPieceType();
                        if (threatPieceType == PieceType.ROOK || threatPieceType == PieceType.QUEEN) {
                            return true;
                        }
                    }
                    break;
                }
            }
        }
        return false;
    }

    /**
     * Determines if a diagonally attacking threat has the given king in check
     * 
     * @param kingPos The position of the king to check
     * @param teamColor The team of the given king
     * @return True if the king is in check from a piece diagonal to it
     */
    private boolean checkThreatDiagonal(ChessPosition kingPos, TeamColor teamColor) {
        for (int h = -1; h <= 1; h += 2) {
            for (int v = -1; v <= 1; v += 2) {
                for (int col = kingPos.getColumn() + h, row = kingPos.getRow() + v;
                    col > 0 && col <= ChessBoard.BOARD_SIZE && row > 0 && row <= ChessBoard.BOARD_SIZE;
                    col += h, row += v) {
                        ChessPosition diaPosition = new ChessPosition(row, col);
                        ChessPiece potentialThreat = gameBoard.getPiece(diaPosition);
                        // Either return true or break
                        if (potentialThreat != null) {
                            if (potentialThreat.getTeamColor() != teamColor) {
                                PieceType threatPieceType = potentialThreat.getPieceType();
                                if (threatPieceType == PieceType.BISHOP || threatPieceType == PieceType.QUEEN) {
                                    return true;
                                }
                                if (threatPieceType == PieceType.PAWN) {
                                    // Checking if the kingPos is a destination of one of the pawn's pieceMoves
                                    Collection<ChessMove> pawnMoves = potentialThreat.pieceMoves(gameBoard, diaPosition);
                                    for (ChessMove move_m : pawnMoves) {
                                        if (kingPos == move_m.getEndPosition()) {
                                            return true;
                                        }
                                    }
                                }
                            }
                            break;
                        }
                    }
            }
        }
        return false;
    }

    /**
     * Determines if a chess piece is a knight belonging to the other team
     * 
     * @param potentialKnight A chess piece that may be null
     * @param teamColor The friendly team, not the opponent
     * @return
     */
    private boolean threatIsKnight(ChessPiece potentialKnight, TeamColor teamColor) {
        if (potentialKnight != null && potentialKnight.getTeamColor() != teamColor) {
            if (potentialKnight.getPieceType() == PieceType.KNIGHT) {
                return true;
            }
        }
        return false;
    }

    /**
     * Determines if a knight has the given king in check
     * 
     * @param kingPos The position of the king to check
     * @param teamColor The team of the given king
     * @return True if the king is in check because a knight
     */
    private boolean checkThreatKnight(ChessPosition kingPos, TeamColor teamColor) {
        int col = kingPos.getColumn();
        int row = kingPos.getRow();
        if (col < ChessBoard.BOARD_SIZE) {
            if (row < ChessBoard.BOARD_SIZE - 1) {
                ChessPiece potentialThreat = gameBoard.getPiece(new ChessPosition(row + 2, col + 1));
                if (threatIsKnight(potentialThreat, teamColor)) {
                    return true;
                }
            }
        }
        return false;
    }
    
    /**
     * Determines if the given team is in check
     *
     * @param teamColor which team to check for check
     * @return True if the specified team is in check
     */
    public boolean isInCheck(TeamColor teamColor) {
        // Do this first since it is required to determine valid moves
        //
        // There are two ways to solve this problem:
        // Method 1: Check possible moves of opponent's pieces
        // Method 2:
        //      From the this king's perspective, can any piece attack it?
        //      This would require all diagonal, horizontal, and vertical,
        //      paths to be checked from the perspective of the king.
        //      While checking a path, if an opponent's piece is found,
        //          then we check if that piece can attack along that path.
        //          We can then go to check the other direction of that
        //          path or check a different path.
        //      While checking the path, if a friendly piece is found,
        //          We can move to the next path.
        //      Addtionally, we must check the maximum of 8 possible knight positions surrounding the king.
        //          If the opponent has a knight there, the king is in check.
        //
        // We'll go with METHOD 2 because it is more scalable and efficient. Although it is more difficult to implement.

        // Get the king position.
        ChessPosition kingPos = gameBoard.getKingPos(teamColor);
        boolean isInCheck = checkThreatHorizontal(kingPos, teamColor) || checkThreatVertical(kingPos, teamColor)
                            || checkThreatDiagonal(kingPos, teamColor) || checkThreatKnight(kingPos, teamColor);
        return isInCheck;
    }
    
    /**
     * Determines if the given team is in checkmate
     *
     * @param teamColor which team to check for checkmate
     * @return True if the specified team is in checkmate
     */
    public boolean isInCheckmate(TeamColor teamColor) {
        throw new RuntimeException("Not implemented");
    }

    /**
     * Determines if the given team is in stalemate, which here is defined as having
     * no valid moves
     *
     * @param teamColor which team to check for stalemate
     * @return True if the specified team is in stalemate, otherwise false
     */
    public boolean isInStalemate(TeamColor teamColor) {
        throw new RuntimeException("Not implemented");
    }

    /**
     * Sets this game's chessboard with a given board
     *
     * @param board the new board to use
     */
    public void setBoard(ChessBoard board) {
        gameBoard = board;
    }

    /**
     * Gets the current chessboard
     *
     * @return the chessboard
     */
    public ChessBoard getBoard() {
        return gameBoard;
    }
}
