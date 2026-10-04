package ui;

import chess.*;
import org.jline.terminal.Size;
import org.jline.terminal.Attributes;
import org.jline.terminal.TerminalBuilder;
import org.junit.jupiter.api.Test;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.nio.charset.StandardCharsets;

import static chess.ChessGame.TeamColor.*;
import static org.junit.jupiter.api.Assertions.*;

class BoardGraphicsTests {
    @Test
    void detectionRequiresAdvertisedGraphicsAndValidGeometry() {
        assertEquals(new TerminalGraphics.CellPixels(9, 18),
                TerminalGraphics.parse("\u001b[?64;1;4;6c\u001b[6;18;9t", 80, 24));
        assertEquals(new TerminalGraphics.CellPixels(9, 18),
                TerminalGraphics.parse("\u001b[4;432;720t\u001b[?62;4c", 80, 24));
        for (String unsupported : new String[]{"", "\u001b[?64;1;6c\u001b[6;18;9t", "\u001b[?4c\u001b[6;18;9t",
                "\u001b[?64;4c", "\u001b[6;18;9t", "\u001b[?64;4c\u001b[6;0;0t",
                "\u001b[?64;4c\u001b[6;9000;9000t", "\u001b[?64;4c\u001b[4;433;721t"}) {
            assertNull(TerminalGraphics.parse(unsupported, 80, 24), unsupported);
        }
    }

    @Test
    void probingPreservesOrdinaryInputAndMouseReports() throws Exception {
        String mouse = "\u001b[<0;26;16M";
        String input = "sta" + mouse + "\u001b[?80;1$y\u001b[?64;4c\u001b[6;18;9t";
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try (var source = new PipedInputStream(); var sender = new PipedOutputStream(source);
             var terminal = TerminalBuilder.builder().system(false).type("xterm").dumb(true)
                .attributes(new Attributes()).streams(source, output)
                .size(new Size(80, 24)).build()) {
            sender.write(input.getBytes(StandardCharsets.UTF_8));
            sender.flush();
            var probe = TerminalGraphics.probe(terminal);
            assertEquals(new TerminalGraphics.CellPixels(9, 18), probe.pixels());
            assertTrue(probe.restoreDisplayMode());
            StringBuilder preserved = new StringBuilder();
            probe.pendingInput().forEach(ch -> preserved.append((char) ch.intValue()));
            assertEquals("sta" + mouse, preserved.toString());
            assertTrue(output.toString(StandardCharsets.UTF_8).contains("\u001b[16t"));
        }
    }

    @Test
    void pieceOutlinesAreLargeAndCenteredWithoutRectangularBadges() {
        for (var type : ChessPiece.PieceType.values()) {
            for (var team : ChessGame.TeamColor.values()) {
                for (var symbols : new PieceSymbols[]{PieceSymbols.UNICODE, PieceSymbols.NERD}) {
                    ChessGame game = new ChessGame();
                    ChessBoard board = new ChessBoard();
                    board.addPiece(new ChessPosition(4, 5), new ChessPiece(team, type));
                    game.setBoard(board);
                    BufferedImage image = BoardImage.render(game, WHITE, null, new BoardDraw.Layout(5, 2),
                            new TerminalGraphics.CellPixels(10, 20), symbols);
                    assertEquals(400, image.getWidth());
                    assertEquals(320, image.getHeight());
                    int x = 4 * 50;
                    int y = 4 * 40;
                    int background = image.getRGB(x, y);
                    int minX = 50, minY = 40, maxX = -1, maxY = -1;
                    for (int dy = 0; dy < 40; dy++) {
                        for (int dx = 0; dx < 50; dx++) {
                            if (image.getRGB(x + dx, y + dy) != background) {
                                minX = Math.min(minX, dx);
                                maxX = Math.max(maxX, dx);
                                minY = Math.min(minY, dy);
                                maxY = Math.max(maxY, dy);
                            }
                        }
                    }
                    assertTrue(maxY - minY >= 27, type + " was not enlarged");
                    assertEquals(24.5, (minX + maxX) / 2.0, 1.0, type + " horizontal center");
                    assertEquals(19.5, (minY + maxY) / 2.0, 1.0, type + " vertical center");
                    assertTrue(minX >= 6 && maxX <= 43 && minY >= 2 && maxY <= 37);
                    assertEquals(background, image.getRGB(x + 2, y + 20), "No backdrop rectangle at piece edge");
                }
            }
        }
    }

