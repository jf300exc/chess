package chess;

import java.util.Objects;

/**
 * Represents a single square position on a chess board
 * <p>
 * Note: You can add to this class, but you may not alter
 * signature of the existing methods.
 */
public class ChessPosition {
    private final int row;
    private final int col;

    public ChessPosition(int row, int col) {
        if (row < 1 || row > ChessBoard.BOARD_SIZE) {
            throw new IndexOutOfBoundsException("row: " + row + ", col: " + col);
        }
        this.row = row;
        this.col = col;
    }

    /**
     * @return which row this position is in
     * 1 codes for the bottom row
     */
    public int getRow() {
        return row;
    }

    /**
     * @return which column this position is in
     * 1 codes for the left row
     */
    public int getColumn() {
        return col;
    }

    @Override
    public String toString() {
        String colStr;
        switch (this.col) {
            case 1 -> colStr = "a";
            case 2 -> colStr = "b";
            case 3 -> colStr = "c";
            case 4 -> colStr = "d";
            case 5 -> colStr = "e";
            case 6 -> colStr = "f";
            case 7 -> colStr = "g";
            case 8 -> colStr = "h";
            default -> colStr = Integer.toString(this.col);
        }
        return colStr + row;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ChessPosition that = (ChessPosition) o;
        return row == that.row && col == that.col;
    }

    @Override
    public int hashCode() {
        return Objects.hash(row, col);
    }
}
