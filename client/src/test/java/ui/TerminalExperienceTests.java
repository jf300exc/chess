package ui;

import chess.*;
import org.jline.terminal.Attributes;
import org.jline.terminal.Size;
import org.jline.terminal.TerminalBuilder;
import org.jline.utils.AttributedString;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static chess.ChessGame.TeamColor.*;
import static org.junit.jupiter.api.Assertions.*;

class TerminalExperienceTests {
    @Test
    void unchangedFramesEmitNoTextAndTypingOnlyDamagesThePrompt() {
        TerminalScreen screen = new TerminalScreen();
        var before = List.of(new AttributedString("CHESS"), new AttributedString("board"), new AttributedString("> "));
        assertEquals(3, screen.changes(before, 79).size());
        assertTrue(screen.changes(before, 79).isEmpty());
        var after = List.of(before.get(0), before.get(1), new AttributedString("> status"));
        var damage = screen.changes(after, 79);
        assertEquals(1, damage.size());
        assertEquals(2, damage.getFirst().row());
        assertEquals(" status", damage.getFirst().text().toString());
        assertTrue(screen.changes(after, 79).isEmpty());
    }

    @Test
    void colorsErasureWideGlyphsAndSurrogatePairsStayAligned() {
        TerminalScreen screen = new TerminalScreen();
        screen.changes(List.of(new AttributedString("♔\uD83D\uDE00 old message")), 40);
        var change = screen.changes(List.of(new AttributedString("♔\uD83D\uDE01")), 40).getFirst();
        assertEquals(1, change.column());
        assertTrue(change.text().toString().startsWith("\uD83D\uDE01"));
        assertFalse(Character.isLowSurrogate(change.text().charAt(0)));
        assertTrue(change.text().toString().endsWith(" "), "Old message must be overwritten, not left on screen");
        var colored = AttributedString.fromAnsi("\u001b[31m♔\uD83D\uDE01\u001b[0m");
        assertFalse(screen.changes(List.of(colored), 40).isEmpty(), "Style-only updates are damage too");
        assertTrue(screen.changes(List.of(colored), 40).isEmpty());
    }

    @Test
    void textUpdatesNeverOverwriteTheImageRectangleOrScroll() throws Exception {
        try (var input = new PipedInputStream(); var sender = new PipedOutputStream(input);
             var output = new ByteArrayOutputStream();
             var terminal = TerminalBuilder.builder().system(false).type("xterm").dumb(true)
                     .streams(input, output).attributes(new Attributes()).size(new Size(80, 24)).build()) {
            TerminalScreen screen = new TerminalScreen();
            var image = new TerminalScreen.ImageArea(0, 3, 1, 40);
            screen.update(terminal, List.of(new AttributedString(" 8 " + " ".repeat(40) + " 8")), 80, 23, 0, image);
            terminal.flush();
            output.reset();
            screen.update(terminal, List.of(new AttributedString(" 1 " + " ".repeat(40) + " 1")), 80, 23, 0, image);
            terminal.flush();
            String delta = output.toString(StandardCharsets.UTF_8);
            assertFalse(delta.contains(" ".repeat(40)), "Rank-label changes must not erase unchanged squares");
            assertFalse(delta.contains("\u001b[2J"));
            assertFalse(delta.contains("\n"));
            assertTrue(delta.contains("\u001b[1;44H"), "Right gutter needs a separate cursor position");
        }
    }

    @Test
    void boardDamageOnlyContainsChangedSquares() throws Exception {
        ChessGame game = new ChessGame();
        var layout = new BoardDraw.Layout(5, 2);
        var pixels = new TerminalGraphics.CellPixels(9, 18);
        var before = BoardImage.render(game, WHITE, null, layout, pixels, PieceSymbols.UNICODE);
        assertEquals(1, BoardDamage.between(null, before).size());
        assertTrue(BoardDamage.between(before,
                BoardImage.render(game.copy(), WHITE, null, layout, pixels, PieceSymbols.UNICODE)).isEmpty());
        var selected = BoardImage.render(game, WHITE, new ChessPosition(2, 5), layout, pixels, PieceSymbols.UNICODE);
        assertEquals(Set.of("6:4", "5:4", "4:4"), BoardDamage.between(before, selected).stream()
                .map(p -> p.row() + ":" + p.column()).collect(Collectors.toSet()));
        game.makeMove(new ChessMove(new ChessPosition(2, 5), new ChessPosition(4, 5), null));
        var moved = BoardImage.render(game, WHITE, null, layout, pixels, PieceSymbols.UNICODE);
        var patches = BoardDamage.between(before, moved);
        assertEquals(Set.of("6:4", "4:4"), patches.stream()
                .map(p -> p.row() + ":" + p.column()).collect(Collectors.toSet()));
        assertTrue(patches.stream().allMatch(p -> p.image().getWidth() == 45 && p.image().getHeight() == 36));
    }

    @Test
    void turnCuesFollowPlayerRoleNotPerspectiveAndStopAtGameOver() {
        ChessGame game = new ChessGame();
        assertTrue(GameStatus.describe(game, WHITE, null).text().contains("YOUR TURN"));
        assertFalse(GameStatus.describe(game, WHITE, null).waiting());
        assertTrue(GameStatus.describe(game, BLACK, null).waiting());
        assertFalse(GameStatus.describe(game, null, null).waiting(), "Observers must not get personal turn cues");
        game.setGameOver(true);
        var ended = GameStatus.describe(game, BLACK, "Alice has resigned");
        assertEquals("GAME OVER  /  Alice has resigned", ended.text());
        assertFalse(ended.waiting());
        assertEquals("GAME OVER", GameStatus.describe(game, BLACK, null).text(), "Do not guess a resignation winner");
    }

    @Test
    void checkmateNamesTheWinnerAndStalemateNamesTheDraw() throws Exception {
        ChessGame game = new ChessGame();
        for (String move : List.of("f2f3", "e7e5", "g2g4", "d8h4")) {
            game.makeMove(CliInputParser.parseMove(move));
        }
        assertEquals("GAME OVER  /  BLACK wins by checkmate", GameStatus.describe(game, WHITE, null).text());
        ChessBoard board = new ChessBoard();
        board.addPiece(new ChessPosition(1, 8), new ChessPiece(WHITE, ChessPiece.PieceType.KING));
        board.addPiece(new ChessPosition(2, 6), new ChessPiece(BLACK, ChessPiece.PieceType.KING));
        board.addPiece(new ChessPosition(3, 7), new ChessPiece(BLACK, ChessPiece.PieceType.QUEEN));
        game.setBoard(board);
        game.setGameOver(false);
        assertEquals("GAME OVER  /  Draw by stalemate", GameStatus.describe(game, WHITE, null).text());
    }

    @Test
    void noColorOutputRetainsTheStatusWithoutEscapeSequences() {
        assertEquals("YOUR TURN", GameStatus.paint("YOUR TURN", "1;32", false));
        assertTrue(GameStatus.paint("CHECK!", "1;31", true).startsWith("\u001b[1;31m"));
        String frame = BoardDraw.imageFrame(new BoardDraw.Layout(5, 2), WHITE);
        assertFalse(frame.contains("♔"));
        assertEquals(18, frame.split("\n").length);
    }
}
