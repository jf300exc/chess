package client;

import UI.ServerFacade;
import dataaccess.SQLAuthDAO;
import dataaccess.SQLGameDAO;
import dataaccess.SQLUserDAO;
import org.junit.jupiter.api.*;
import requests.*;
import server.Server;


public class ServerFacadeTests {

    private static Server server;
    private static ServerFacade serverFacade;

    @BeforeAll
    public static void init() {
        server = new Server();
        var port = server.run(0);
        System.out.println("Started test HTTP server on " + port);
        serverFacade = new ServerFacade(port);
        try {
            Thread.sleep(1000); // Sleep for 1 second
        } catch (InterruptedException e) {
            Assertions.fail(e);
        }
    }

    @BeforeEach
    public void beforeEach() {
        clearDatabase();
    }

    @AfterAll
    static void stopServer() {
        server.stop();
    }


    @Test
    @Order(1)
    public void testRegisterClient() {
        RegisterResult result = serverFacade.registerClient(new RegisterRequest("user", "password", "email"));
        Assertions.assertNotNull(result);
    }

    @Test
    @Order(2)
    public void testRegisterClientInvalid() {
        RegisterResult result = serverFacade.registerClient(new RegisterRequest("user", "password", "email"));
        Assertions.assertNotNull(result);
        result = serverFacade.registerClient(new RegisterRequest("user", "pass", "em"));
        Assertions.assertNull(result);
        result = serverFacade.registerClient(new RegisterRequest("newUser", "", "email"));
        Assertions.assertNull(result);
    }

    @Test
    @Order(3)
    public void testLoginClient() {
        RegisterResult registerResult = serverFacade.registerClient(new RegisterRequest("user", "password", "email"));
        Assertions.assertNotNull(registerResult);
        LoginResult result = serverFacade.loginClient(new LoginRequest("user", "password"));
        Assertions.assertNotNull(result);
    }

    @Test
    @Order(4)
    public void testLoginClientInvalid() {
        LoginResult result = serverFacade.loginClient(new LoginRequest("user", "wrong password"));
        Assertions.assertNull(result);
    }

    @Test
    @Order(5)
    public void testLogoutClient() {
        RegisterResult registerResult = serverFacade.registerClient(new RegisterRequest("user", "password", "email"));
        Assertions.assertNotNull(registerResult);
        LoginResult loginResult = serverFacade.loginClient(new LoginRequest("user", "password"));
        Assertions.assertNotNull(loginResult);
        String authToken = loginResult.authToken();
        serverFacade.setAuthToken(authToken);
        LogoutResult result = serverFacade.logoutClient(new LogoutRequest(authToken));
        Assertions.assertNotNull(result);
        serverFacade.setAuthToken(null);
    }

    @Test
    @Order(6)
    public void testLogoutClientInvalid() {
        RegisterResult registerResult = serverFacade.registerClient(new RegisterRequest("user", "password", "email"));
        Assertions.assertNotNull(registerResult);
        LoginResult loginResult = serverFacade.loginClient(new LoginRequest("user", "password"));
        String authToken = loginResult.authToken();
        serverFacade.setAuthToken("invalid");
        LogoutResult result = serverFacade.logoutClient(new LogoutRequest(authToken));
        Assertions.assertNull(result);
        serverFacade.setAuthToken(null);
    }

    @Test
    @Order(7)
    public void testCreateGameClient() {
        RegisterResult registerResult = serverFacade.registerClient(new RegisterRequest("user", "password", "email"));
        Assertions.assertNotNull(registerResult);
        LoginResult loginResult = serverFacade.loginClient(new LoginRequest("user", "password"));
        String authToken = loginResult.authToken();
        serverFacade.setAuthToken(authToken);
        CreateGameResult createGameResult = serverFacade.createGameClient(new CreateGameRequest(authToken, "game"));
        Assertions.assertNotNull(createGameResult);
    }

    @Test
    @Order(8)
    public void testCreateGameClientInvalid() {
        RegisterResult registerResult = serverFacade.registerClient(new RegisterRequest("user", "password", "email"));
        Assertions.assertNotNull(registerResult);
        LoginResult loginResult = serverFacade.loginClient(new LoginRequest("user", "password"));
        String authToken = loginResult.authToken();
        serverFacade.setAuthToken(authToken);
        CreateGameResult createGameResult = serverFacade.createGameClient(new CreateGameRequest(authToken, ""));
        Assertions.assertNull(createGameResult);
    }

