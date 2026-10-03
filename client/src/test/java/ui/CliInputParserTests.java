package ui;

import chess.ChessMove;
import chess.ChessPiece;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class CliInputParserTests {
    @Test
    void commandsNormalizeCaseAndWhitespace() {
        assertEquals("make move e2e4", CliInputParser.normalizeCommand("  Make   Move   E2E4 "));
    }

    @Test
    void movesAcceptCommonCoordinateFormats() {
        ChessMove compact = CliInputParser.parseMove("E2E4");
        ChessMove spaced = CliInputParser.parseMove("e2 e4");

        assertEquals(compact, spaced);
        assertEquals("e2e4", CliInputParser.formatMove(compact));
    }

    @Test
    void movesSupportExplicitPromotion() {
        ChessMove move = CliInputParser.parseMove("e7-e8=N");

        assertEquals(ChessPiece.PieceType.KNIGHT, move.getPromotionPiece());
        assertEquals("e7e8=N", CliInputParser.formatMove(move));
    }

    @Test
    void malformedCoordinatesAreRejected() {
        assertNull(CliInputParser.parsePosition("i4"));
        assertNull(CliInputParser.parseMove("e2-e9"));
        assertNull(CliInputParser.parseMove("e2e4=K"));
    }
}
