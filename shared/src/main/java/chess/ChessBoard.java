package chess;

import java.util.*;

import chess.ChessPiece.PieceType;
import chess.ChessGame.TeamColor;

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
    private final Map<TeamColor, Map<CastlePieceTypes, Map<CastleType, Boolean>>> castleRequirements = new EnumMap<>(TeamColor.class);

    private ChessPosition whiteKingPos;
    private ChessPosition blackKingPos;
    private ChessPosition enPassantWhite;
    private ChessPosition enPassantBlack;

    public ChessBoard() {
        // Initialize the Castle map for both teams
        for (TeamColor teamColor : TeamColor.values()) {
            Map<CastlePieceTypes, Map<CastleType, Boolean>> pieceTypeMap = new EnumMap<>(CastlePieceTypes.class);
            castleRequirements.put(teamColor, pieceTypeMap);

            // Each piece type will initialize with false values
            for (CastlePieceTypes pieceType : CastlePieceTypes.values()) {
                Map<CastleType, Boolean> castleTypeMap = new EnumMap<>(CastleType.class);
                castleTypeMap.put(CastleType.KING_SIDE, false);
                castleTypeMap.put(CastleType.QUEEN_SIDE, false);
                pieceTypeMap.put(pieceType, castleTypeMap);
            }
        }
    }

    public Map<ChessPosition, ChessPiece> getBoardMap() {
        return board;
    }

    public void addPieceMidGame(ChessPosition position, ChessPiece piece) {
        if (piece == null) {
            return;
        }
        board.put(position, piece);

        // Update the position for the king if it moved
        if (piece.getPieceType() == PieceType.KING) {
            setKingPos(position, piece.getTeamColor());
        }
    }

    /**
     * Adds a chess piece to the chessboard
     *
     * @param position where to add the piece to
     * @param piece    the piece to add
     */
    public void addPiece(ChessPosition position, ChessPiece piece) {
        if (piece == null) {
            return;
        }
        addPieceMidGame(position, piece);

        TeamColor teamColor = piece.getTeamColor();
        PieceType pieceType = piece.getPieceType();
        int row = position.getRow();
        int col = position.getColumn();
        if (pieceType == PieceType.ROOK) {
            meetCastleRequirement(teamColor, CastlePieceTypes.ROOK, row, col);
        } else if (pieceType == PieceType.KING) {
            meetCastleRequirement(teamColor, CastlePieceTypes.KING, row, col);
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

    public enum CastlePieceTypes {
        KING,
        ROOK
    }

    /**
     * The type of castle move
     */
    public enum CastleType {
        KING_SIDE,
        QUEEN_SIDE
    }

    /**
     * Stores any castle requirement that is met by having a piece in the correct location
     *
     * @param teamColor The team of the piece in question
     * @param pieceType The type of the piece in question (either a Rook or King)
     * @param row The row the piece is in
     * @param col The col the piece is in
     */
    private void meetCastleRequirement(ChessGame.TeamColor teamColor, CastlePieceTypes pieceType, int row, int col) {
        int teamRow;
        if (teamColor == TeamColor.WHITE) {
            teamRow = WHITE_ROW;
        } else {
            teamRow = BLACK_ROW;
        }
        if (row != teamRow) {
            return;
        }

        if (pieceType == CastlePieceTypes.KING) {
            if (col == KING_COL) {
                castleRequirements.get(teamColor).get(pieceType).put(CastleType.KING_SIDE, true);
                castleRequirements.get(teamColor).get(pieceType).put(CastleType.QUEEN_SIDE, true);
            }
        } else if (pieceType == CastlePieceTypes.ROOK) {
            if (col == ROOK_1_COL) {
                castleRequirements.get(teamColor).get(pieceType).put(CastleType.QUEEN_SIDE, true);
            } else if (col == ROOK_2_COL) {
                castleRequirements.get(teamColor).get(pieceType).put(CastleType.KING_SIDE, true);
            }
        }
    }

    /**
     * @param teamColor The team to check for castle status
     * @return True if this team still has a castle move
     */
    public boolean getCastleStatus(ChessGame.TeamColor teamColor, CastleType castleType) {
        boolean isKing = castleRequirements.get(teamColor).get(CastlePieceTypes.KING).get(castleType);
        boolean isRook = castleRequirements.get(teamColor).get(CastlePieceTypes.ROOK).get(castleType);
        return isKing && isRook;
    }

    /**
     * Changes the castling capability for a given team
     *
     * @param teamColor The team to address
     * @param enable If true, enables castling, otherwise disables it
     */
    public void setCastleStatus(ChessGame.TeamColor teamColor, CastlePieceTypes castlePiece, CastleType castleType, boolean enable) {
        castleRequirements.get(teamColor).get(castlePiece).put(castleType, enable);
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
        addPieceMidGame(newLocation, movingPiece);
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
        return Objects.equals(board, that.board) && Objects.equals(whiteKingPos, that.whiteKingPos) &&
                Objects.equals(blackKingPos, that.blackKingPos) &&
                Objects.equals(enPassantWhite, that.enPassantWhite) &&
                Objects.equals(enPassantBlack, that.enPassantBlack) &&
                Objects.equals(castleRequirements, that.castleRequirements);
    }

    @Override
    public int hashCode() {
        return Objects.hash(board, whiteKingPos, blackKingPos, enPassantWhite, enPassantBlack, castleRequirements);
    }
}
