package engine;

import chess.*;
import model.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.*;
import static org.junit.jupiter.api.Assertions.*;
import static chess.ChessGame.TeamColor.*;

class StockfishEngineTests {
    @TempDir Path directory;

    @Test void eloUsesStrengthLimitingAndClampsToAdvertisedRange() throws Exception {
        Path executable = TestStockfish.executable(directory, "e2e4");
        try (var engine = new StockfishEngine(executable.toString())) {
            var player = engine.player(new StockfishOptions(WHITE, StockfishOptions.Mode.ELO, null, null), 1200);
            assertEquals(1320, player.elo());
            assertEquals(ChessUci.move("e2e4"), engine.bestMove(new ChessGame(), player));
        }
        String log = Files.readString(directory.resolve("fake-stockfish.log"));
        assertTrue(log.contains("setoption name UCI_LimitStrength value true\n"));
        assertTrue(log.contains("setoption name UCI_Elo value 1320\n"));
        assertTrue(log.contains("position fen rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1\n"));
    }

    @Test void skillEndpointsDisableEloLimiting() throws Exception {
        Path executable = TestStockfish.executable(directory, "e2e4");
        for (int skill : new int[]{0, 20}) {
            try (var engine = new StockfishEngine(executable.toString())) {
                engine.bestMove(new ChessGame(), engine.player(new StockfishOptions(WHITE, StockfishOptions.Mode.SKILL, null, skill), 1500));
            }
        }
        String log = Files.readString(directory.resolve("fake-stockfish.log"));
        assertTrue(log.contains("UCI_LimitStrength value false\n"));
        assertTrue(log.contains("Skill Level value 0\n"));
        assertTrue(log.contains("Skill Level value 20\n"));
        assertFalse(log.contains("setoption name UCI_Elo"));
    }

    @Test void exitsAndInvalidMovesFailCleanly() throws Exception {
        Path script = directory.resolve("exit.py");
        Files.writeString(script, "#!/usr/bin/python3\nprint('not an engine', flush=True)\n");
        script.toFile().setExecutable(true);
        assertThrows(IOException.class, () -> new StockfishEngine(script.toString()));
        try (var engine = new StockfishEngine(TestStockfish.executable(directory, "(none)").toString())) {
            assertThrows(IOException.class, () -> engine.bestMove(new ChessGame(), engine.player(new StockfishOptions(WHITE, StockfishOptions.Mode.ELO, null, null), 1500)));
        }
    }

    @Test void unresponsiveEngineTimesOutAndIsKilled() throws Exception {
        Path script = directory.resolve("stall.py");
        Files.writeString(script, "#!/usr/bin/python3\nimport time\ntime.sleep(60)\n");
        script.toFile().setExecutable(true);
        assertTimeout(java.time.Duration.ofSeconds(8), () -> assertThrows(IOException.class, () -> new StockfishEngine(script.toString())));
    }

    @Test void fenIncludesEnPassantAndLostCastleRights() throws Exception {
        ChessGame game = new ChessGame();
        game.makeMove(ChessUci.move("e2e4"));
        assertEquals("rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR b KQkq e3 0 1", ChessUci.fen(game));
        assertEquals(ChessUci.fen(game), ChessUci.fen(game.copy()));
        game.getBoard().setCastleStatus(WHITE, ChessBoard.CastlePieceTypes.KING, ChessBoard.CastleType.KING_SIDE, false);
        assertTrue(ChessUci.fen(game).contains(" b Qkq e3 "));
        for (String promotion : new String[]{"e7e8q", "e7e8r", "e7e8b", "e7e8n"}) {
            assertNotNull(ChessUci.move(promotion).getPromotionPiece());
        }
        assertThrows(IllegalArgumentException.class, () -> ChessUci.move("e9e2"));
    }

    @Test void realStockfishReturnsLegalMovesInBothModes() throws Exception {
        Assumptions.assumeTrue(System.getenv("STOCKFISH_PATH") != null, "Set STOCKFISH_PATH for real-engine smoke test");
        for (var options : new StockfishOptions[]{new StockfishOptions(WHITE, StockfishOptions.Mode.ELO, null, null),
                new StockfishOptions(WHITE, StockfishOptions.Mode.SKILL, null, 0),
                new StockfishOptions(WHITE, StockfishOptions.Mode.SKILL, null, 20)}) {
            ChessGame game = new ChessGame();
            try (var engine = new StockfishEngine()) {
                var player = engine.player(options, 1500);
                for (int ply = 0; ply < 4; ply++) { game.makeMove(engine.bestMove(game, player)); }
            }
        }
    }
}