    @Test
    @Order(9)
    public void testListGamesClient() {
        RegisterResult registerResult = serverFacade.registerClient(new RegisterRequest("user", "password", "email"));
        String authToken = registerResult.authToken();
        serverFacade.setAuthToken(authToken);
        ListGamesResult listGamesResult = serverFacade.listGamesClient(new ListGamesRequest(authToken));
        Assertions.assertEquals(0, listGamesResult.games().size());
        CreateGameResult createGameResult = serverFacade.createGameClient(new CreateGameRequest(authToken, "game"));
        Assertions.assertNotNull(createGameResult);
        listGamesResult = serverFacade.listGamesClient(new ListGamesRequest(authToken));
        Assertions.assertEquals(1, listGamesResult.games().size());
    }

    @Test
    @Order(10)
    public void testListGamesClientInvalid() {
        RegisterResult registerResult = serverFacade.registerClient(new RegisterRequest("user", "password", "email"));
        Assertions.assertNotNull(registerResult);
        LoginResult loginResult = serverFacade.loginClient(new LoginRequest("user", "password"));
        Assertions.assertNotNull(loginResult);
        String authToken = loginResult.authToken();
        serverFacade.setAuthToken("invalid");
        CreateGameResult createGameResult = serverFacade.createGameClient(new CreateGameRequest(authToken, ""));
        Assertions.assertNull(createGameResult);
    }

    @Test
    @Order(11)
    public void testJoinGameClient() {
        RegisterResult registerResult = serverFacade.registerClient(new RegisterRequest("user", "password", "email"));
        Assertions.assertNotNull(registerResult);
        LoginResult loginResult = serverFacade.loginClient(new LoginRequest("user", "password"));
        String authToken = loginResult.authToken();
        serverFacade.setAuthToken(authToken);
        CreateGameResult createGameResult = serverFacade.createGameClient(new CreateGameRequest(authToken, "game"));
        Assertions.assertNotNull(createGameResult);
        ListGamesResult listGamesResult = serverFacade.listGamesClient(new ListGamesRequest(authToken));
        int gameID = listGamesResult.games().iterator().next().gameID();
        String gameIDStr = Integer.toString(gameID);
        JoinGameResult joinGameResult = serverFacade.joinGameClient(new JoinGameRequest(authToken, "BLACK", gameIDStr));
        Assertions.assertNotNull(joinGameResult);
        listGamesResult = serverFacade.listGamesClient(new ListGamesRequest(authToken));
        Assertions.assertNotNull(listGamesResult.games().iterator().next().blackUsername());
    }

    @Test
    @Order(12)
    public void testJoinGameClientInvalid() {
        RegisterResult registerResult = serverFacade.registerClient(new RegisterRequest("user", "password", "email"));
        Assertions.assertNotNull(registerResult);
        LoginResult loginResult = serverFacade.loginClient(new LoginRequest("user", "password"));
        String authToken = loginResult.authToken();
        serverFacade.setAuthToken(authToken);
        CreateGameResult createGameResult = serverFacade.createGameClient(new CreateGameRequest(authToken, "newGame"));
        Assertions.assertNotNull(createGameResult);
        ListGamesResult listGamesResult = serverFacade.listGamesClient(new ListGamesRequest(authToken));
        int gameID = listGamesResult.games().iterator().next().gameID();
        String gameIDStr = Integer.toString(gameID);
        JoinGameResult joinGameResult = serverFacade.joinGameClient(new JoinGameRequest(authToken, "BLACK", gameIDStr));
        Assertions.assertNotNull(joinGameResult);
        joinGameResult = serverFacade.joinGameClient(new JoinGameRequest(authToken, "RED", gameIDStr));
        Assertions.assertNull(joinGameResult);
        joinGameResult = serverFacade.joinGameClient(new JoinGameRequest(authToken, "BLACK", gameIDStr));
        Assertions.assertNull(joinGameResult);
    }

    private static void clearDatabase() {
        var userDAO = new SQLUserDAO();
        var authDAO = new SQLAuthDAO();
        var gameDAO = new SQLGameDAO();
        userDAO.clear();
        authDAO.clear();
        gameDAO.clear();
    }
}