    @Test
    void boardImagesFlipHighlightsAndKeepTheGameSnapshotUnchanged() {
        ChessGame game = new ChessGame();
        ChessGame before = game.copy();
        var layout = new BoardDraw.Layout(5, 2);
        var pixels = new TerminalGraphics.CellPixels(10, 20);
        BufferedImage white = BoardImage.render(game, WHITE, new ChessPosition(2, 5), layout, pixels, PieceSymbols.UNICODE);
        BufferedImage black = BoardImage.render(game, BLACK, new ChessPosition(2, 5), layout, pixels, PieceSymbols.UNICODE);
        assertEquals(white.getRGB(4 * 50, 6 * 40), black.getRGB(3 * 50, 1 * 40));
        assertEquals(white.getRGB(4 * 50, 4 * 40), black.getRGB(3 * 50, 3 * 40));
        assertNotEquals(white.getRGB(4 * 50, 4 * 40), white.getRGB(3 * 50, 4 * 40));
        assertEquals(before, game);
    }

    @Test
    void sixelEncodingRoundTripsPixelsIncludingPartialBandsAndRuns() {
        BufferedImage source = new BufferedImage(19, 13, BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < source.getHeight(); y++) {
            for (int x = 0; x < source.getWidth(); x++) {
                source.setRGB(x, y, x < 8 ? 0xffffff : (x * 13 << 16) | (y * 17 << 8) | 89);
            }
        }
        String encoded = SixelEncoder.encode(source);
        assertTrue(encoded.startsWith("\u001bP0;1;0q\"1;1;19;13"));
        assertTrue(encoded.endsWith("\u001b\\"));
        assertTrue(encoded.contains("!"));
        BufferedImage decoded = decode(encoded, 19, 13);
        for (int y = 0; y < 13; y++) {
            for (int x = 0; x < 19; x++) {
                int rgb = source.getRGB(x, y);
                int quantized = 0xff000000 | (((rgb >> 16 & 255) + 25) / 51 * 51 << 16)
                        | (((rgb >> 8 & 255) + 25) / 51 * 51 << 8) | ((rgb & 255) + 25) / 51 * 51;
                assertEquals(quantized, decoded.getRGB(x, y));
            }
        }
    }

    @Test
    void oversizedImagesAndPixelSizesAreRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> SixelEncoder.encode(new BufferedImage(1025, 1, BufferedImage.TYPE_INT_RGB)));
        assertThrows(IllegalArgumentException.class, () -> new TerminalGraphics.CellPixels(0, 20));
        assertThrows(IllegalArgumentException.class, () -> BoardImage.render(new ChessGame(), WHITE, null,
                new BoardDraw.Layout(7, 3), new TerminalGraphics.CellPixels(64, 128), PieceSymbols.UNICODE));
    }

    @Test
    void textRendererDoesNotEmitPerPieceBackgroundBadges() {
        String board = BoardDraw.draw(new ChessGame(), WHITE, null, new BoardDraw.Layout(3, 1), true, PieceSymbols.NERD);
        assertFalse(board.contains("48;5;238"));
        assertFalse(board.contains("48;5;252"));
        assertFalse(board.contains("."), "Empty colored squares should not add dot clutter");
    }

    /** Independent decoder for the commands produced by the encoder, not a screenshot reconstruction. */
    private static BufferedImage decode(String encoded, int width, int height) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        String data = encoded.substring(encoded.indexOf('q') + 1, encoded.length() - 2);
        int x = 0, y = 0, color = 0;
        for (int i = 0; i < data.length();) {
            char token = data.charAt(i++);
            if (token == '"' || token == '#') {
                int start = i;
                while (i < data.length() && (Character.isDigit(data.charAt(i)) || data.charAt(i) == ';')) {
                    i++;
                }
                if (token == '#') {
                    color = Integer.parseInt(data.substring(start, i).split(";")[0]);
                }
            } else if (token == '$') {
                x = 0;
            } else if (token == '-') {
                x = 0;
                y += 6;
            } else {
                int repeat = 1;
                if (token == '!') {
                    int start = i;
                    while (Character.isDigit(data.charAt(i))) {
                        i++;
                    }
                    repeat = Integer.parseInt(data.substring(start, i));
                    token = data.charAt(i++);
                }
                int bits = token - 63;
                int rgb = color / 36 * 51 << 16 | color / 6 % 6 * 51 << 8 | color % 6 * 51;
                for (int count = 0; count < repeat; count++, x++) {
                    for (int bit = 0; bit < 6 && y + bit < height; bit++) {
                        if ((bits & 1 << bit) != 0) {
                            image.setRGB(x, y + bit, rgb);
                        }
                    }
                }
            }
        }
        return image;
    }
}
