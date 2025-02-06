package chess;
import java.util.HashMap;
import java.util.Objects;

import chess.ChessPiece.PieceType;

/**
 * A chessboard that can hold and rearrange chess pieces.
 * <p>
 * Note: You can add to this class, but you may not alter
 * signature of the existing methods.
 */
public class ChessBoard {
    public static final int BLACK_ROW      = 8;
    public static final int BLACK_PAWN_ROW = BLACK_ROW - 1;
    public static final int WHITE_ROW      = 1;
    public static final int WHITE_PAWN_ROW = WHITE_ROW + 1;

    public static final int ROOK_1_COL     = 1;
    public static final int KNIGHT_1_COL   = 2;
    public static final int BISHOP_1_COL   = 3;
    public static final int QUEEN_COL      = 4;
    public static final int KING_COL       = 5;
    public static final int BISHOP_2_COL   = 6;
    public static final int KNIGHT_2_COL   = 7;
    public static final int ROOK_2_COL     = 8;

    public static final int BOARD_SIZE     = 8;

    private final HashMap<ChessPosition, ChessPiece> board = new HashMap<>();

    private ChessPosition whiteKingPos = new ChessPosition(WHITE_ROW, KING_COL);
    private ChessPosition blackKingPos = new ChessPosition(BLACK_ROW, KING_COL);

    public ChessBoard() { }

    /**
     * Adds a chess piece to the chessboard
     *
     * @param position where to add the piece to
     * @param piece    the piece to add
     */
    public void addPiece(ChessPosition position, ChessPiece piece) {
        board.put(position, piece);
        if (piece.getPieceType() == PieceType.KING) {
            setKingPos(position, piece.getTeamColor());
        }
    }

    /**
     * Gets a chess piece on the chessboard
     *
     * @param position The position to get the piece from
     * @return Either the piece at the position, or null if no piece is at that
     * position
     */
    public ChessPiece getPiece(ChessPosition position) {
        return board.get(position);
    }

    /**
     * Sets the position of a king on the board. This is not a move.
     * Only stores the position of a king for this board
     * 
     * @param position The new position
     * @param teamColor The team of the king to set
     */
    private void setKingPos(ChessPosition position, ChessGame.TeamColor teamColor) {
        if (teamColor == ChessGame.TeamColor.WHITE) {
            whiteKingPos = position;
        } else {
            blackKingPos = position;
        }
    }

    /**
     * Gets the position of a king on the board
     * 
     * @param teamColor The team of the king to get
     * @return The position of the king
     */
    public ChessPosition getKingPos(ChessGame.TeamColor teamColor) {
        if (teamColor == ChessGame.TeamColor.WHITE) {
            return whiteKingPos;
        } else {
            return blackKingPos;
        }
    }

    /**
     * Adds Capital pieces (Rooks, Knights, Bishops, Queen, King) belonging
     * to `teamColor` to their starting locations in the board.
     *
     * @param teamColor The team to initialize.
     */
    private void initializeCapitalPieces(ChessGame.TeamColor teamColor) {
        int row;
        if (teamColor == ChessGame.TeamColor.BLACK) {
            row = BLACK_ROW;
        } else {
            row = WHITE_ROW;
        }
        ChessPosition position = new ChessPosition(row, ROOK_1_COL);
        ChessPiece piece = new ChessPiece(teamColor, ChessPiece.PieceType.ROOK);
        addPiece(position, piece);

        position = new ChessPosition(row, KNIGHT_1_COL);
        piece = new ChessPiece(teamColor, ChessPiece.PieceType.KNIGHT);
        addPiece(position, piece);

        position = new ChessPosition(row, BISHOP_1_COL);
        piece = new ChessPiece(teamColor, ChessPiece.PieceType.BISHOP);
        addPiece(position, piece);

        position = new ChessPosition(row, QUEEN_COL);
        piece = new ChessPiece(teamColor, ChessPiece.PieceType.QUEEN);
        addPiece(position, piece);

        position = new ChessPosition(row, KING_COL);
        piece = new ChessPiece(teamColor, ChessPiece.PieceType.KING);
        addPiece(position, piece);

        position = new ChessPosition(row, BISHOP_2_COL);
        piece = new ChessPiece(teamColor, ChessPiece.PieceType.BISHOP);
        addPiece(position, piece);

        position = new ChessPosition(row, KNIGHT_2_COL);
        piece = new ChessPiece(teamColor, ChessPiece.PieceType.KNIGHT);
        addPiece(position, piece);

        position = new ChessPosition(row, ROOK_2_COL);
        piece = new ChessPiece(teamColor, ChessPiece.PieceType.ROOK);
        addPiece(position, piece);
    }

    /**
     * Adds Pawn pieces belonging to `teamColor` to their starting locations
     * in the board.
     *
     * @param teamColor The team to initialize.
     */
    private void initializePawns(ChessGame.TeamColor teamColor) {
        int row;
        if (teamColor == ChessGame.TeamColor.WHITE) {
            row = WHITE_PAWN_ROW;
        } else {
            row = BLACK_PAWN_ROW;
        }
        for (int col = 1; col <= BOARD_SIZE; col++) {
            ChessPosition position = new ChessPosition(row, col);
            ChessPiece piece = new ChessPiece(teamColor, ChessPiece.PieceType.PAWN);
            addPiece(position, piece);
        }
    }

    /**
     * Sets the board to the default starting board
     * (How the game of chess normally starts)
     */
    public void resetBoard() {
        board.clear();
        initializeCapitalPieces(ChessGame.TeamColor.WHITE);
        initializeCapitalPieces(ChessGame.TeamColor.BLACK);
        initializePawns(ChessGame.TeamColor.WHITE);
        initializePawns(ChessGame.TeamColor.BLACK);
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ChessBoard that = (ChessBoard) o;
        return Objects.equals(board, that.board);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(board);
    }
}
