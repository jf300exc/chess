package chess;

import java.util.Objects;

/**
 * Represents moving a chess piece on a chessboard
 * <p>
 * Note: You can add to this class, but you may not alter
 * signature of the existing methods.
 */
public class ChessMove {
    private final ChessPosition startPosition;
    private final ChessPosition endPosition;
    private final ChessPiece.PieceType promotionPiece;
    private boolean doublePawnMove = false;

    public ChessMove(ChessPosition startPosition, ChessPosition endPosition,
                     ChessPiece.PieceType promotionPiece) {
        this.startPosition = startPosition;
        this.endPosition = endPosition;
        this.promotionPiece = promotionPiece;
    }

    /**
     * @return ChessPosition of starting location
     */
    public ChessPosition getStartPosition() {
        return startPosition;
    }

    /**
     * @return ChessPosition of ending location
     */
    public ChessPosition getEndPosition() {
        return endPosition;
    }

    /**
     * @return True if this move was set as a double pawn move with `setDoublePawnMove()`
     */
    public boolean isDoublePawnMove() {
        return this.doublePawnMove;
    }

    /**
     * Sets this move as a double pawn move
     */
    public void setDoublePawnMove() {
        this.doublePawnMove = true;
    }

    /**
     * Determines if this move is castling move
     * @param pieceType The type of piece this move belongs to
     * @return True if a castling move, else false
     */
    public boolean isCastleMove(ChessPiece.PieceType pieceType) {
        if (pieceType == ChessPiece.PieceType.KING) {
            int diff = endPosition.getColumn() - startPosition.getColumn();
            return (diff > 1) || (diff < -1);
        }
        return false;
    }

    /**
     * Gets the type of piece to promote a pawn to if pawn promotion is part of this
     * chess move
     *
     * @return Type of piece to promote a pawn to, or null if no promotion
     */
    public ChessPiece.PieceType getPromotionPiece() {
        return promotionPiece;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ChessMove chessMove = (ChessMove) o;
        return Objects.equals(startPosition, chessMove.startPosition) && Objects.equals(endPosition, chessMove.endPosition) && promotionPiece == chessMove.promotionPiece;
    }

    @Override
    public int hashCode() {
        return Objects.hash(startPosition, endPosition, promotionPiece);
    }
}
