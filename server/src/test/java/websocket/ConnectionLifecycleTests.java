package websocket;

import chess.*;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dataaccess.*;
import model.AuthData;
import model.GameData;
import org.eclipse.jetty.websocket.api.Session;
import org.junit.jupiter.api.*;
import websocket.commands.MakeMoveCommand;
import websocket.commands.UserGameCommand;

import java.lang.reflect.Proxy;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static websocket.commands.UserGameCommand.CommandType.*;

class ConnectionLifecycleTests {
    private final Gson gson = new Gson();
    private final Map<Session, List<JsonObject>> messages = new HashMap<>();
    private final List<Session> sessions = new ArrayList<>();
    private final WSServer server = new WSServer() {
        @Override
        public void sendMessage(Session session, String message) {
            messages.computeIfAbsent(session, ignored -> new ArrayList<>())
                    .add(JsonParser.parseString(message).getAsJsonObject());
        }
    };

    @BeforeEach
    void setup() {
        DatabaseManager.configureDatabase();
        new SQLAuthDAO().clear();
        new SQLGameDAO().clear();
        new SQLAuthDAO().addAuth(new AuthData("white-token", "white"));
        new SQLAuthDAO().addAuth(new AuthData("observer-token", "observer"));
        new SQLGameDAO().addGameData(new GameData(71, "white", null, "Lifecycle", new ChessGame()));
    }

    @AfterEach
    void cleanup() {
        sessions.forEach(session -> server.onClose(session, 1000, "test cleanup"));
        new SQLAuthDAO().clear();
        new SQLGameDAO().clear();
    }

    @Test
    void movesStillBroadcastAfterLastObserverDisconnects() throws Exception {
        Session white = connect("white-token");
        Session observer = connect("observer-token");
        server.onClose(observer, 1000, "observer disconnected");
        messages.clear();

        move(white);

        assertLoadGame(white);
        assertFalse(messages.containsKey(observer));
        assertNotNull(new SQLGameDAO().findGameDataByID("71").game().getBoard().getPiece(new ChessPosition(4, 5)));
    }

    @Test
    void playerCanReconnectAfterLastPlayerDisconnects() throws Exception {
        Session original = connect("white-token");
        Session observer = connect("observer-token");
        server.onClose(original, 1000, "player disconnected");
        Session reconnected = connect("white-token");
        messages.clear();

        move(reconnected);

        assertLoadGame(reconnected);
        assertLoadGame(observer);
        assertFalse(messages.containsKey(original));
    }

    private Session connect(String token) throws Exception {
        Session session = (Session) Proxy.newProxyInstance(Session.class.getClassLoader(), new Class<?>[]{Session.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "hashCode" -> System.identityHashCode(proxy);
                    case "equals" -> proxy == args[0];
                    case "toString" -> "test-session";
                    default -> throw new UnsupportedOperationException(method.getName());
                });
        sessions.add(session);
        server.onMessage(session, gson.toJson(new UserGameCommand(CONNECT, token, 71)));
        return session;
    }

    private void move(Session session) throws Exception {
        server.onMessage(session, gson.toJson(new MakeMoveCommand(MAKE_MOVE, "white-token", 71,
                new ChessMove(new ChessPosition(2, 5), new ChessPosition(4, 5), null))));
    }

    private void assertLoadGame(Session session) {
        assertNotNull(messages.get(session));
        assertTrue(messages.get(session).stream().anyMatch(message ->
                message.get("serverMessageType").getAsString().equals("LOAD_GAME")));
        assertFalse(messages.get(session).stream().anyMatch(message ->
                message.get("serverMessageType").getAsString().equals("ERROR")));
    }
}
