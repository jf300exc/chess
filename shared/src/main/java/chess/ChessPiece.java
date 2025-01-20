package chess;

import java.util.Collection;
import java.util.Objects;
import java.util.HashSet;

/**
 * Represents a single chess piece
 * <p>
 * Note: You can add to this class, but you may not alter
 * signature of the existing methods.
 */
public class ChessPiece {
    private final ChessGame.TeamColor pieceColor;
    private final PieceType type;

    public ChessPiece(ChessGame.TeamColor pieceColor, ChessPiece.PieceType type) {
        this.pieceColor = pieceColor;
        this.type = type;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ChessPiece that = (ChessPiece) o;
        return pieceColor == that.pieceColor && type == that.type;
    }

    @Override
    public int hashCode() {
        return Objects.hash(pieceColor, type);
    }

    /**
     * The various different chess piece options
     */
    public enum PieceType {
        KING,
        QUEEN,
        BISHOP,
        KNIGHT,
        ROOK,
        PAWN
    }

    /**
     * @return Which team this chess piece belongs to
     */
    public ChessGame.TeamColor getTeamColor() {
        return pieceColor;
//        throw new RuntimeException("Not implemented");
    }

    /**
     * @return which type of chess piece this piece is
     */
    public PieceType getPieceType() {
        return type;
//        throw new RuntimeException("Not implemented");
    }

    /**
     * Calculates horizontal moves for a piece, adjacent or extended. Using position of piece as reference.
     * Does not account for moves that place king in check.
     *
     * @param board To see other positions on the board.
     * @param myPosition Reference for finding moves.
     * @param extended Determines if the whole row is checked, or, if false, only adjacent locations.
     * @return HashSet of horizontal moves that are not blocked
     */
    private HashSet<ChessMove> pieceFindHorizontalMoves(ChessBoard board, ChessPosition myPosition, boolean extended) {
        HashSet<ChessMove> moves = new HashSet<>();
        int myRow = myPosition.getRow();
        for (int i = -1; i <= 1; i += 2) {
            // Runs left, then right, using negative and positive increment.
            for (int c = myPosition.getColumn() + i; c > 0 && c <= ChessBoard.BOARD_SIZE; c += i) {
                if (addMoveAndCheckBlock(board, myPosition, extended, moves, myRow, c)) {
                    break;
                }
            }
        }
        return moves;
//        throw new UnsupportedOperationException("Not supported yet.");
    }

    /**
     * Calculates vertical moves for a piece, adjacent or extended. Using position of piece as reference.
     * Does not account for moves that place king in check.
     *
     * @param board To see other positions on the board.
     * @param myPosition Reference for finding moves.
     * @param extended Determines if the whole column is checked, or, if false, only adjacent locations.
     * @return HashSet of vertical moves that are not blocked.
     */
    private HashSet<ChessMove> pieceFindVerticalMoves(ChessBoard board, ChessPosition myPosition, boolean extended) {
        HashSet<ChessMove> moves = new HashSet<>();
        int myColumn = myPosition.getColumn();
        for (int i = -1; i <= 1; i += 2) {
            // Runs down, then up, using negative and positive increment.
            for (int r = myPosition.getRow() + i; r > 0 && r <= ChessBoard.BOARD_SIZE; r += i) {
                if (addMoveAndCheckBlock(board, myPosition, extended, moves, r, myColumn)) {
                    break;
                }
            }
        }
        return moves;
//        throw new UnsupportedOperationException("Not supported yet.");
    }

