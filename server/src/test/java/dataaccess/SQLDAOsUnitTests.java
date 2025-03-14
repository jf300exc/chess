package dataaccess;

import chess.ChessGame;
import chess.ChessMove;
import chess.ChessPosition;
import model.AuthData;
import model.GameData;
import model.UserData;
import org.junit.jupiter.api.*;

import java.util.Collection;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class SQLDAOsUnitTests {
    private static UserDAO userDAO;
    private static AuthDAO authDAO;
    private static GameDAO gameDAO;

    @BeforeAll
    public static void init() {
        userDAO = new SQLUserDAO();
        authDAO = new SQLAuthDAO();
        gameDAO = new SQLGameDAO();
        userDAO.clear();
        authDAO.clear();
        gameDAO.clear();
        userDAO = null;
        authDAO = null;
        gameDAO = null;
    }

    @BeforeEach
    public void setup() {
        userDAO = new SQLUserDAO();
        authDAO = new SQLAuthDAO();
        gameDAO = new SQLGameDAO();
    }

    @Test
    @Order(1)
    @DisplayName("Get User Database")
    public void sqlUserGetAllUserData() {
        Collection<UserData> users = userDAO.getAllUserData();
        Assertions.assertTrue(users.isEmpty());
        userDAO.addUser(new UserData("test", "test", "test"));
        users = userDAO.getAllUserData();
        Assertions.assertEquals(1, users.size());
        clearAll();
        users = userDAO.getAllUserData();
        Assertions.assertEquals(0, users.size());
    }

    @Test
    @Order(2)
    @DisplayName("Add a valid User")
    public void sqlUserAddValidUser() {
        userDAO.addUser(new UserData("test", "test", "test"));
        Collection<UserData> users = userDAO.getAllUserData();
        Assertions.assertEquals(1, users.size());
    }

    @Test
    @Order(3)
    @DisplayName("Add a duplicate User")
    public void sqlUserAddDuplicateUser() {
        Collection<UserData> users = userDAO.getAllUserData();
        Assertions.assertEquals(1, users.size());
        userDAO.addUser(new UserData("test", "test_", "test_"));
        users = userDAO.getAllUserData();
        Assertions.assertEquals(1, users.size());
    }

    @Test
    @Order(4)
    @DisplayName("Find User by Username")
    public void sqlUserFindUserByUsername() {
        Collection<UserData> users = userDAO.getAllUserData();
        Assertions.assertEquals(1, users.size());
        UserData data = userDAO.findUserDataByUsername("test");
        Assertions.assertNotNull(data);
    }

    @Test
    @Order(5)
    @DisplayName("Find NON User by Username")
    public void sqlUserFindNonUserByUsername() {
        Collection<UserData> users = userDAO.getAllUserData();
        Assertions.assertEquals(1, users.size());
        UserData data = userDAO.findUserDataByUsername("test_");
        Assertions.assertNull(data);
    }

    @Test
    @Order(6)
    @DisplayName("clear UserDAO")
    public void sqlUserClearUserDAO() {
        userDAO.clear();
        Collection<UserData> users = userDAO.getAllUserData();
        Assertions.assertEquals(0, users.size());
        clearAll();
    }

    @Test
    @Order(7)
    @DisplayName("Get Auth Database")
    public void sqlAuthGetAuthDatabase() {
        Collection<AuthData> auths = authDAO.getAllAuthData();
        Assertions.assertEquals(0, auths.size());
        authDAO.addAuth(new AuthData("test", "test"));
        auths = authDAO.getAllAuthData();
        Assertions.assertEquals(1, auths.size());
        clearAll();
        auths = authDAO.getAllAuthData();
        Assertions.assertEquals(0, auths.size());
    }

    @Test
    @Order(8)
    @DisplayName("Add Auth")
    public void sqlAuthAddAuth() {
        authDAO.addAuth(new AuthData("testToken", "test"));
        Collection<AuthData> auths = authDAO.getAllAuthData();
        Assertions.assertEquals(1, auths.size());
    }

    @Test
    @Order(9)
    @DisplayName("Add duplicate auth")
    public void sqlAuthAddDuplicateAuth() {
        Collection<AuthData> auths = authDAO.getAllAuthData();
        Assertions.assertEquals(1, auths.size());
        authDAO.addAuth(new AuthData("testToken", "test"));
        auths = authDAO.getAllAuthData();
        Assertions.assertEquals(1, auths.size());
    }

    @Test
    @Order(10)
    @DisplayName("Find auth Data by Token")
    public void sqlAuthFindAuthDataByToken() {
        Collection<AuthData> auths = authDAO.getAllAuthData();
        Assertions.assertEquals(1, auths.size());
        AuthData data = authDAO.findAuthDataByAuthToken("testToken");
        Assertions.assertNotNull(data);
    }

    @Test
    @Order(11)
    @DisplayName("Find auth Data by Non Token")
    public void sqlAuthFindAuthDataByNonToken() {
        Collection<AuthData> auths = authDAO.getAllAuthData();
        Assertions.assertEquals(1, auths.size());
        AuthData data = authDAO.findAuthDataByAuthToken("testToken1");
        Assertions.assertNull(data);
    }

    @Test
    @Order(12)
    @DisplayName("Delete Auth")
    public void sqlAuthDeleteAuth() {
        Collection<AuthData> auths = authDAO.getAllAuthData();
        Assertions.assertEquals(1, auths.size());
        authDAO.deleteAuth(new AuthData("testToken", "test"));
        auths = authDAO.getAllAuthData();
        Assertions.assertEquals(0, auths.size());
    }

    @Test
    @Order(13)
    @DisplayName("Delete NON Auth")
    public void sqlAuthDeleteNonAuth() {
        Collection<AuthData> auths = authDAO.getAllAuthData();
        Assertions.assertEquals(0, auths.size());
        authDAO.addAuth(new AuthData("testToken", "test"));
        auths = authDAO.getAllAuthData();
        Assertions.assertEquals(1, auths.size());
        authDAO.deleteAuth(new AuthData("testToken1", "test"));
        auths = authDAO.getAllAuthData();
        Assertions.assertEquals(1, auths.size());
    }

    @Test
    @Order(14)
    @DisplayName("Clear Auth")
    public void sqlAuthClearAuth() {
        Collection<AuthData> auths = authDAO.getAllAuthData();
        Assertions.assertEquals(1, auths.size());
        authDAO.clear();
        auths = authDAO.getAllAuthData();
        Assertions.assertEquals(0, auths.size());
        clearAll();
    }

    @Test
    @Order(15)
    @DisplayName("Find Game Data")
    public void sqlGameFindGameData() {
        Collection<GameData> gameDataList = gameDAO.findGameData();
        Assertions.assertEquals(0, gameDataList.size());
        int id = GameIDCounter.getNewGameID();
        ChessGame game = new ChessGame();
        GameData newGameData = new GameData(id, null, null, "newGame", game);
        gameDAO.addGameData(newGameData);
        gameDataList = gameDAO.findGameData();
        Assertions.assertEquals(1, gameDataList.size());
        clearAll();
        gameDataList = gameDAO.findGameData();
        Assertions.assertEquals(0, gameDataList.size());
    }

    @Test
    @Order(16)
    @DisplayName("Find Game Data by Id")
    public void sqlGameFindGameDataById() {
        Collection<GameData> gameDataList = gameDAO.findGameData();
        Assertions.assertEquals(0, gameDataList.size());
        int id = GameIDCounter.getNewGameID();
        ChessGame game = new ChessGame();
        GameData newGameData = new GameData(id, null, null, "newGame", game);
        gameDAO.addGameData(newGameData);
        gameDataList = gameDAO.findGameData();
        Assertions.assertEquals(1, gameDataList.size());

        String idStr = Integer.toString(id);
        GameData gameData = gameDAO.findGameDataByID(idStr);
        Assertions.assertNotNull(gameData);
    }

    @Test
    @Order(17)
    @DisplayName("Find Game Data by NON Id")
    public void sqlGameFindGameDataByNonId() {
        Collection<GameData> gameDataList = gameDAO.findGameData();
        Assertions.assertEquals(1, gameDataList.size());
        int id = GameIDCounter.getNewGameID();
        String idStr = Integer.toString(id);
        Assertions.assertNull(gameDAO.findGameDataByID(idStr));
        clearAll();
    }

    @Test
    @Order(18)
    @DisplayName("Add Game Data")
    public void sqlGameAddGameData() {
        Collection<GameData> gameDataList = gameDAO.findGameData();
        Assertions.assertEquals(0, gameDataList.size());
        int id = GameIDCounter.getNewGameID();
        ChessGame game = new ChessGame();
        GameData newGameData = new GameData(id, null, null, "newGame", game);
        gameDAO.addGameData(newGameData);
        gameDataList = gameDAO.findGameData();
        Assertions.assertEquals(1, gameDataList.size());
        id = GameIDCounter.getNewGameID();
        newGameData = new GameData(id, null, null, "newGame2", game);
        gameDAO.addGameData(newGameData);
        gameDataList = gameDAO.findGameData();
        Assertions.assertEquals(2, gameDataList.size());
    }

    @Test
    @Order(19)
    @DisplayName("Add Game twice")
    public void sqlGameAddGameTwice() {
        Collection<GameData> gameDataList = gameDAO.findGameData();
        Assertions.assertEquals(2, gameDataList.size());
        int id = GameIDCounter.getNewGameID();
        ChessGame game = new ChessGame();
        GameData newGameData = new GameData(id, null, null, "newGame3", game);
        gameDAO.addGameData(newGameData);
        gameDataList = gameDAO.findGameData();
        Assertions.assertEquals(3, gameDataList.size());
        game = new ChessGame();
        newGameData = new GameData(id, null, null, "newGame4", game);
        gameDAO.addGameData(newGameData);
        gameDataList = gameDAO.findGameData();
        Assertions.assertEquals(3, gameDataList.size());
    }

    @Test
    @Order(20)
    @DisplayName("Remove Game Data")
    public void sqlGameRemoveGameData() {
        Collection<GameData> gameDataList = gameDAO.findGameData();
        Assertions.assertEquals(3, gameDataList.size());
        int id = GameIDCounter.getNewGameID();
        ChessGame game = new ChessGame();
        GameData newGameData = new GameData(id, null, null, "newGame5", game);
        gameDAO.addGameData(newGameData);
        gameDataList = gameDAO.findGameData();
        Assertions.assertEquals(4, gameDataList.size());
        gameDAO.removeGameData(newGameData);
        gameDataList = gameDAO.findGameData();
        Assertions.assertEquals(3, gameDataList.size());
        clearAll();
    }

    @Test
    @Order(21)
    @DisplayName("Remove Wrong Game Data")
    public void sqlGameRemoveWrongGameData() {
        Collection<GameData> gameDataList = gameDAO.findGameData();
        Assertions.assertEquals(0, gameDataList.size());
        int id = GameIDCounter.getNewGameID();
        ChessGame game = new ChessGame();
        GameData newGameData = new GameData(id, null, null, "newGame0", game);
        gameDAO.addGameData(newGameData);
        gameDataList = gameDAO.findGameData();
        Assertions.assertEquals(1, gameDataList.size());
        id = GameIDCounter.getNewGameID();
        GameData wrongGameData = new GameData(id, null, null, "wrongGame", game);
        gameDAO.removeGameData(wrongGameData);
        gameDataList = gameDAO.findGameData();
        Assertions.assertEquals(1, gameDataList.size());
        clearAll();
    }

    @Test
    @Order(22)
    @DisplayName("DeSerialize Game Data")
    public void sqlGameDeSerializeGameData() {
        Collection<GameData> gameDataList = gameDAO.findGameData();
        Assertions.assertEquals(0, gameDataList.size());
        int id = GameIDCounter.getNewGameID();
        ChessGame game = new ChessGame();
        var startPos = new ChessPosition(2, 5);
        var endPos = new ChessPosition(3, 5);
        ChessMove move = new ChessMove(startPos, endPos, null);
        try {
            game.makeMove(move);
        } catch (Exception e) {
            Assertions.fail();
        }
        GameData newGameData = new GameData(id, null, null, "newGame0", game);
        gameDAO.addGameData(newGameData);
        gameDataList = gameDAO.findGameData();
        Assertions.assertEquals(1, gameDataList.size());
        GameData returnedData = gameDAO.findGameDataByID(Integer.toString(id));
        Assertions.assertEquals(returnedData, newGameData);
        Assertions.assertEquals(returnedData.game(), newGameData.game());
    }

    @Test
    @Order(23)
    @DisplayName("Serialize Game Data")
    public void sqlGameSerializeGameData() {
        ChessGame game = new ChessGame();
        var startPos = new ChessPosition(2, 7);
        var endPos = new ChessPosition(4, 7);
        ChessMove move = new ChessMove(startPos, endPos, null);
        try {
            game.makeMove(move);
        } catch (Exception e) {
            Assertions.fail();
        }
        SQLGameDAO gameDAO = new SQLGameDAO();
        String json = gameDAO.serializeChessGame(game);
        var returnedGame = gameDAO.deserializeChessGame(json);
        Assertions.assertEquals(game, returnedGame);
    }

    private static void clearAll() {
        userDAO.clear();
        authDAO.clear();
        gameDAO.clear();
    }
}
