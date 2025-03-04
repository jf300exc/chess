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
    public static final int WHITE_DIRECTION = 1;
    public static final int BLACK_DIRECTION = -1;

    private final ChessGame.TeamColor pieceColor;
    private PieceType type;

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
    }

    /**
     * @return which type of chess piece this piece is
     */
    public PieceType getPieceType() {
        return type;
    }

    /**
     * Changes the pieces type. Useful for promotions
     *
     * @param pieceType The new type of piece to set
     */
    public void setPieceType(PieceType pieceType) {
        this.type = pieceType;
    }

    /**
     * Helper for adding moves in horizontal, straight, or diagonal lines.
     * Adds the move to parameter `moves` if destination is not a friendly position.
     * Returns true if the destination is blocking source piece from moving past it.
     * Also returns true for blocking if the move type is not extended.
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
            return !extended; // If extended then the move won't default to blocking.
        }
        if (threatenedPiece.getTeamColor() != pieceColor) {
            // If opponent's piece, then it's a valid destination.
            ChessMove newMove = new ChessMove(myPosition, potentialPosition, null);
            moves.add(newMove);
        }
        // Opponent's piece or Friendly piece is always blocking
        return true;
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
                for (int newC = myPosition.getColumn() + ci, newR = myPosition.getRow() + ri;
                    newR > 0 && newC > 0 && newR <= ChessBoard.BOARD_SIZE && newC <= ChessBoard.BOARD_SIZE;
                    newC += ci, newR += ri) {
                    if (addMoveAndCheckBlock(board, myPosition, extended, moves, newR, newC)) {
                        break;
                    }
                }
            }
        }
        return moves;
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
    }

    /**
     * Determines if a pawn move is valid.
     *
     * @param potentialPosition The position in range of the pawn that it may be able to move to
     * @param isAttack Determines if an attack (diagonal) move or a straight move.
     * @return True if the move is legal.
     */
    private boolean isPawnMoveLegal(ChessBoard board, ChessPosition potentialPosition, boolean isAttack) {
        ChessPiece threatenedPiece = board.getPiece(potentialPosition);
        // Check if attack is legal.
        if (isAttack) {
            if (threatenedPiece != null) {
                return threatenedPiece.getTeamColor() != pieceColor;
            } else {
                return potentialPosition.equals(board.getEnPassant(pieceColor));
            }
        }
        // If not attack, check if not blocked.
        return threatenedPiece == null;
    }

    /**
     * Assumes that destinationRow and destinationColumn are within pawn movement range.
     * Adds a pawn move if legal. Returns true if it did.
     *
     * @param board To see other position on the board.
     * @param moves The HashSet of moves to add the move to.
     * @param myPosition The position of the source piece.
     * @param destinationRow The potential destination row.
     * @param destinationColumn The potential destination column.
     * @param isAttack Determines if and only if an opponent should be present to be valid.
     * @return True if move was added.
     */
    private boolean pieceAddPawnMove(ChessBoard board, HashSet<ChessMove> moves, ChessPosition myPosition, int destinationRow, int destinationColumn, boolean isAttack) {
        ChessPosition potentialPosition = new ChessPosition(destinationRow, destinationColumn);
        if (isPawnMoveLegal(board, potentialPosition, isAttack)) {
            ChessMove newMove;
            if (destinationRow == ChessBoard.BLACK_ROW || destinationRow == ChessBoard.WHITE_ROW) {
                // Promote
                newMove = new ChessMove(myPosition, potentialPosition, PieceType.QUEEN);
                moves.add(newMove);
                newMove = new ChessMove(myPosition, potentialPosition, PieceType.BISHOP);
                moves.add(newMove);
                newMove = new ChessMove(myPosition, potentialPosition, PieceType.ROOK);
                moves.add(newMove);
                newMove = new ChessMove(myPosition, potentialPosition, PieceType.KNIGHT);
            } else {
                newMove = new ChessMove(myPosition, potentialPosition, null);
                int diff = myPosition.getRow() - destinationRow;
                int mask = diff >> (Integer.SIZE - 1);
                if (((diff + mask) ^ mask) > 1){
                    newMove.setDoublePawnMove();
                }
            }
            moves.add(newMove);
            return true;
        }
        return false;
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
        int row = myPosition.getRow();
        int column = myPosition.getColumn();
        int direction;
        int startingRow;
        int boundaryRow;
        if (pieceColor == ChessGame.TeamColor.WHITE) {
            direction = WHITE_DIRECTION;
            startingRow = ChessBoard.WHITE_PAWN_ROW;
            boundaryRow = ChessBoard.BLACK_ROW; // Black starts at edge of board.
        } else {
            direction = BLACK_DIRECTION;
            startingRow = ChessBoard.BLACK_PAWN_ROW;
            boundaryRow = ChessBoard.WHITE_ROW; // White starts at edge of board.
        }

        if ((direction == BLACK_DIRECTION && row > boundaryRow) || (direction == WHITE_DIRECTION && row < boundaryRow)) {
            if (pieceAddPawnMove(board, moves, myPosition, row + direction, column, false)) {
                if (row == startingRow) {
                    pieceAddPawnMove(board, moves, myPosition, row + 2 * direction, column, false);
                }
            }
            if (column > 1) {
                pieceAddPawnMove(board, moves, myPosition, row + direction, column - 1, true);
            }
            if (column < ChessBoard.BOARD_SIZE) {
                pieceAddPawnMove(board, moves, myPosition, row + direction, column + 1, true);
            }
        }
        return moves;
    }

    /**
     * Assumes that the piece at myPosition is a king. Does not check for check.
     *
     * @param board To see other positions on the board
     * @param myPosition Reference for finding moves
     * @return The castling moves if there are space
     */
    private HashSet<ChessMove> pieceAddCastling(ChessBoard board, ChessPosition myPosition) {
        HashSet<ChessMove> moves = new HashSet<>();
        int row = myPosition.getRow();
        if ((pieceColor == ChessGame.TeamColor.WHITE && row != ChessBoard.WHITE_ROW) ||
                (pieceColor == ChessGame.TeamColor.BLACK && row != ChessBoard.BLACK_ROW)) {
            return moves;
        }
        // Add castling only if there is empty spaces until the rook
        ChessBoard.CastleType castleType = ChessBoard.CastleType.QUEEN_SIDE;
        boolean blocked = true;
        for (int h = -1; h <= 1; h += 2, castleType = ChessBoard.CastleType.KING_SIDE) {
            if (!board.getCastleStatus(pieceColor, castleType)) {
                continue;   // Skip if the castling move is not available
            }
            for (int col = myPosition.getColumn() + h; col > 1 && col <= ChessBoard.BOARD_SIZE - 1; col += h) {
                blocked = false;
                if (board.getPiece(new ChessPosition(row, col)) != null) {
                    blocked = true;
                    break;  // No need to continue
                }
            }
            if (!blocked) {
                ChessPosition destination = new ChessPosition(row, myPosition.getColumn() + 2*h);
                moves.add(new ChessMove(myPosition, destination, null));
            }
        }
        return moves;
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
                moves.addAll(pieceAddCastling(board, myPosition));
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
    }
}
