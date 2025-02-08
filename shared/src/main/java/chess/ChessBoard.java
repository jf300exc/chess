package chess;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

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

    private ChessPosition whiteKingPos;
    private ChessPosition blackKingPos;
    private ChessPosition enPassantWhite;
    private ChessPosition enPassantBlack;
    private boolean castWhiteKingSide = true;
    private boolean castWhiteQueenSide = true;
    private boolean castBlackKingSide = true;
    private boolean castBlackQueenSide = true;

    public ChessBoard() { }

    /**
     * Adds a chess piece to the chessboard
     *
     * @param position where to add the piece to
     * @param piece    the piece to add
     */
    public void addPiece(ChessPosition position, ChessPiece piece) {
        board.put(position, piece);
        if (piece != null && piece.getPieceType() == PieceType.KING) {
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
     * Gets all filled positions on the board
     *
     * @return a Set of ChessPosition
     */
    public Set<ChessPosition> getAllPositions() {
        return new HashSet<>(board.keySet());
    }

    /**
     * Removes a piece from the board
     * 
     * @param position The position of the piece to remove
     */
    public void removePiece(ChessPosition position) {
        ChessPiece piece = getPiece(position);
        if (piece != null) {
            if (piece.getPieceType() == PieceType.KING) {
                setKingPos(null, piece.getTeamColor());
            }
            board.remove(position);
        }
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
        }
        return blackKingPos;
    }

    /**
     * Gets the destination where an En passant move is allowed for a team
     *
     * @param teamColor The team which could perform the En passant.
     * @return ChessPosition where a pawn from the other team skipped
     */
    public ChessPosition getEnPassant(ChessGame.TeamColor teamColor) {
        if (teamColor == ChessGame.TeamColor.WHITE) {
            return enPassantWhite;
        }
        return enPassantBlack;
    }

    /**
     * Sets the destination where an En passant move is allowed for a team
     *
     * @param enPassantPosition The position to set
     * @param teamColor The team providing the En Passant move
     *                  (the team which just moved a pawn two spaces forward)
     */
    public void setEnPassant(ChessPosition enPassantPosition, ChessGame.TeamColor teamColor) {
        if (teamColor == ChessGame.TeamColor.WHITE) {
            enPassantBlack = enPassantPosition;
        } else {
            enPassantWhite = enPassantPosition;
        }
    }

    /**
     * Sets the enPassant move for the current turn's team to null
     *
     * @param teamColor The team who just lost any option of taking an enPassant move
     */
    public void clearEnPassant(ChessGame.TeamColor teamColor) {
        if (teamColor == ChessGame.TeamColor.WHITE) {
            enPassantWhite = null;
        } else {
            enPassantBlack = null;
        }
    }

    /**
     * The type of castle move
     */
    public enum CastleType {
        KING_SIDE,
        QUEEN_SIDE
    }

    /**
     * @param teamColor The team to check for castle status
     * @return True if this team still has a castle move
     */
    public boolean getCastleStatus(ChessGame.TeamColor teamColor, CastleType castleType) {
        if (teamColor == ChessGame.TeamColor.WHITE) {
            if (castleType == CastleType.KING_SIDE) {
                return castWhiteKingSide;
            } else {
                return castWhiteQueenSide;
            }
        }
        if (castleType == CastleType.KING_SIDE) {
            return castBlackKingSide;
        }
        return castBlackQueenSide;
    }

    /**
     * Changes the castling capability for a given team
     *
     * @param teamColor The team to address
     * @param enable If true, enables castling, otherwise disables it
     */
    public void setCastleStatus(ChessGame.TeamColor teamColor, CastleType castleType, boolean enable) {
        if (teamColor == ChessGame.TeamColor.WHITE) {
            if (castleType == CastleType.KING_SIDE) {
                castWhiteKingSide = enable;
            } else {
                castWhiteQueenSide = enable;
            }
        } else {
            if (castleType == CastleType.KING_SIDE) {
                castBlackKingSide = enable;
            } else {
                castBlackQueenSide = enable;
            }
        }
    }

    /**
     * Places the rook in its new position as a result of a castling move.
     *
     * @param move The move of the king causing the castle
     * @param teamColor The team of the king causing the castle
     */
    public void moveCastleRook(ChessMove move, ChessGame.TeamColor teamColor) {
        int row;
        if (teamColor == ChessGame.TeamColor.WHITE) {
            row = WHITE_ROW;
        } else {
            row = BLACK_ROW;
        }

        ChessPosition oldLocation;
        ChessPosition newLocation;
        int endPosCol = move.getEndPosition().getColumn();
        int diff = endPosCol - move.getStartPosition().getColumn();
        if (diff > 0) {
            // KingSide
            oldLocation = new ChessPosition(row, BOARD_SIZE);
            newLocation = new ChessPosition(row, endPosCol - 1);
        } else {
            // QueenSide
            oldLocation = new ChessPosition(row, 1);
            newLocation = new ChessPosition(row, endPosCol + 1);
        }
        ChessPiece movingPiece = getPiece(oldLocation);
        removePiece(oldLocation);
        addPiece(newLocation, movingPiece);
    }

    /**
     * Captures a pawn that is correlated with the En Passant move available to this team
     *
     * @param teamColor The team that is capturing
     */
    public void captureEnPassant(ChessGame.TeamColor teamColor) {
        int row;
        int col;
        if (teamColor == ChessGame.TeamColor.WHITE) {
            row = enPassantWhite.getRow() + ChessPiece.BLACK_DIRECTION;
            col = enPassantWhite.getColumn();
        } else {
            row = enPassantBlack.getRow() + ChessPiece.WHITE_DIRECTION;
            col = enPassantBlack.getColumn();
        }
        removePiece(new ChessPosition(row, col));
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

    /**
     * Removes all keys that store null
     */
    public void cleanBoard() {
        board.entrySet().removeIf(entry -> entry.getValue() == null);
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
