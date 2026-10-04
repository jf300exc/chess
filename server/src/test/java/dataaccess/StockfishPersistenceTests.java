package dataaccess;

import chess.*;
import model.*;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import static chess.ChessGame.TeamColor.*;

class StockfishPersistenceTests {
    SQLGameDAO games = new SQLGameDAO();
    SQLUserDAO users = new SQLUserDAO();
    @BeforeEach void setup() {
        DatabaseManager.configureDatabase(); games.clear(); users.clear();
        users.addUser(new UserData("Ada", "password", "email")); users.addUser(new UserData("Ben", "password", "email"));
    }
    @AfterEach void cleanup() { games.clear(); users.clear(); }
    GameData game(StockfishOptions.Mode mode) {
        return new GameData(81, "Ada", mode == null ? "Ben" : "Stockfish", "Ratings", new ChessGame(),
                mode == null ? null : new StockfishPlayer(BLACK, mode, 1500, mode == StockfishOptions.Mode.SKILL ? 5 : null));
    }

    @Test void defaultRatingAndEngineSettingsSurviveReloadAndSeatUpdates() {
        assertEquals(1500, users.findUserDataByUsername("Ada").elo());
        assertEquals("email", users.getAllUserData().iterator().next().email());
        GameData game = game(StockfishOptions.Mode.SKILL); games.addGameData(game);
        assertEquals(game, games.findGameDataByID("81"));
        games.saveGame(GameData.updateGameDataUsers("WHITE", "Ada", game));
        assertEquals(game.stockfish(), games.findGameDataByID("81").stockfish());
        DatabaseManager.configureDatabase(); // Migration is idempotent and retains accounts/games.
        assertEquals(game, games.findGameDataByID("81"));
    }

    @Test void eloEngineResultIsAppliedExactlyOnce() {
        GameData game = game(StockfishOptions.Mode.ELO); games.addGameData(game); game.game().setGameOver(true);
        games.finishGame(game, WHITE); games.finishGame(game, WHITE);
        assertEquals(1516, users.findUserDataByUsername("Ada").elo());
        assertEquals(1500, users.findUserDataByUsername("Ben").elo());
        assertTrue(games.findGameDataByID("81").game().isGameOver());
        games.saveGame(game); games.finishGame(game, BLACK);
        assertEquals(1516, users.findUserDataByUsername("Ada").elo());
    }

    @Test void humanResultsUpdateBothPlayersAndManualGamesAreUnrated() {
        GameData game = game(null); games.addGameData(game); game.game().setGameOver(true); games.finishGame(game, BLACK);
        assertEquals(1484, users.findUserDataByUsername("Ada").elo()); assertEquals(1516, users.findUserDataByUsername("Ben").elo());
        games.clear(); game = game(StockfishOptions.Mode.SKILL); games.addGameData(game); game.game().setGameOver(true); games.finishGame(game, WHITE);
        assertEquals(1484, users.findUserDataByUsername("Ada").elo());
    }

    @Test void aLegacyHumanNamedStockfishIsRatedAsAHuman() {
        users.addUser(new UserData("Stockfish", "password", "email"));
        var game = new GameData(81, "Ada", "Stockfish", "Legacy account", new ChessGame());
        games.addGameData(game); game.game().setGameOver(true); games.finishGame(game, BLACK);
        assertEquals(1484, users.findUserDataByUsername("Ada").elo());
        assertEquals(1516, users.findUserDataByUsername("Stockfish").elo());
    }

    @Test void aDrawBetweenEqualPlayersLeavesRatingsUnchanged() {
        GameData game = game(null); games.addGameData(game); game.game().setGameOver(true); games.finishGame(game, null);
        assertEquals(1500, users.findUserDataByUsername("Ada").elo()); assertEquals(1500, users.findUserDataByUsername("Ben").elo());
    }
}