    /**
     * Helper for adding moves in horizontal, straight, or diagonal lines.
     * Adds the move to parameter `moves` if destination is not a friendly position.
     * Assumes `potentialColumn` and `potentialRow` are within bounds and legal.
     *
     * @param board           The main board to check other pieces.
     * @param myPosition      Required to make ChessMove objects.
     * @param extended        Defaults blocking when `false`
     * @param moves           The reference to add moves to.
     * @param potentialRow    The potential destination row.
     * @param potentialColumn The potential destination column.
     * @return Returns `true` if blocking.
     */
    private boolean addMoveAndCheckBlock(ChessBoard board, ChessPosition myPosition, boolean extended, HashSet<ChessMove> moves, int potentialRow, int potentialColumn) {
        ChessPosition potentialPosition = new ChessPosition(potentialRow, potentialColumn);
        ChessPiece threatenedPiece = board.getPiece(potentialPosition);
        if (threatenedPiece == null) {
            // Empty destinations are valid.
            ChessMove newMove = new ChessMove(myPosition, potentialPosition, null);
            moves.add(newMove);
        } else if (threatenedPiece.getTeamColor() != pieceColor) {
            // If opponents piece, then it's a valid destination.
            ChessMove newMove = new ChessMove(myPosition, potentialPosition, null);
            moves.add(newMove);
            return true;
        } else {
            // This is a friendly piece and is blocking.
            return true;
        }
        return !extended; // If extended then the move won't default to blocking.
    }

    /**
     * Calculates diagonal moves for a piece, adjacent or extended. Using position of piece as reference.
     * Does not account for moves that place king in check.
     *
     * @param board To see other positions on the board.
     * @param myPosition Reference for finding moves.
     * @param extended Determines if extended diagonals are checked, or, if false, only adjacent diagonals.
     * @return HashSet of diagonal moves that are not blocked.
     */
    private HashSet<ChessMove> pieceFindDiagonalMoves(ChessBoard board, ChessPosition myPosition, boolean extended) {
        HashSet<ChessMove> moves = new HashSet<>();
        for (int ci = -1; ci <= 1; ci += 2) {           // Goes left and then right
            for (int ri = -1; ri <= 1; ri += 2) {       // Goes down and then up
                // Initialize with -1 or +1 offsets on both new_c and new_r
                // Check bounds
                // Increment with -1 or +1 on both new_c and new_r
                for (int new_c = myPosition.getColumn() + ci, new_r = myPosition.getRow() + ri;
                     new_r > 0 && new_c > 0 && new_r <= ChessBoard.BOARD_SIZE && new_c <= ChessBoard.BOARD_SIZE;
                     new_c += ci, new_r += ri) {
                    if (addMoveAndCheckBlock(board, myPosition, extended, moves, new_r, new_c)) {
                        break;
                    }
                }
            }
        }
        return moves;
        
//        throw new UnsupportedOperationException("Not supported yet.");
    }

    /**
     * Calculates moves as if the piece is a knight. Invalidating blocked moves.
     * Does not account for moves that place king in check.
     *
     * @param board To see other positions on the board.
     * @param myPosition Reference for finding moves.
     * @return HashSet of knight type moves.
     */
    private HashSet<ChessMove> pieceFindKnightMoves(ChessBoard board, ChessPosition myPosition) {
        HashSet<ChessMove> moves = new HashSet<>();
        int column = myPosition.getColumn();
        int row = myPosition.getRow();
        if (column < ChessBoard.BOARD_SIZE) {
            if (row < ChessBoard.BOARD_SIZE - 1) {
                addMoveAndCheckBlock(board, myPosition, false, moves, row + 2, column + 1);
            }
            if (row > 2) {
                addMoveAndCheckBlock(board, myPosition, false, moves, row - 2, column + 1);
            }
        }
        if (column < ChessBoard.BOARD_SIZE - 1) {
            if (row < ChessBoard.BOARD_SIZE) {
                addMoveAndCheckBlock(board, myPosition, false, moves, row + 1, column + 2);
            }
            if (row > 1) {
                addMoveAndCheckBlock(board, myPosition, false, moves, row - 1, column + 2);
            }
        }
        if (column > 1) {
            if (row < ChessBoard.BOARD_SIZE - 1) {
                addMoveAndCheckBlock(board, myPosition, false, moves, row + 2, column - 1);
            }
            if (row > 2) {
                addMoveAndCheckBlock(board, myPosition, false, moves, row - 2, column - 1);
            }
        }
        if (column > 2) {
            if (row < ChessBoard.BOARD_SIZE) {
                addMoveAndCheckBlock(board, myPosition, false, moves, row + 1, column - 2);
            }
            if (row > 1) {
                addMoveAndCheckBlock(board, myPosition, false, moves, row - 1, column - 2);
            }
        }
        return moves;

//        throw new UnsupportedOperationException("Not supported yet.");
    }

