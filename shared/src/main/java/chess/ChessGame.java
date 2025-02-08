package chess;

import java.util.Collection;
import java.util.Set;
import java.util.HashSet;
import java.util.Iterator;

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
     * Switches current turn to the other team
     */
    private void toggleTeamTurn() {
        if (teamTurn == TeamColor.WHITE) {
            teamTurn = TeamColor.BLACK;
        } else {
            teamTurn = TeamColor.WHITE;
        }
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
        TeamColor pieceTeam = chosenPiece.getTeamColor();
        HashSet<ChessMove> allMoves = new HashSet<>(chosenPiece.pieceMoves(gameBoard, startPosition));
 
        Iterator<ChessMove> iterator = allMoves.iterator();
        while (iterator.hasNext()) {
            ChessMove move = iterator.next();
            ChessPosition moveDest = move.getEndPosition();

            // Make the move
            gameBoard.removePiece(startPosition);
            // If there is a piece at the destination, save it
            ChessPiece destPiece = gameBoard.getPiece(moveDest);
            gameBoard.addPiece(moveDest, chosenPiece);

            // Safely remove if in check
            if (isInCheck(pieceTeam)) {
                iterator.remove();
            }

            // Move back
            gameBoard.removePiece(moveDest);
            // Add back saved destination piece
            gameBoard.addPiece(moveDest, destPiece);
            // Add back moving piece
            gameBoard.addPiece(startPosition, chosenPiece);
        }
        castleInvalidate(chosenPiece, startPosition, allMoves);
        gameBoard.cleanBoard(); // Remove keys that store null
        return allMoves;
    }

    /**
     * Removes a castling move if the adjacent move no longer exists. King Passes through check.
     * @param movingPiece The piece that should be a king. If not a king the function returns early
     * @param startPosition The starting position of the king. Useful for referring to the adjacent locations.
     *                      This is not checked for starting board location because adding
     *                      the move is handled elsewhere.
     * @param allMoves All the moves that passed the validMoves filter
     */
    private void castleInvalidate(ChessPiece movingPiece, ChessPosition startPosition, Collection<ChessMove> allMoves) {
        if (movingPiece.getPieceType() != PieceType.KING) {
            return;
        }
        int row = startPosition.getRow();
        int col = startPosition.getColumn();
        ChessPosition castleKingSidePos = new ChessPosition(row, col + 2);
        ChessMove castleKing = new ChessMove(startPosition, castleKingSidePos, null);
        ChessMove adjKingSide = new ChessMove(startPosition, new ChessPosition(row , col + 1), null);
        ChessPosition castleQueenSidePos = new ChessPosition(row, col - 2);
        ChessMove castleQueen = new ChessMove(startPosition, castleQueenSidePos, null);
        ChessMove adjQueenSide = new ChessMove(startPosition, new ChessPosition(row , col - 1), null);
        if (allMoves.contains(castleKing) && !allMoves.contains(adjKingSide)) {
            allMoves.remove(castleKing);
        }
        if (allMoves.contains(castleQueen) && !allMoves.contains(adjQueenSide)) {
            allMoves.remove(castleQueen);
        }
    }

    /**
     * Adds an En passant option to the board if it becomes available because of `move`.
     * Assumes that `move` exists in `availableMoves`.
     *
     * @param move The move that may be a double move for pawn
     * @param availableMoves Assumes not null. The moves that the pawn could take legally.
     */
    private void checkForEnPassant(ChessMove move, Collection<ChessMove> availableMoves) {
        // Loop over the collection, when move is equal to one of the available moves
        //      use the available move to check for a double move
        for (ChessMove storedMove : availableMoves) {
            if (storedMove.equals(move) && storedMove.isDoublePawnMove()) {
                ChessPosition endPosition = move.getEndPosition();
                int enPassantRow = endPosition.getRow();
                if (teamTurn == TeamColor.WHITE) {
                    enPassantRow = enPassantRow - ChessPiece.WHITE_DIRECTION;
                } else {
                    enPassantRow = enPassantRow - ChessPiece.BLACK_DIRECTION;
                }
                gameBoard.setEnPassant(new ChessPosition(enPassantRow, endPosition.getColumn()), teamTurn);
                break;  // No need to continue
            }
        }
    }

    /**
     * Makes a move in a chess game
     *
     * @param move chess move to preform
     * @throws InvalidMoveException if move is invalid
     */
    public void makeMove(ChessMove move) throws InvalidMoveException {
        ChessPosition startPosition = move.getStartPosition();
        ChessPiece movingPiece = gameBoard.getPiece(startPosition);
        if (movingPiece == null) {
            throw new InvalidMoveException("Starting position does not hold a piece");
        } else if (movingPiece.getTeamColor() != teamTurn) {
            throw new InvalidMoveException("Piece does not belong to current player");
        }

        Collection<ChessMove> availableMoves = validMoves(startPosition);
        if(availableMoves != null && availableMoves.contains(move)) {
            gameBoard.removePiece(startPosition);
            ChessPiece.PieceType promo = move.getPromotionPiece();

            PieceType mPieceType = movingPiece.getPieceType();
            if (promo != null) {
                movingPiece.setPieceType(promo);
            } else if (mPieceType == PieceType.KING) {
                // Perform additional steps if this is a castling move
                if (move.isCastleMove(mPieceType)) {
                    gameBoard.moveCastleRook(move, teamTurn);
                }
                // Set remove both castling options
                gameBoard.setCastleStatus(teamTurn, ChessBoard.CastleType.KING_SIDE, false);
                gameBoard.setCastleStatus(teamTurn, ChessBoard.CastleType.KING_SIDE, false);
            } else if (mPieceType == PieceType.ROOK) {
                // Remove the castling option for just this rook
                int startRow = startPosition.getRow();
                if (startRow == ChessBoard.BOARD_SIZE) {
                    gameBoard.setCastleStatus(teamTurn, ChessBoard.CastleType.KING_SIDE, false);
                } else if (startRow == 1) {
                    gameBoard.setCastleStatus(teamTurn, ChessBoard.CastleType.QUEEN_SIDE, false);
                }
            } else if (mPieceType == PieceType.PAWN) {
                checkForEnPassant(move, availableMoves);
                if (move.getEndPosition().equals(gameBoard.getEnPassant(teamTurn))) {
                    gameBoard.captureEnPassant(teamTurn);
                }
            }
            gameBoard.addPiece(move.getEndPosition(), movingPiece);
            gameBoard.clearEnPassant(teamTurn);
            toggleTeamTurn();

        } else {
            throw new InvalidMoveException("This move is not available");
        }
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
                        if (threatPieceType == PieceType.KING) {
                            // Checking if the kingPos is a destination of one of the pawn's pieceMoves
                            Collection<ChessMove> limitedMoves = potentialThreat.pieceMoves(gameBoard, horizPosition);
                            for (ChessMove smallMove : limitedMoves) {
                                if (kingPos.equals(smallMove.getEndPosition())) {
                                    return true;
                                }
                            }
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
                        if (threatPieceType == PieceType.KING) {
                            // Checking if the kingPos is a destination of one of the pawn's pieceMoves
                            Collection<ChessMove> limitedMoves = potentialThreat.pieceMoves(gameBoard, verPosition);
                            for (ChessMove smallMove : limitedMoves) {
                                if (kingPos.equals(smallMove.getEndPosition())) {
                                    return true;
                                }
                            }
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
                                if (threatPieceType == PieceType.PAWN || threatPieceType == PieceType.KING) {
                                    // Checking if the kingPos is a destination of one of the pawn's pieceMoves
                                    Collection<ChessMove> limitedMoves = potentialThreat.pieceMoves(gameBoard, diaPosition);
                                    for (ChessMove smallMove : limitedMoves) {
                                        if (kingPos.equals(smallMove.getEndPosition())) {
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
            return row > 1 && threatIsKnight(row - 1, col - 2, teamColor);
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
     * Determines if a team has no moves available
     *
     * @param teamColor The team to check for moves
     * @return True if there are no moves available
     */
    private boolean isNoTurnPossible(TeamColor teamColor) {
        Set<ChessPosition> positions = gameBoard.getAllPositions();
        for (ChessPosition pos : positions) {
            if (gameBoard.getPiece(pos).getTeamColor() == teamColor) {
                if (!validMoves(pos).isEmpty()) {
                    return false;
                }
            }
        }
        return true;
    }
    
    /**
     * Determines if the given team is in checkmate
     *
     * @param teamColor which team to check for checkmate
     * @return True if the specified team is in checkmate
     */
    public boolean isInCheckmate(TeamColor teamColor) {
        return isInCheck(teamColor) && isNoTurnPossible(teamColor);
    }

    /**
     * Determines if the given team is in stalemate, which here is defined as having
     * no valid moves
     *
     * @param teamColor which team to check for stalemate
     * @return True if the specified team is in stalemate, otherwise false
     */
    public boolean isInStalemate(TeamColor teamColor) {
        return !isInCheck(teamColor) && isNoTurnPossible(teamColor);
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
