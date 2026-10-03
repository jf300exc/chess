package ui.gui;

import chess.ChessGame;
import chess.ChessPosition;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

class ChessBoardPanelTests {
    @Test
    void whitePerspectiveCoordinatesAreCorrect() {
        ChessBoardPanel board = new ChessBoardPanel();
        board.setOrientation(ChessGame.TeamColor.WHITE);
        Assertions.assertEquals(new ChessPosition(8, 1), board.positionForScreenSquare(0, 0));
        Assertions.assertEquals(new ChessPosition(1, 8), board.positionForScreenSquare(7, 7));
    }

    @Test
    void blackPerspectiveCoordinatesAreCorrect() {
        ChessBoardPanel board = new ChessBoardPanel();
        board.setOrientation(ChessGame.TeamColor.BLACK);
        Assertions.assertEquals(new ChessPosition(1, 8), board.positionForScreenSquare(0, 0));
        Assertions.assertEquals(new ChessPosition(8, 1), board.positionForScreenSquare(7, 7));
    }

    @Test
    void initialPositionRendersHeadlessly() {
        ChessBoardPanel board = new ChessBoardPanel();
        board.setSize(720, 720);
        board.setPosition(new ChessGame(), ChessGame.TeamColor.WHITE, true);
        BufferedImage image = new BufferedImage(720, 720, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        Assertions.assertDoesNotThrow(() -> board.paint(graphics));
        graphics.dispose();
        Assertions.assertNotEquals(0, image.getRGB(360, 360));
    }
}