    /**
     * Calculates moves as if the piece is a pawn. Adding double moves if piece is in starting location.
     * Does not account for moves that place king in check.
     *
     * @param board To see other positions on the board.
     * @param myPosition Reference for finding moves.
     * @return HashSet of pawn type moves.
     */
    private HashSet<ChessMove> pieceFindPawnMoves(ChessBoard board, ChessPosition myPosition) {
        HashSet<ChessMove> moves = new HashSet<>();
        ChessPosition potentialPosition;
        ChessMove newMove;
        ChessPiece threatenedPiece;
        int row = myPosition.getRow();
        int column = myPosition.getColumn();
        if (pieceColor == ChessGame.TeamColor.BLACK && row > 1) {
            potentialPosition = new ChessPosition(row - 1, column);
            threatenedPiece = board.getPiece(potentialPosition);
            if (threatenedPiece == null) {
                newMove = new ChessMove(myPosition, potentialPosition, null);
                moves.add(newMove);
                // Check to add double move only if not blocked.
                if (row == ChessBoard.BLACK_PAWN_ROW) {
                    potentialPosition = new ChessPosition(row - 2, column);
                    threatenedPiece = board.getPiece(potentialPosition);
                    if (threatenedPiece == null) {
                        newMove = new ChessMove(myPosition, potentialPosition, null);
                        moves.add(newMove);
                    }
                }
            }
            if (column > 1) {
                potentialPosition = new ChessPosition(row - 1, column - 1);
                threatenedPiece = board.getPiece(potentialPosition);
                if (threatenedPiece != null && threatenedPiece.getTeamColor() != pieceColor) {
                    // If opponent at diagonal, then diagonal is a valid destination.
                    newMove = new ChessMove(myPosition, potentialPosition, null);
                    moves.add(newMove);
                }
            }
            if (column < ChessBoard.BOARD_SIZE) {
                potentialPosition = new ChessPosition(row - 1, column + 1);
                threatenedPiece = board.getPiece(potentialPosition);
                if (threatenedPiece != null && threatenedPiece.getTeamColor() != pieceColor) {
                    // If opponent at diagonal, then diagonal is a valid destination.
                    newMove = new ChessMove(myPosition, potentialPosition, null);
                    moves.add(newMove);
                }
            }
        }
        // TODO: Make this work for White Pieces and clean it up.
        throw new UnsupportedOperationException("Not supported yet.");
    }

    /**
     * Calculates all the positions a chess piece can move to
     * Does not take into account moves that are illegal due to leaving the king in
     * danger
     *
     * @return Collection of valid moves
     */
    public Collection<ChessMove> pieceMoves(ChessBoard board, ChessPosition myPosition) {
        HashSet<ChessMove> moves = new HashSet<>();
        switch (type) {
            case KING:
                moves.addAll(pieceFindHorizontalMoves(board, myPosition, false));
                moves.addAll(pieceFindVerticalMoves(board, myPosition, false));
                moves.addAll(pieceFindDiagonalMoves(board, myPosition, false));
                break;
            case QUEEN:
                moves.addAll(pieceFindHorizontalMoves(board, myPosition, true));
                moves.addAll(pieceFindVerticalMoves(board, myPosition, true));
                moves.addAll(pieceFindDiagonalMoves(board, myPosition, true));
                break;
            case BISHOP:
                moves.addAll(pieceFindDiagonalMoves(board, myPosition, true));
                break;
            case KNIGHT:
                moves.addAll(pieceFindKnightMoves(board, myPosition));
                break;
            case ROOK:
                moves.addAll(pieceFindHorizontalMoves(board, myPosition, true));
                moves.addAll(pieceFindVerticalMoves(board, myPosition, true));
                break;
            case PAWN:
                moves.addAll(pieceFindPawnMoves(board, myPosition));
                break;
            default:
                throw new RuntimeException("Invalid piece type: " + type);
        }
        return moves;
//        throw new RuntimeException("Not implemented");
    }
}
