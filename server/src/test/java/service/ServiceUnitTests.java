package service;

import requests.*;
import model.UserData;
import org.junit.jupiter.api.*;

import java.util.Collection;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ServiceUnitTests {
    private static UserService userService;
    private static AuthService authService;
    private static GameService gameService;

    @BeforeAll
    public static void init() {
        userService = new UserService();
        authService = new AuthService();
        gameService = new GameService();
    }

    @BeforeEach
    public void setup() {
        userService.clearUserDataBase();
        authService.clearAuthDataBase();
        gameService.clearGameDataBase();
    }

    @Test
    @Order(1)
    @DisplayName("Get User Database")
    public void getUserDatabase() {
        Assertions.assertNotNull(userService.getUserDataBase());
    }

    @Test
    @Order(2)
    @DisplayName("Register User")
    public void registerUser() {
        userService.register(new RegisterRequest("username", "password", "email"));
        Assertions.assertEquals(1, userService.getUserDataBase().getAllUserData().size());
        RegisterResult result = userService.register(new RegisterRequest("new-user", "password", "email"));
        Assertions.assertEquals(2, userService.getUserDataBase().getAllUserData().size());
        Assertions.assertFalse(result.authToken().isEmpty() || result.username().isEmpty());
    }

    @Test
    @Order(3)
    @DisplayName("Register Duplicate user")
    public void registerDuplicateUser() {
        userService.register(new RegisterRequest("username", "a", "a"));
        RegisterResult result = userService.register(new RegisterRequest("username", "b", "b"));
        Assertions.assertEquals(1, userService.getUserDataBase().getAllUserData().size());
        Assertions.assertTrue(result.authToken().isEmpty() && result.username().isEmpty());
        Assertions.assertFalse(result.message().isEmpty());
    }

    @Test
    @Order(4)
    @DisplayName("Clear User Database")
    public void clearUserDataBase() {
        userService.register(new RegisterRequest("username", "password", "email"));
        userService.clearUserDataBase();
        Collection<UserData> allUserData = userService.getUserDataBase().getAllUserData();
        Assertions.assertTrue(allUserData.isEmpty());
    }

    @Test
    @Order(5)
    @DisplayName("Log in User")
    public void loginUser() {
        var username = "username";
        var password = "password";
        userService.register(new RegisterRequest(username, password, "email"));
        var result = userService.login(new LoginRequest(username, password));
        Assertions.assertTrue(result.message().isEmpty());
        Assertions.assertFalse(result.username().isEmpty() || result.authToken().isEmpty());
    }

    @Test
    @Order(6)
    @DisplayName("Log in non-user")
    public void loginNonUser() {
        var username = "username";
        var password = "password";
        userService.register(new RegisterRequest(username, password, "email"));
        var result = userService.login(new LoginRequest("notAUser", password));
        Assertions.assertFalse(result.message().isEmpty());
        Assertions.assertTrue(result.username().isEmpty() && result.authToken().isEmpty());
    }

    @Test
    @Order(7)
    @DisplayName("Create Auth")
    public void authCreateAuth() {
        var authData = authService.createAuth("username");
        Assertions.assertFalse(authData.authToken().isEmpty() || authData.username().isEmpty());
        Assertions.assertEquals(1, authService.getAuthDatabase().getAllAuthData().size());
    }

    @Test
    @Order(8)
    @DisplayName("Create Auth No Username")
    public void authCreateAuthNoUsername() {
        Assertions.assertThrows(NullPointerException.class, () -> authService.createAuth(null));
        Assertions.assertEquals(0, authService.getAuthDatabase().getAllAuthData().size());
    }

    @Test
    @Order(9)
    @DisplayName("Clear Auth Database")
    public void authClearDatabase() {
        authService.createAuth("username");
        authService.clearAuthDataBase();
        Assertions.assertEquals(0, authService.getAuthDatabase().getAllAuthData().size());
    }

    @Test
    @Order(10)
    @DisplayName("Find auth token")
    public void authFindAuthToken() {
        var authToken = authService.createAuth("username").authToken();
        Assertions.assertFalse(authService.isAuthTokenUnavailable(authToken));
    }

    @Test
    @Order(11)
    @DisplayName("Find non auth token")
    public void authFindNonAuthToken() {
        authService.createAuth("username");
        Assertions.assertTrue(authService.isAuthTokenUnavailable("falseToken"));
    }

    @Test
    @Order(12)
    @DisplayName("Find AuthData by AuthToken")
    public void authFindByAuthToken() {
        var username = "username";
        var authToken = authService.createAuth(username).authToken();
        var authData = authService.findAuthDataByAuthToken(authToken);
        Assertions.assertEquals(username, authData.username());
        Assertions.assertEquals(authToken, authData.authToken());
    }

    @Test
    @Order(13)
    @DisplayName("Find AuthData by Non AuthToken")
    public void authFindByNonAuthToken() {
        var username = "username";
        authService.createAuth(username);
        var authData = authService.findAuthDataByAuthToken("falseToken");
        Assertions.assertNull(authData);
    }

    @Test
    @Order(14)
    @DisplayName("Get Auth Database")
    public void getAuthDatabase() {
        Assertions.assertNotNull(userService.getUserDataBase());
    }

    @Test
    @Order(15)
    @DisplayName("auth Log Out")
    public void authLogOut() {
        var registerResult = userService.register(new RegisterRequest("username", "password", "email"));
        var authToken = registerResult.authToken();
        var request = new LogoutRequest(authToken);
        var result = authService.logout(request);
        Assertions.assertTrue(result.message().isEmpty());
    }

    @Test
    @Order(16)
    @DisplayName("auth Invalid Log out")
    public void authLogOutInvalid() {
        userService.register(new RegisterRequest("username", "password", "email"));
        var request = new LogoutRequest("authToken");
        var result = authService.logout(request);
        Assertions.assertFalse(result.message().isEmpty());
    }

    @Test
    @Order(17)
    @DisplayName("Game Create Game")
    public void gameCreate() {
        var regResult = userService.register(new RegisterRequest("username", "password", "email"));
        var request = new CreateGameRequest(regResult.authToken(), "gameName");
        var result = gameService.createGame(request);
        Assertions.assertTrue(result.message().isEmpty());
        Assertions.assertFalse(result.gameID().isEmpty());
    }

    @Test
    @Order(18)
    @DisplayName("Game Create Invalid Game")
    public void gameCreateInvalid() {
        userService.register(new RegisterRequest("username", "password", "email"));
        var request = new CreateGameRequest("", "");
        var result = gameService.createGame(request);
        Assertions.assertFalse(result.message().isEmpty());
        Assertions.assertNull(result.gameID());
    }

    @Test
    @Order(19)
    @DisplayName("List Games")
    public void gameListGames() {
        var regResult = userService.register(new RegisterRequest("username", "password", "email"));
        var authToken = regResult.authToken();
        var request1 = new CreateGameRequest(authToken, "game1");
        var request2 = new CreateGameRequest(authToken, "game2");
        gameService.createGame(request1);
        gameService.createGame(request2);
        var result = gameService.listGames(new ListGamesRequest(authToken));
        Assertions.assertTrue(result.message().isEmpty());
        Assertions.assertEquals(2, result.games().size());
    }

    @Test
    @Order(20)
    @DisplayName("List Games unauthorized")
    public void gamesListUnauthorized() {
        var regResult = userService.register(new RegisterRequest("username", "password", "email"));
        var authToken = regResult.authToken();
        var request1 = new CreateGameRequest(authToken, "game1");
        var request2 = new CreateGameRequest(authToken, "game2");
        gameService.createGame(request1);
        gameService.createGame(request2);
        var result = gameService.listGames(new ListGamesRequest("falseToken"));
        Assertions.assertFalse(result.message().isEmpty());
        Assertions.assertNull(result.games());
    }

    @Test
    @Order(21)
    @DisplayName("Join Game")
    public void gamesJoin() {
        var regResult = userService.register(new RegisterRequest("username", "password", "email"));
        var authToken = regResult.authToken();
        var request1 = new CreateGameRequest(authToken, "game1");
        var request2 = new CreateGameRequest(authToken, "game2");
        gameService.createGame(request1);
        var gameID = gameService.createGame(request2).gameID();
        var result = gameService.joinGame(new JoinGameRequest(authToken, "WHITE", gameID));
        Assertions.assertTrue(result.message().isEmpty());
    }

    @Test
    @Order(22)
    @DisplayName("Join Game Twice")
    public void gameJoinTwice() {
        var regResult = userService.register(new RegisterRequest("username", "password", "email"));
        var authToken = regResult.authToken();
        var request1 = new CreateGameRequest(authToken, "game1");
        gameService.createGame(request1);
        var gameID = gameService.createGame(request1).gameID();
        gameService.joinGame(new JoinGameRequest(authToken, "WHITE", gameID));
        var result = gameService.joinGame(new JoinGameRequest(authToken, "WHITE", gameID));
        Assertions.assertFalse(result.message().isEmpty());
    }

    @Test
    @Order(23)
    @DisplayName("Game Clear Database")
    public void gameClearDatabase() {
        var regResult = userService.register(new RegisterRequest("user", "pass", "email"));
        var request = new CreateGameRequest(regResult.authToken(), "gameName");
        gameService.createGame(request);
        gameService.clearGameDataBase();
        var gameListRequest = new ListGamesRequest(regResult.authToken());
        Assertions.assertEquals(0, gameService.listGames(gameListRequest).games().size());
    }
}

