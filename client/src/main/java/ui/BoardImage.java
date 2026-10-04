package ui;

import chess.*;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

/** Scale font outlines inside board squares, independently of the terminal's text font. */
final class BoardImage {
    private static final Color LIGHT = new Color(204, 204, 153);
    private static final Color DARK = new Color(102, 153, 102);
    private static final Color SELECTED = new Color(204, 153, 51);
    private static final Color LEGAL = new Color(102, 204, 204);
    private static final Color INK = new Color(32, 40, 44);
    private static final Font CHESS_FONT = new Font("DejaVu Sans", Font.PLAIN, 100);

    static BufferedImage render(ChessGame game, ChessGame.TeamColor perspective, ChessPosition selected,
                                BoardDraw.Layout layout, TerminalGraphics.CellPixels pixels, PieceSymbols symbols) {
        int cellWidth = layout.cellWidth() * pixels.width();
        int cellHeight = layout.cellHeight() * pixels.height();
        int width = 8 * cellWidth;
        int height = 8 * cellHeight;
        if (width > 1024 || height > 1024) {
            throw new IllegalArgumentException("Terminal board image is too large");
        }
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        try {
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            Collection<ChessMove> moves = selected == null ? null : game.validMoves(selected);
            Set<ChessPosition> legal = moves == null ? Set.of()
                    : moves.stream().map(ChessMove::getEndPosition).collect(Collectors.toSet());
            for (int row = 0; row < 8; row++) {
                for (int column = 0; column < 8; column++) {
                    ChessPosition position = BoardDraw.positionForCell(row, column, perspective);
                    Color background = position.equals(selected) ? SELECTED : legal.contains(position) ? LEGAL
                            : (position.getRow() + position.getColumn()) % 2 == 1 ? LIGHT : DARK;
                    int x = column * cellWidth;
                    int y = row * cellHeight;
                    graphics.setColor(background);
                    graphics.fillRect(x, y, cellWidth, cellHeight);
                    ChessPiece piece = game.getBoard().getPiece(position);
                    if (piece != null) {
                        drawPiece(graphics, piece, x, y, cellWidth, cellHeight, symbols);
                    } else if (legal.contains(position)) {
                        int diameter = Math.max(4, Math.min(cellWidth, cellHeight) / 6);
                        graphics.setColor(INK);
                        graphics.fillOval(x + (cellWidth - diameter) / 2, y + (cellHeight - diameter) / 2,
                                diameter, diameter);
                    }
                }
            }
        } finally {
            graphics.dispose();
        }
        return image;
    }

    private static void drawPiece(Graphics2D graphics, ChessPiece piece, int x, int y, int width, int height,
                                  PieceSymbols symbols) {
        // Filled silhouettes for both teams; white gets an outline, not a rectangular badge.
        String glyph = (symbols == PieceSymbols.ASCII ? PieceSymbols.UNICODE : symbols)
                .glyph(piece.getPieceType(), ChessGame.TeamColor.BLACK);
        Font font = CHESS_FONT;
        if (symbols == PieceSymbols.NERD) {
            font = new Font("JetBrainsMono Nerd Font", Font.PLAIN, 100);
        }
        if (!font.canDisplay(glyph.codePointAt(0))) {
            glyph = PieceSymbols.UNICODE.glyph(piece.getPieceType(), ChessGame.TeamColor.BLACK);
            font = new Font(Font.DIALOG, Font.PLAIN, 100);
        }
        Shape shape = font.createGlyphVector(graphics.getFontRenderContext(), glyph).getOutline();
        Rectangle2D bounds = shape.getBounds2D();
        double scale = Math.min(width * .70 / bounds.getWidth(), height * .78 / bounds.getHeight());
        AffineTransform transform = new AffineTransform();
        transform.translate(x + width / 2.0, y + height / 2.0);
        transform.scale(scale, scale);
        transform.translate(-bounds.getCenterX(), -bounds.getCenterY());
        Shape centered = transform.createTransformedShape(shape);
        graphics.setColor(piece.getTeamColor() == ChessGame.TeamColor.WHITE ? Color.WHITE : INK);
        graphics.fill(centered);
        if (piece.getTeamColor() == ChessGame.TeamColor.WHITE) {
            graphics.setStroke(new BasicStroke(Math.max(1f, Math.min(width, height) / 32f),
                    BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            graphics.setColor(INK);
            graphics.draw(centered);
        }
    }
}
