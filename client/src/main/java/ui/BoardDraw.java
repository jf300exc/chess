package ui;

import chess.*;
import org.jline.utils.AttributedString;

import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

/** Chess symbols and markers share a centered terminal-column anchor. */
public final class BoardDraw {
    private static final String RESET = "\u001b[0m";
    private static final String LIGHT = "\u001b[48;5;187m";
    private static final String DARK = "\u001b[48;5;65m";
    private static final String SELECTED = "\u001b[48;5;179m";
    private static final String LEGAL = "\u001b[48;5;80m";
    private static final String WHITE = "\u001b[97m";
    private static final String BLACK = "\u001b[30m";

    public record Layout(int cellWidth, int cellHeight) {
        public int width() {
            return 3 + 8 * cellWidth + 3;
        }

        public int height() {
            return 2 + 8 * cellHeight;
        }

        public static Layout forSize(int columns, int rows) {
            if (columns >= 62 && rows >= 34) {
                return new Layout(7, 3);
            }
            if (columns >= 46 && rows >= 24) {
                return new Layout(5, 2);
            }
            return new Layout(3, 1);
        }

        /** Zero-based screen coordinates, relative to the top-left of the board. */
        public ChessPosition positionAt(int x, int y, ChessGame.TeamColor perspective) {
            int column = x - 3;
            int row = y - 1;
            if (column < 0 || column >= cellWidth * 8 || row < 0 || row >= cellHeight * 8) {
                return null;
            }
            return positionForCell(row / cellHeight, column / cellWidth, perspective);
        }
    }

    private BoardDraw() {
    }

    public static String drawBoard(ChessGame game, ChessGame.TeamColor perspective) {
        return draw(game, perspective, null, new Layout(3, 1), true);
    }

    public static String drawBoardWithValidMoves(ChessGame game, ChessGame.TeamColor perspective, ChessPosition start) {
        return draw(game, perspective, start, new Layout(3, 1), true);
    }

    static String draw(ChessGame game, ChessGame.TeamColor perspective, ChessPosition selected, Layout layout, boolean color) {
        return draw(game, perspective, selected, layout, color, PieceSymbols.UNICODE);
    }

    static String draw(ChessGame game, ChessGame.TeamColor perspective, ChessPosition selected, Layout layout,
                       boolean color, PieceSymbols symbols) {
        symbols = symbols.forRendering(color);
        Collection<ChessMove> moves = selected == null ? null : game.validMoves(selected);
        Set<ChessPosition> destinations = moves == null ? Set.of()
                : moves.stream().map(ChessMove::getEndPosition).collect(Collectors.toSet());
        StringBuilder board = new StringBuilder();
        StringBuilder coordinates = new StringBuilder("   ");
        for (int c = 0; c < 8; c++) {
            char file = (char) ('a' + positionForCell(0, c, perspective).getColumn() - 1);
            coordinates.append(center(Character.toString(file), layout.cellWidth));
        }
        board.append(coordinates).append('\n');
        for (int r = 0; r < 8; r++) {
            for (int line = 0; line < layout.cellHeight; line++) {
                boolean labelLine = line == (layout.cellHeight - 1) / 2;
                int rank = positionForCell(r, 0, perspective).getRow();
                board.append(labelLine ? " " + rank + " " : "   ");
                for (int c = 0; c < 8; c++) {
                    ChessPosition position = positionForCell(r, c, perspective);
                    ChessPiece piece = game.getBoard().getPiece(position);
                    boolean isSelected = position.equals(selected);
                    boolean legal = destinations.contains(position);
                    String background = isSelected ? SELECTED : legal ? LEGAL
                            : (position.getRow() + position.getColumn()) % 2 == 1 ? LIGHT : DARK;
                    String label = labelLine ? piece == null ? legal ? "+" : color ? "" : "."
                            : symbols.glyph(piece.getPieceType(), piece.getTeamColor()) : "";
                    int labelWidth = new AttributedString(label).columnLength();
                    if (color) {
                        board.append(background);
                        int left = (layout.cellWidth - labelWidth) / 2;
                        board.append(" ".repeat(left));
                        if (piece != null && labelLine) {
                            // Hollow/filled Unicode shapes distinguish teams without pale white-on-light text.
                            board.append(symbols == PieceSymbols.UNICODE ? BLACK
                                    : piece.getTeamColor() == ChessGame.TeamColor.WHITE ? WHITE : BLACK);
                        } else {
                            board.append("\u001b[30m");
                        }
                        board.append(label).append(RESET).append(background)
                                .append(" ".repeat(layout.cellWidth - left - labelWidth)).append(RESET);
                    } else {
                        board.append(center(label, layout.cellWidth));
                    }
                }
                board.append(labelLine ? " " + rank : "  ").append('\n');
            }
        }
        return board.append(coordinates).toString();
    }

    static ChessPosition positionForCell(int row, int column, ChessGame.TeamColor perspective) {
        return perspective == ChessGame.TeamColor.BLACK
                ? new ChessPosition(row + 1, 8 - column) : new ChessPosition(8 - row, column + 1);
    }

    /** Text gutters only: writing invisible glyphs beneath SIXEL erases image cells on some terminals. */
    static String imageFrame(Layout layout, ChessGame.TeamColor perspective) {
        StringBuilder coordinates = new StringBuilder("   ");
        for (int c = 0; c < 8; c++) {
            coordinates.append(center(Character.toString((char) ('a' + positionForCell(0, c, perspective).getColumn() - 1)),
                    layout.cellWidth()));
        }
        StringBuilder frame = new StringBuilder(coordinates).append('\n');
        for (int row = 0; row < 8; row++) {
            for (int line = 0; line < layout.cellHeight(); line++) {
                boolean label = line == (layout.cellHeight() - 1) / 2;
                int rank = positionForCell(row, 0, perspective).getRow();
                frame.append(label ? " " + rank + " " : "   ")
                        .append(" ".repeat(8 * layout.cellWidth())).append(label ? " " + rank : "  ").append('\n');
            }
        }
        return frame.append(coordinates).toString();
    }

    private static String center(String label, int width) {
        int columns = new AttributedString(label).columnLength();
        int left = (width - columns) / 2;
        return " ".repeat(left) + label + " ".repeat(width - left - columns);
    }
}
