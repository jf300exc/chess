package handler;

import com.google.gson.*;
import dataaccess.*;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

class StockfishApiTests {
    Handler handler;
    String token;
    @BeforeEach void setup() throws Exception {
        handler = new Handler(); handler.clearDatabase();
        JsonObject registered = JsonParser.parseString(handler.registerUser("{\"username\":\"Ada\",\"password\":\"password\",\"email\":\"email\"}")).getAsJsonObject();
        token = registered.get("authToken").getAsString();
    }
    @AfterEach void cleanup() { handler.clearDatabase(); }
    @Test void profileExposesRatingWithoutPasswordAndReservesEngineName() throws Exception {
        var profile = JsonParser.parseString(handler.profile(token)).getAsJsonObject();
        assertEquals(1500, profile.get("elo").getAsInt()); assertFalse(profile.has("password"));
        assertThrows(DataAccessException.class, () -> handler.profile("invalid"));
        assertThrows(DataAccessException.class, () -> handler.registerUser("{\"username\":\"stockFISH\",\"password\":\"password\",\"email\":\"email\"}"));
    }
    @Test void malformedDifficultyIsABadRequest() {
        for (String options : new String[]{"{\"color\":\"BLACK\",\"mode\":\"SKILL\",\"skillLevel\":2.5}",
                "{\"color\":\"BLACK\",\"mode\":\"SKILL\",\"skillLevel\":21}",
                "{\"color\":\"BLACK\",\"mode\":\"ELO\",\"elo\":1500,\"skillLevel\":4}",
                "{\"color\":\"BLACK\",\"mode\":\"unknown\"}"}) {
            assertEquals("Error: bad request", assertThrows(DataAccessException.class,
                    () -> handler.createGame(token, "{\"gameName\":\"Invalid\",\"stockfish\":" + options + "}")).getMessage());
        }
        assertEquals("Error: bad request", assertThrows(DataAccessException.class, () -> handler.createGame(token, "null")).getMessage());
        assertTrue(new SQLGameDAO().findGameData().isEmpty());
    }
    @Test void realEngineCreateAndAddEndpointsKeepDifficultyAcrossReload() throws Exception {
        Assumptions.assumeTrue(System.getenv("STOCKFISH_PATH") != null);
        var result = JsonParser.parseString(handler.createGame(token,
                "{\"gameName\":\"Adaptive\",\"stockfish\":{\"color\":\"BLACK\",\"mode\":\"ELO\"}}")).getAsJsonObject();
        var game = new SQLGameDAO().findGameDataByID(result.get("gameID").getAsString());
        assertEquals("Ada", game.whiteUsername()); assertEquals(1500, game.stockfish().elo());
        var list = JsonParser.parseString(handler.listGames(token)).getAsJsonObject().getAsJsonArray("games");
        assertEquals("ELO", list.get(0).getAsJsonObject().getAsJsonObject("stockfish").get("mode").getAsString());
        result = JsonParser.parseString(handler.createGame(token, "{\"gameName\":\"Manual\"}")).getAsJsonObject();
        String id = result.get("gameID").getAsString();
        handler.joinGame(token, "{\"gameID\":" + id + ",\"playerColor\":\"WHITE\"}");
        handler.addStockfish(token, "{\"gameID\":" + id + ",\"stockfish\":{\"color\":\"BLACK\",\"mode\":\"SKILL\",\"skillLevel\":0}}");
        assertEquals(0, new SQLGameDAO().findGameDataByID(id).stockfish().skillLevel());
    }
}
