package ui.gui;

import chess.ChessGame;
import chess.ChessMove;
import chess.ChessPiece;
import chess.ChessPosition;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

/** Scalable, keyboard-free chess board used by the desktop client. */
public class ChessBoardPanel extends JPanel {
    private static final Color LIGHT_SQUARE = new Color(236, 229, 211);
    private static final Color DARK_SQUARE = new Color(91, 132, 105);
    private static final Color BOARD_EDGE = new Color(24, 32, 40);
    private static final Color ACCENT = new Color(242, 177, 74);
    private static final Color LAST_MOVE = new Color(246, 201, 92, 120);
    private static final Color MOVE_DOT = new Color(24, 32, 40, 115);
    private static final Color CAPTURE_RING = new Color(191, 67, 67, 190);

    private ChessGame game;
    private ChessGame.TeamColor playerColor;
    private ChessGame.TeamColor orientation = ChessGame.TeamColor.WHITE;
    private ChessPosition selected;
    private Set<ChessPosition> destinations = Set.of();
    private ChessMove pendingMove;
    private boolean interactive;
    private Consumer<ChessMove> moveHandler = ignored -> { };

    public ChessBoardPanel() {
        setOpaque(true);
        setBackground(BOARD_EDGE);
        setPreferredSize(new Dimension(680, 680));
        setMinimumSize(new Dimension(360, 360));
        setToolTipText("");
        getAccessibleContext().setAccessibleName("Interactive chess board");
        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent event) {
                handleClick(positionAt(event.getPoint()));
            }
        });
    }

    public void setMoveHandler(Consumer<ChessMove> handler) {
        moveHandler = handler == null ? ignored -> { } : handler;
    }

    public void setPosition(ChessGame game, ChessGame.TeamColor playerColor, boolean interactive) {
        this.game = game;
        this.playerColor = playerColor;
        this.interactive = interactive;
        this.pendingMove = null;
        clearSelection();
        repaint();
    }

    public void setOrientation(ChessGame.TeamColor orientation) {
        this.orientation = orientation == null ? ChessGame.TeamColor.WHITE : orientation;
        clearSelection();
        repaint();
    }

    public ChessGame.TeamColor getOrientation() {
        return orientation;
    }

    public void flip() {
        setOrientation(orientation == ChessGame.TeamColor.WHITE
                ? ChessGame.TeamColor.BLACK : ChessGame.TeamColor.WHITE);
    }

    public void acknowledgeMove() {
        pendingMove = null;
        clearSelection();
        repaint();
    }

    @Override
    public String getToolTipText(MouseEvent event) {
        ChessPosition position = positionAt(event.getPoint());
        if (position == null) {
            return null;
        }
        char file = (char) ('a' + position.getColumn() - 1);
        ChessPiece piece = game == null ? null : game.getBoard().getPiece(position);
        String square = "" + file + position.getRow();
        return piece == null ? square : square + " — " + pretty(piece);
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        Graphics2D g = (Graphics2D) graphics.create();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            BoardGeometry geometry = geometry();
            if (geometry.size <= 0) {
                return;
            }

            g.setColor(new Color(0, 0, 0, 55));
            g.fillRoundRect(geometry.x + 9, geometry.y + 12, geometry.size, geometry.size, 12, 12);
            for (int screenRow = 0; screenRow < 8; screenRow++) {
                for (int screenColumn = 0; screenColumn < 8; screenColumn++) {
                    ChessPosition position = positionForScreenSquare(screenRow, screenColumn);
                    int x = geometry.x + screenColumn * geometry.square;
                    int y = geometry.y + screenRow * geometry.square;
                    paintSquare(g, position, screenRow, screenColumn, x, y, geometry.square);
                }
            }
            paintCoordinates(g, geometry);
        } finally {
            g.dispose();
        }
    }

    private void paintSquare(Graphics2D g, ChessPosition position, int screenRow, int screenColumn,
                             int x, int y, int square) {
        boolean light = (position.getRow() + position.getColumn()) % 2 == 1;
        g.setColor(light ? LIGHT_SQUARE : DARK_SQUARE);
        g.fillRect(x, y, square, square);

        if (pendingMove != null && (position.equals(pendingMove.getStartPosition())
                || position.equals(pendingMove.getEndPosition()))) {
            g.setColor(LAST_MOVE);
            g.fillRect(x, y, square, square);
        }
        if (position.equals(selected)) {
            g.setColor(new Color(ACCENT.getRed(), ACCENT.getGreen(), ACCENT.getBlue(), 175));
            g.fillRect(x, y, square, square);
        }

        ChessPiece piece = game == null ? null : game.getBoard().getPiece(position);
        if (destinations.contains(position)) {
            if (piece == null) {
                int dot = Math.max(10, square / 5);
                g.setColor(MOVE_DOT);
                g.fillOval(x + (square - dot) / 2, y + (square - dot) / 2, dot, dot);
            } else {
                g.setColor(CAPTURE_RING);
                g.setStroke(new BasicStroke(Math.max(3f, square / 16f)));
                int inset = Math.max(4, square / 14);
                g.drawOval(x + inset, y + inset, square - inset * 2, square - inset * 2);
            }
        }

        if (piece != null) {
            paintPiece(g, piece, x, y, square);
        }

        if (screenColumn == 0 || screenRow == 7) {
            g.setFont(getFont().deriveFont(Font.BOLD, Math.max(10f, square * .14f)));
            g.setColor(light ? DARK_SQUARE.darker() : LIGHT_SQUARE);
            if (screenColumn == 0) {
                g.drawString(Integer.toString(position.getRow()), x + 5, y + 14);
            }
            if (screenRow == 7) {
                String file = Character.toString((char) ('a' + position.getColumn() - 1));
                FontMetrics fm = g.getFontMetrics();
                g.drawString(file, x + square - fm.stringWidth(file) - 5, y + square - 5);
            }
        }
    }

    private void paintPiece(Graphics2D g, ChessPiece piece, int x, int y, int square) {
        String symbol = symbol(piece);
        Font font = new Font("DejaVu Sans", Font.PLAIN, Math.max(28, (int) (square * .72)));
        g.setFont(font);
        FontMetrics fm = g.getFontMetrics();
        int textX = x + (square - fm.stringWidth(symbol)) / 2;
        int textY = y + (square - fm.getHeight()) / 2 + fm.getAscent() - Math.max(1, square / 30);
        g.setColor(new Color(0, 0, 0, 65));
        g.drawString(symbol, textX + 2, textY + 3);
        g.setColor(piece.getTeamColor() == ChessGame.TeamColor.WHITE
                ? new Color(250, 248, 239) : new Color(28, 34, 40));
        g.drawString(symbol, textX, textY);
    }

    private void paintCoordinates(Graphics2D g, BoardGeometry geometry) {
        g.setFont(getFont().deriveFont(Font.BOLD, 12f));
        g.setColor(new Color(205, 214, 220));
        String perspective = orientation == ChessGame.TeamColor.WHITE ? "WHITE PERSPECTIVE" : "BLACK PERSPECTIVE";
        g.drawString(perspective, geometry.x, geometry.y - 10);
    }

    private void handleClick(ChessPosition position) {
        if (!interactive || game == null || playerColor == null || position == null || pendingMove != null) {
            return;
        }
        ChessPiece piece = game.getBoard().getPiece(position);
        if (selected == null) {
            if (piece != null && piece.getTeamColor() == playerColor) {
                select(position);
            }
            return;
        }
        if (position.equals(selected)) {
            clearSelection();
        } else if (piece != null && piece.getTeamColor() == playerColor) {
            select(position);
        } else if (destinations.contains(position)) {
            pendingMove = new ChessMove(selected, position, null);
            clearSelection();
            moveHandler.accept(pendingMove);
        }
        repaint();
    }

    private void select(ChessPosition position) {
        selected = position;
        Collection<ChessMove> valid = game.validMoves(position);
        Set<ChessPosition> next = new HashSet<>();
        if (valid != null) {
            for (ChessMove move : valid) {
                next.add(move.getEndPosition());
            }
        }
        destinations = next;
        repaint();
    }

    private void clearSelection() {
        selected = null;
        destinations = Set.of();
    }

    public ChessPosition positionForScreenSquare(int screenRow, int screenColumn) {
        if (screenRow < 0 || screenRow > 7 || screenColumn < 0 || screenColumn > 7) {
            return null;
        }
        if (orientation == ChessGame.TeamColor.WHITE) {
            return new ChessPosition(8 - screenRow, screenColumn + 1);
        }
        return new ChessPosition(screenRow + 1, 8 - screenColumn);
    }

    private ChessPosition positionAt(Point point) {
        BoardGeometry geometry = geometry();
        if (point.x < geometry.x || point.y < geometry.y
                || point.x >= geometry.x + geometry.size || point.y >= geometry.y + geometry.size) {
            return null;
        }
        return positionForScreenSquare((point.y - geometry.y) / geometry.square,
                (point.x - geometry.x) / geometry.square);
    }

    private BoardGeometry geometry() {
        int padding = 34;
        int available = Math.min(getWidth() - padding * 2, getHeight() - padding * 2);
        int square = Math.max(0, available / 8);
        int size = square * 8;
        return new BoardGeometry((getWidth() - size) / 2, (getHeight() - size) / 2 + 5, square, size);
    }

    private static String pretty(ChessPiece piece) {
        String name = piece.getPieceType().name().toLowerCase();
        return piece.getTeamColor().name().toLowerCase() + " " + name;
    }

    private static String symbol(ChessPiece piece) {
        boolean white = piece.getTeamColor() == ChessGame.TeamColor.WHITE;
        return switch (piece.getPieceType()) {
            case KING -> white ? "♔" : "♚";
            case QUEEN -> white ? "♕" : "♛";
            case ROOK -> white ? "♖" : "♜";
            case BISHOP -> white ? "♗" : "♝";
            case KNIGHT -> white ? "♘" : "♞";
            case PAWN -> white ? "♙" : "♟";
        };
    }

    private record BoardGeometry(int x, int y, int square, int size) { }
}
