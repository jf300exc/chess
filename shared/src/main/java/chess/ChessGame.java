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

    public ChessGame() {
        gameBoard.resetBoard();
    }

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
        ChessPiece chosenPiece = gameBoard.getPiece(startPosition);
        if (chosenPiece == null) {
            return null;
        }
        Collection<ChessMove> allMoves = chosenPiece.pieceMoves(gameBoard, startPosition);
        
        // Remove the moves that place this piece's king in check
        // Method 'isInCheck' can determine check based on the current state of the board.
        // So essentially, make a move, check for check, remove the move if in check.

        // These moves can't be done through the 'makeMove' method because it depends on this method.
        // Rather, we will manually add the piece to the board and set its old position to null.
        //      This will be efficient and allow the `isInCheck` method to function properly.
        // Since we need to make sure that there are only unique moves (and that removing one move removes all of the same)
        // we'll use a hashSet.
        
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
     * Determines if coordinates contain knight belonging to the other team
     * 
     * @param row The row of the piece to identify
     * @param col The column of the piece to identify
     * @param teamColor The friendly team, not the opponent
     * @return True if coordinates contain a knight from the other team
     */
    private boolean threatIsKnight(int row, int col, TeamColor teamColor) {
        ChessPiece potentialKnight = gameBoard.getPiece(new ChessPosition(row, col));
        if (potentialKnight != null && potentialKnight.getTeamColor() != teamColor) {
            return potentialKnight.getPieceType() == PieceType.KNIGHT;
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
        int size = ChessBoard.BOARD_SIZE;
        if (col < size) {
            if (row < size - 1 && threatIsKnight(row + 2, col + 1, teamColor)) {
                return true;
            }
            if (row > 2 && threatIsKnight(row - 2, col + 1, teamColor)) {
                return true;
            }
        }
        if (col < size - 1) {
            if (row < size && threatIsKnight(row + 1, col + 2, teamColor)) {
                return true;
            }
            if (row > 1 && threatIsKnight(row - 1, col + 2, teamColor)) {
                return true;
            }
        }
        if (col > 1) {
            if (row < size - 1 && threatIsKnight(row + 2, col - 1, teamColor)) {
                return true;
            }
            if (row > 2 && threatIsKnight(row - 2, col - 1, teamColor)) {
                return true;
            }
        }
        if (col > 2) {
            if (row < size && threatIsKnight(row + 1, col - 2, teamColor)) {
                return true;
            }
            if (row > 1 && threatIsKnight(row - 1, col - 2, teamColor)) {
                return true;
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
        ChessPosition kingPos = gameBoard.getKingPos(teamColor);
        return checkThreatHorizontal(kingPos, teamColor) || checkThreatVertical(kingPos, teamColor)
                            || checkThreatDiagonal(kingPos, teamColor) || checkThreatKnight(kingPos, teamColor);
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
