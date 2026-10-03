package ui;

import chess.*;
import org.junit.jupiter.api.Test;
import org.jline.utils.AttributedString;

import java.util.List;

import static chess.ChessGame.TeamColor.*;
import static org.junit.jupiter.api.Assertions.*;

class TerminalControlsTests {
    @Test
    void hitTestingMatchesEverySquareAndCellInteriorInBothOrientations() {
        for (BoardDraw.Layout layout : List.of(new BoardDraw.Layout(4, 1), new BoardDraw.Layout(5, 2), new BoardDraw.Layout(6, 3))) {
            for (ChessGame.TeamColor perspective : ChessGame.TeamColor.values()) {
                for (int row = 0; row < 8; row++) {
                    for (int column = 0; column < 8; column++) {
                        for (int dy = 0; dy < layout.cellHeight(); dy++) {
                            for (int dx = 0; dx < layout.cellWidth(); dx++) {
                                ChessPosition expected = perspective == WHITE
                                        ? new ChessPosition(8 - row, column + 1) : new ChessPosition(row + 1, 8 - column);
                                assertEquals(expected, layout.positionAt(3 + column * layout.cellWidth() + dx,
                                        1 + row * layout.cellHeight() + dy, perspective));
                            }
                        }
                    }
                }
                assertNull(layout.positionAt(2, 1, perspective));
                assertNull(layout.positionAt(3, 0, perspective));
                assertNull(layout.positionAt(3 + 8 * layout.cellWidth(), 1, perspective));
                assertNull(layout.positionAt(3, 1 + 8 * layout.cellHeight(), perspective));
            }
        }
    }

    @Test
    void renderingFitsItsHitMapAndHandlesEmptyHighlightsWithoutMutatingGame() {
        ChessGame game = new ChessGame();
        ChessGame before = game.copy();
        for (BoardDraw.Layout layout : List.of(new BoardDraw.Layout(4, 1), new BoardDraw.Layout(5, 2), new BoardDraw.Layout(6, 3))) {
            String board = AttributedString.stripAnsi(BoardDraw.draw(game, WHITE, new ChessPosition(3, 3), layout, true));
            String[] lines = board.split("\n");
            assertEquals(layout.height(), lines.length);
            for (String line : lines) {
                assertTrue(line.length() <= layout.width());
            }
            assertTrue(board.contains("wK"));
            assertTrue(board.contains("bK"));
            assertEquals(before, game);
        }
        String plain = BoardDraw.draw(game, BLACK, new ChessPosition(2, 5), new BoardDraw.Layout(4, 1), false);
        assertFalse(plain.contains("\u001b"));
        assertTrue(plain.contains("+"));
        assertEquals(before, game);
    }

    @Test
    void decoderHandlesSgrLegacyAndFragmentedMouseReports() {
        TerminalEventDecoder decoder = new TerminalEventDecoder();
        String sgr = "\u001b[<0;24;18M";
        for (int i = 0; i < sgr.length() - 1; i++) {
            assertNull(decoder.feed(sgr.charAt(i)));
        }
        var click = decoder.feed('M');
        assertEquals(TerminalEventDecoder.Key.MOUSE, click.key());
        assertEquals(23, click.x());
        assertEquals(17, click.y());
        assertFalse(click.rightButton());
        assertTrue(decode("\u001b[<2;24;18M").rightButton());
        var legacy = decode("\u001b[M" + (char) 32 + (char) (24 + 32) + (char) (18 + 32));
        assertEquals(click, legacy);
        for (String ignored : List.of("\u001b[<0;24;18m", "\u001b[<64;24;18M", "\u001b[<32;24;18M",
                "\u001b[<brokenM", "\u001b[<0;0;2M", "\u001b[A")) {
            assertNull(decode(ignored));
        }
        assertEquals(TerminalEventDecoder.Key.TEXT, decoder.feed('e').key());
    }

    @Test
    void playerClickSelectionAllowsOnlyLegalDestinationsAndWaitsForServer() {
        BoardInteraction interaction = new BoardInteraction();
        ChessGame game = new ChessGame();
        assertNull(interaction.click(game, new ChessPosition(7, 1), WHITE).selected());
        assertEquals(new ChessPosition(2, 5), interaction.click(game, new ChessPosition(2, 5), WHITE).selected());
        assertNull(interaction.click(game, new ChessPosition(5, 5), WHITE).move());
        ChessMove move = interaction.click(game, new ChessPosition(4, 5), WHITE).move();
        assertEquals(CliInputParser.parseMove("e2e4"), move);
        assertNull(interaction.click(game, new ChessPosition(2, 4), WHITE).selected());
        interaction.clear();
        assertNotNull(interaction.click(game, new ChessPosition(2, 4), WHITE).selected());
    }

    @Test
    void observersCanInspectButCannotMakeMovesAndWrongTurnCannotSelect() {
        BoardInteraction observer = new BoardInteraction();
        ChessGame game = new ChessGame();
        assertEquals(new ChessPosition(2, 5), observer.click(game, new ChessPosition(2, 5), null).selected());
        assertNull(observer.click(game, new ChessPosition(4, 5), null).move());
        assertNull(observer.click(game, new ChessPosition(7, 5), BLACK).selected());
        game.setGameOver(true);
        assertNull(observer.click(game, new ChessPosition(2, 5), WHITE).selected());
    }

    private static TerminalEventDecoder.Event decode(String sequence) {
        TerminalEventDecoder decoder = new TerminalEventDecoder();
        TerminalEventDecoder.Event result = null;
        for (char character : sequence.toCharArray()) {
            result = decoder.feed(character);
        }
        return result;
    }
}
