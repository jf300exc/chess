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
        for (BoardDraw.Layout layout : layouts()) {
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
        for (BoardDraw.Layout layout : layouts()) {
            String board = AttributedString.stripAnsi(BoardDraw.draw(game, WHITE, new ChessPosition(3, 3), layout, true));
            String[] lines = board.split("\n");
            assertEquals(layout.height(), lines.length);
            for (String line : lines) {
                assertTrue(new AttributedString(line).columnLength() <= layout.width());
            }
            assertTrue(board.contains("♔"));
            assertTrue(board.contains("♚"));
            assertEquals(before, game);
        }
        String plain = BoardDraw.draw(game, BLACK, new ChessPosition(2, 5), new BoardDraw.Layout(3, 1), false);
        assertFalse(plain.contains("\u001b"));
        assertTrue(plain.contains("+"));
        assertEquals(before, game);
    }

    @Test
    void symbolsEmptySquaresMarkersAndCoordinatesShareTheExactCellCenter() {
        ChessGame game = new ChessGame();
        for (BoardDraw.Layout layout : layouts()) {
            for (ChessGame.TeamColor perspective : ChessGame.TeamColor.values()) {
                for (PieceSymbols style : PieceSymbols.values()) {
                    for (boolean color : List.of(true, false)) {
                        String[] lines = BoardDraw.draw(game, perspective, new ChessPosition(2, 5), layout, color, style)
                                .split("\n");
                        for (int row = 0; row < 8; row++) {
                            AttributedString line = AttributedString.fromAnsi(lines[1 + row * layout.cellHeight()
                                    + (layout.cellHeight() - 1) / 2]);
                            assertEquals(3 + 8 * layout.cellWidth() + 2, line.columnLength());
                            for (int column = 0; column < 8; column++) {
                                int left = 3 + column * layout.cellWidth();
                                int center = left + layout.cellWidth() / 2;
                                String label = line.columnSubSequence(center, center + 1).toString();
                                ChessPosition square = BoardDraw.positionForCell(row, column, perspective);
                                ChessPiece piece = game.getBoard().getPiece(square);
                                String expected = piece != null
                                        ? style.forRendering(color).glyph(piece.getPieceType(), piece.getTeamColor())
                                        : square.equals(new ChessPosition(3, 5)) || square.equals(new ChessPosition(4, 5))
                                        ? "+" : color ? " " : ".";
                                assertEquals(expected, label, style + " " + perspective + " " + square);
                                assertEquals(" ".repeat(layout.cellWidth() / 2),
                                        line.columnSubSequence(left, center).toString());
                                assertEquals(" ".repeat(layout.cellWidth() / 2),
                                        line.columnSubSequence(center + 1, left + layout.cellWidth()).toString());
                                assertEquals(square, layout.positionAt(center,
                                        1 + row * layout.cellHeight() + (layout.cellHeight() - 1) / 2, perspective));
                                char file = (char) ('a' + square.getColumn() - 1);
                                assertEquals(Character.toString(file), new AttributedString(lines[0])
                                        .columnSubSequence(center, center + 1).toString());
                            }
                        }
                    }
                }
            }
        }
    }

    @Test
    void allPieceTypesUseSingleColumnGlyphsIncludingSupplementaryNerdIcons() {
        for (ChessPiece.PieceType type : ChessPiece.PieceType.values()) {
            for (ChessGame.TeamColor team : ChessGame.TeamColor.values()) {
                for (PieceSymbols style : PieceSymbols.values()) {
                    String symbol = style.glyph(type, team);
                    assertEquals(1, symbol.codePointCount(0, symbol.length()));
                    assertEquals(1, new AttributedString(symbol).columnLength());
                }
            }
            assertNotEquals(PieceSymbols.UNICODE.glyph(type, WHITE), PieceSymbols.UNICODE.glyph(type, BLACK));
        }
        assertEquals(2, PieceSymbols.NERD.glyph(ChessPiece.PieceType.KING, WHITE).length());
        assertEquals(0xF0857, PieceSymbols.NERD.glyph(ChessPiece.PieceType.KING, WHITE).codePointAt(0));
        assertEquals(PieceSymbols.UNICODE, PieceSymbols.NERD.forRendering(false));
    }

    @Test
    void adaptiveLayoutsAndExplicitFontOptionsAreValidated() {
        assertEquals(new BoardDraw.Layout(3, 1), BoardDraw.Layout.forSize(44, 18));
        assertEquals(new BoardDraw.Layout(5, 2), BoardDraw.Layout.forSize(80, 24));
        assertEquals(new BoardDraw.Layout(7, 3), BoardDraw.Layout.forSize(90, 36));
        assertEquals(new BoardDraw.Layout(5, 2), BoardDraw.Layout.forSize(61, 36));
        assertEquals(PieceSymbols.UNICODE, PieceSymbols.fromArgs(new String[]{}));
        assertEquals(PieceSymbols.NERD, PieceSymbols.fromArgs(new String[]{"--pieces", "nerd"}));
        assertEquals(PieceSymbols.ASCII, PieceSymbols.fromArgs(new String[]{"--pieces=ASCII", "--text"}));
        assertThrows(IllegalArgumentException.class, () -> PieceSymbols.fromArgs(new String[]{"--pieces"}));
        assertThrows(IllegalArgumentException.class, () -> PieceSymbols.fromArgs(new String[]{"--pieces", "--text"}));
        assertThrows(IllegalArgumentException.class, () -> PieceSymbols.fromArgs(new String[]{"--pieces=unknown"}));
    }

    private static List<BoardDraw.Layout> layouts() {
        return List.of(new BoardDraw.Layout(3, 1), new BoardDraw.Layout(5, 2), new BoardDraw.Layout(7, 3));
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
