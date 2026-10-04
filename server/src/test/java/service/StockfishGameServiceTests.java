package service;

import chess.ChessGame;
import dataaccess.*;
import engine.*;
import model.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import requests.*;

import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;
import static chess.ChessGame.TeamColor.*;

class StockfishGameServiceTests {
    @TempDir Path directory;
    MemoryGameDAO games;
    MemoryAuthDAO auth;
    MemoryUserDAO users;
    GameService service;

    @BeforeEach void setup() throws Exception {
        games = new MemoryGameDAO(); auth = new MemoryAuthDAO(); users = new MemoryUserDAO();
        auth.addAuth(new AuthData("token", "Ada")); auth.addAuth(new AuthData("other", "Ben"));
        users.addUser(new UserData("Ada", "password", "email"));
        var executable = TestStockfish.executable(directory, "e2e4");
        var ids = new AtomicInteger();
        service = new GameService(games, auth, users, ids::incrementAndGet, () -> new StockfishEngine(executable.toString()));
    }

    StockfishOptions adaptive(ChessGame.TeamColor color) { return new StockfishOptions(color, StockfishOptions.Mode.ELO, null, null); }

    @Test void newPlayersStartAt1500AndAdaptiveDifficultyUsesLatestRating() {
        assertEquals(1500, service.profile("token").elo());
        var first = service.createGame(new CreateGameRequest("token", "First", adaptive(BLACK)));
        var game = games.findGameDataByID(first.gameID());
        assertEquals("Ada", game.whiteUsername()); assertEquals("Stockfish", game.blackUsername());
        assertEquals(1500, game.stockfish().elo());
        users.addUser(new UserData("Ada", "password", "email", 1730));
        var next = service.createGame(new CreateGameRequest("token", "Next", adaptive(WHITE)));
        assertEquals(1730, games.findGameDataByID(next.gameID()).stockfish().elo());
        assertEquals("Ada", games.findGameDataByID(next.gameID()).blackUsername());
        assertEquals(1500, games.findGameDataByID(first.gameID()).stockfish().elo());
    }

    @Test void humanGamesAndSeatsRemainSupported() {
        var id = service.createGame(new CreateGameRequest("token", "Friends")).gameID();
        assertNull(games.findGameDataByID(id).stockfish());
        assertEquals("", service.joinGame(new JoinGameRequest("token", "WHITE", id)).message());
        assertEquals("", service.joinGame(new JoinGameRequest("other", "BLACK", id)).message());
        assertEquals("Ben", games.findGameDataByID(id).blackUsername());
    }

    @Test void addingEngineRequiresOppositeHumanSeatAndFreshGame() throws Exception {
        var id = service.createGame(new CreateGameRequest("token", "Add AI")).gameID();
        assertEquals("Error: unauthorized", service.addStockfish(new AddStockfishRequest("token", id, adaptive(BLACK))).message());
        service.joinGame(new JoinGameRequest("token", "WHITE", id));
        assertEquals("Error: unauthorized", service.addStockfish(new AddStockfishRequest("other", id, adaptive(BLACK))).message());
        assertEquals("", service.addStockfish(new AddStockfishRequest("token", id, adaptive(BLACK))).message());
        assertEquals("Error: already taken", service.joinGame(new JoinGameRequest("other", "BLACK", id)).message());
        var other = service.createGame(new CreateGameRequest("token", "Started")).gameID();
        service.joinGame(new JoinGameRequest("token", "WHITE", other));
        games.findGameDataByID(other).game().makeMove(ChessUci.move("e2e4"));
        assertEquals("Error: bad request", service.addStockfish(new AddStockfishRequest("token", other, adaptive(BLACK))).message());
    }

    @Test void invalidOrUnavailableEngineDoesNotCreateAGame() {
        for (var options : new StockfishOptions[]{new StockfishOptions(BLACK, StockfishOptions.Mode.SKILL, null, -1),
                new StockfishOptions(BLACK, StockfishOptions.Mode.SKILL, null, 21),
                new StockfishOptions(BLACK, StockfishOptions.Mode.ELO, 1500, 3),
                new StockfishOptions(null, StockfishOptions.Mode.ELO, null, null)}) {
            assertEquals("Error: bad request", service.createGame(new CreateGameRequest("token", "Invalid", options)).message());
        }
        var missing = new GameService(games, auth, users, () -> 1, () -> { throw new java.io.IOException("Missing"); });
        assertTrue(missing.createGame(new CreateGameRequest("token", "Unavailable", adaptive(BLACK))).message().contains("Stockfish unavailable"));
        assertTrue(games.findGameData().isEmpty());
        assertNull(service.profile("invalid"));
    }

    @Test void ratingWinsDrawsAndLossesAreSymmetric() {
        assertEquals(1516, EloRating.after(1500, 1500, 1));
        assertEquals(1484, EloRating.after(1500, 1500, 0));
        assertEquals(1500, EloRating.after(1500, 1500, .5));
        assertTrue(EloRating.after(1500, 1900, 1) > 1516);
    }
}
