package chess;

import java.util.*;

import chess.ChessPiece.PieceType;

import javax.smartcardio.TerminalFactory;

/**
 * For a class that can manage a chess game, making moves on a board
 * <p>
 * Note: You can add to this class, but you may not alter
 * signature of the existing methods.
 */
public class ChessGame {
    private ChessBoard gameBoard = new ChessBoard();
    private TeamColor teamTurn = TeamColor.WHITE;
    private boolean gameOver = false;

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

    public boolean isGameOver() {
        return gameOver;
    }

    public void setGameOver(boolean gameOver) {
        this.gameOver = gameOver;
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
            gameBoard.addPieceMidGame(moveDest, chosenPiece);

            // Safely remove if in check
            if (isInCheck(pieceTeam)) {
                iterator.remove();
            }

            // Move back
            gameBoard.removePiece(moveDest);
            // Add back saved destination piece
            gameBoard.addPieceMidGame(moveDest, destPiece);
            // Add back moving piece
            gameBoard.addPieceMidGame(startPosition, chosenPiece);
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
        if (gameOver) {
            throw new InvalidMoveException("Game over");
        }
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
                ChessBoard.CastleType castleType = move.isCastleMove(mPieceType);
                if (castleType != null) {
                    gameBoard.moveCastleRook(move, teamTurn);
                    // Rook moves, make sure that it is not seen as in its valid position anymore
                    // (needed to compare boards properly)
                    gameBoard.setCastleStatus(teamTurn, ChessBoard.CastlePieceTypes.ROOK, castleType, false);
                }
                // Remove castling options from the king and from the rook that moved, only these because it will
                // help match some random board in the test cases.
                gameBoard.setCastleStatus(teamTurn, ChessBoard.CastlePieceTypes.KING, ChessBoard.CastleType.KING_SIDE, false);
                gameBoard.setCastleStatus(teamTurn, ChessBoard.CastlePieceTypes.KING, ChessBoard.CastleType.QUEEN_SIDE, false);
            } else if (mPieceType == PieceType.ROOK) {
                // Remove the castling option for just this rook
                int startCol = startPosition.getColumn();
                if (startCol == ChessBoard.BOARD_SIZE) {
                    gameBoard.setCastleStatus(teamTurn, ChessBoard.CastlePieceTypes.ROOK, ChessBoard.CastleType.KING_SIDE, false);
                } else if (startCol == 1) {
                    gameBoard.setCastleStatus(teamTurn, ChessBoard.CastlePieceTypes.ROOK, ChessBoard.CastleType.QUEEN_SIDE, false);
                }
            } else if (mPieceType == PieceType.PAWN) {
                checkForEnPassant(move, availableMoves);
                if (move.getEndPosition().equals(gameBoard.getEnPassant(teamTurn))) {
                    gameBoard.captureEnPassant(teamTurn);
                }
            }
            gameBoard.addPieceMidGame(move.getEndPosition(), movingPiece);
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
                Boolean inCheck = helperCheckThreat(kingPos, teamColor, row, col);
                if (inCheck == null) {
                    continue;
                }
                if (inCheck) {
                    return true;
                }
                break;
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
                Boolean inCheck = helperCheckThreat(kingPos, teamColor, row, col);
                if (inCheck == null) {
                    continue;
                }
                if (inCheck) {
                    return true;
                }
                break;
            }
        }
        return false;
    }

    /**
     * Helper for determining if a horizontally or vertically attacking piece puts a king in check.
     * Only for horizontal or vertical threats. Not pawns, bishops, or knights.
     *
     * @param kingPos The position of the king to check
     * @param teamColor The team of the given king
     * @param row The row to currently check
     * @param col The column to currently check
     * @return True if there is a piece which puts the king in check, false is a piece is found, null otherwise
     */
    private Boolean helperCheckThreat(ChessPosition kingPos, TeamColor teamColor, int row, int col) {
        ChessPosition threatPosition = new ChessPosition(row, col);
        ChessPiece potentialThreat = gameBoard.getPiece(threatPosition);
        if (potentialThreat != null) {
            if (potentialThreat.getTeamColor() != teamColor) {
                PieceType threatPieceType = potentialThreat.getPieceType();
                if (threatPieceType == PieceType.ROOK || threatPieceType == PieceType.QUEEN) {
                    return true;
                }
                return checkThreatByThreatMoves(potentialThreat, threatPieceType, false, kingPos, threatPosition);
            }
            return false;
        }
        return null;
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
                    Boolean inCheck = helperCheckThreatDiagonal(kingPos, teamColor, row, col);
                    if (inCheck == null) {
                        continue;
                    }
                    if (inCheck) {
                        return true;
                    }
                    break;
                }
            }
        }
        return false;
    }

    /**
     * Helper for determining if a diagonally attacking piece puts a king in check.
     * Only for diagonal threats. Not rook or knights.
     *
     * @param kingPos The position of the king to check
     * @param teamColor The team of the given king
     * @param row The row to currently check
     * @param col The column to currently check
     * @return True if there is a piece which puts the king in check, false is a piece is found, null otherwise
     */
    private Boolean helperCheckThreatDiagonal(ChessPosition kingPos, TeamColor teamColor, int row, int col) {
        ChessPosition diaPosition = new ChessPosition(row, col);
        ChessPiece potentialThreat = gameBoard.getPiece(diaPosition);
        // Either return true or break
        if (potentialThreat != null) {
            if (potentialThreat.getTeamColor() != teamColor) {
                PieceType threatPieceType = potentialThreat.getPieceType();
                if (threatPieceType == PieceType.BISHOP || threatPieceType == PieceType.QUEEN) {
                    return true;
                }
                return checkThreatByThreatMoves(potentialThreat, threatPieceType, true, kingPos, diaPosition);
            }
            return false;
        }
        return null;
    }

    /**
     * Checks if a King or a Pawn puts a king in check by checking that piece's available moves
     *
     * @param threatPiece The King or Pawn that may put the king in check
     * @param threatPieceType The type of the threatPiece
     * @param diagonal False means only a king's moves will be checked
     * @param kingPos The position of the king which may be in check
     * @param threatPosition The position of the threatPiece
     * @return True only if the threatPiece can attack the kingPos, else return false
     */
    private Boolean checkThreatByThreatMoves(ChessPiece threatPiece, PieceType threatPieceType, boolean diagonal,
                                             ChessPosition kingPos, ChessPosition threatPosition) {
        if ((diagonal && threatPieceType == PieceType.PAWN) || threatPieceType == PieceType.KING) {
            // Check if the kingPos is a destination of one of threatPiece's pieceMoves
            Collection<ChessMove> limitedMoves = threatPiece.pieceMoves(gameBoard, threatPosition);
            for (ChessMove smallMove : limitedMoves) {
                if (kingPos.equals(smallMove.getEndPosition())) {
                    return true;
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
                Collection<ChessMove> allMoves = validMoves(pos);
                if (allMoves != null && !allMoves.isEmpty()) {
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

    public void setBoard(ChessBoard board) {
        gameBoard = board;
    }

    public ChessBoard getBoard() {
        return gameBoard;
    }

    public ChessGame copy() {
        StringBuilder sb = new StringBuilder();
        sb.append("Copying ChessGame board");
        System.out.println("Copying ChessGame board");
        System.out.println("Copying ChessGame board");
        System.out.println("Copying ChessGame board");
        System.out.println("Copying ChessGame board");
        System.out.println("Copying ChessGame board");
        System.out.println("Copying ChessGame board");
        System.out.println("Copying ChessGame board");
        System.out.println("Copying ChessGame board");
        ChessBoard gameBoard = this.gameBoard.copy();
        ChessGame.TeamColor teamTurn = this.teamTurn;
        boolean gameOver = this.gameOver;

        ChessGame gameCopy = new ChessGame();
        gameCopy.setBoard(gameBoard);
        gameCopy.setTeamTurn(teamTurn);
        gameCopy.setGameOver(gameOver);
        sb.append("Asserting Equals");
        System.out.println(sb);
        assert gameCopy.equals(this);
        System.out.println("Post Insert Equals.");
        return gameCopy;
    }

//    @Override
//    public boolean equals(Object o) {
//        if (o == null || getClass() != o.getClass()) {
//            return false;
//        }
//        ChessGame chessGame = (ChessGame) o;
//        return Objects.equals(gameBoard, chessGame.gameBoard) && teamTurn == chessGame.teamTurn;
//    }
//
//    @Override
//    public int hashCode() {
//        return Objects.hash(gameBoard, teamTurn);
//    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ChessGame chessGame = (ChessGame) o;
        return gameOver == chessGame.gameOver && Objects.equals(gameBoard, chessGame.gameBoard) && teamTurn == chessGame.teamTurn;
    }

    @Override
    public int hashCode() {
        return Objects.hash(gameBoard, teamTurn, gameOver);
    }
}
