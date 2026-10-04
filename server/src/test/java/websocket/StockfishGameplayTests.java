package websocket;

import chess.*;
import com.google.gson.*;
import dataaccess.*;
import engine.ChessUci;
import model.*;
import org.eclipse.jetty.websocket.api.Session;
import org.junit.jupiter.api.*;
import websocket.commands.*;

import java.lang.reflect.Proxy;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import static org.junit.jupiter.api.Assertions.*;
import static chess.ChessGame.TeamColor.*;
import static websocket.commands.UserGameCommand.CommandType.*;

class StockfishGameplayTests {
    static AtomicInteger ids = new AtomicInteger(10000);
    final Gson gson = new Gson();
    final MemoryGameDAO games = new MemoryGameDAO();
    final MemoryAuthDAO auth = new MemoryAuthDAO();
    final Map<Session, List<JsonObject>> messages = new ConcurrentHashMap<>();
    final List<Session> sessions = new ArrayList<>();
    int id;
    WSServer server;

    @BeforeEach void setup() {
        id = ids.incrementAndGet();
        auth.addAuth(new AuthData("human", "Ada"));
        auth.addAuth(new AuthData("watch", "Observer"));
        auth.addAuth(new AuthData("fake-ai", "Stockfish"));
    }
    @AfterEach void cleanup() { sessions.forEach(session -> server.onClose(session, 1000, "test cleanup")); }

    void start(ChessGame.TeamColor ai, WSServer.MoveEngine engine) {
        games.addGameData(new GameData(id, ai == WHITE ? "Stockfish" : "Ada", ai == BLACK ? "Stockfish" : "Ada", "AI test",
                new ChessGame(), new StockfishPlayer(ai, StockfishOptions.Mode.ELO, 1500, null)));
        server = new WSServer(games, auth, engine) {
            @Override public void sendMessage(Session session, String message) {
                messages.computeIfAbsent(session, ignored -> new CopyOnWriteArrayList<>()).add(JsonParser.parseString(message).getAsJsonObject());
            }
        };
    }
    Session connect(String token) throws Exception {
        Session session = (Session) Proxy.newProxyInstance(Session.class.getClassLoader(), new Class[]{Session.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "hashCode" -> System.identityHashCode(proxy);
                    case "equals" -> proxy == args[0];
                    case "toString" -> "stockfish-test-session";
                    default -> throw new UnsupportedOperationException(method.getName());
                });
        sessions.add(session);
        command(session, CONNECT, token);
        return session;
    }
    void command(Session session, UserGameCommand.CommandType type, String token) throws Exception {
        server.onMessage(session, gson.toJson(new UserGameCommand(type, token, id)));
    }
    void move(Session session, String token, String uci) throws Exception {
        server.onMessage(session, gson.toJson(new MakeMoveCommand(MAKE_MOVE, token, id, ChessUci.move(uci))));
    }
    GameData game() { synchronized (service.GameLocks.forGame(id)) { return games.findGameDataByID("" + id); } }
    void await(java.util.function.BooleanSupplier condition) throws Exception {
        long end = System.nanoTime() + TimeUnit.SECONDS.toNanos(4);
        while (!condition.getAsBoolean() && System.nanoTime() < end) { Thread.sleep(10); }
        assertTrue(condition.getAsBoolean(), "Expected async engine update");
    }
    boolean has(Session session, String type) {
        return messages.getOrDefault(session, List.of()).stream().anyMatch(msg -> type.equals(msg.get("serverMessageType").getAsString()));
    }

    @Test void humanMoveGetsAnEngineReplyAndObserversReceiveBothBoards() throws Exception {
        start(BLACK, (game, player) -> ChessUci.move("e7e5"));
        Session human = connect("human"), watch = connect("watch");
        messages.clear();
        move(human, "human", "e2e4");
        await(() -> game().game().getTeamTurn() == WHITE);
        assertNotNull(game().game().getBoard().getPiece(new ChessPosition(5, 5)));
        assertTrue(has(human, "LOAD_GAME")); assertTrue(has(watch, "LOAD_GAME"));
        assertTrue(messages.get(watch).stream().filter(msg -> "LOAD_GAME".equals(msg.get("serverMessageType").getAsString())).count() >= 2);
    }

    @Test void whiteEngineStartsOnceDespiteMultipleConnections() throws Exception {
        var started = new CountDownLatch(1); var release = new CountDownLatch(1); var searches = new AtomicInteger();
        start(WHITE, (game, player) -> {
            searches.incrementAndGet(); started.countDown();
            try { release.await(3, TimeUnit.SECONDS); } catch (InterruptedException e) { throw new java.io.IOException(e); }
            return ChessUci.move("e2e4");
        });
        Session human = connect("human");
        assertTrue(started.await(2, TimeUnit.SECONDS));
        connect("watch"); command(human, CONNECT, "human"); release.countDown();
        await(() -> game().game().getTeamTurn() == BLACK);
        assertEquals(1, searches.get());
    }

    @Test void resignationCancelsAnInFlightEngineMove() throws Exception {
        var started = new CountDownLatch(1); var release = new CountDownLatch(1); var completed = new CountDownLatch(1);
        start(WHITE, (game, player) -> {
            started.countDown();
            try { release.await(3, TimeUnit.SECONDS); } catch (InterruptedException e) { throw new java.io.IOException(e); }
            completed.countDown(); return ChessUci.move("e2e4");
        });
        Session human = connect("human"); assertTrue(started.await(2, TimeUnit.SECONDS));
        command(human, RESIGN, "human"); release.countDown(); assertTrue(completed.await(2, TimeUnit.SECONDS));
        assertTrue(game().game().isGameOver());
        Thread.sleep(50);
        assertNotNull(game().game().getBoard().getPiece(new ChessPosition(2, 5)));
        assertNull(game().game().getBoard().getPiece(new ChessPosition(4, 5)));
    }

    @Test void engineFailureCanRetryOnReconnectAndAiSeatsCannotBeImpersonated() throws Exception {
        var attempts = new AtomicInteger();
        start(WHITE, (game, player) -> {
            if (attempts.incrementAndGet() == 1) { throw new java.io.IOException("Unavailable"); }
            return ChessUci.move("e2e4");
        });
        Session human = connect("human"); await(() -> has(human, "ERROR"));
        command(human, CONNECT, "human"); await(() -> game().game().getTeamTurn() == BLACK);
        Session fake = connect("fake-ai");
        move(fake, "fake-ai", "e7e5"); assertTrue(has(fake, "ERROR"));
        command(human, LEAVE, "human");
        assertEquals("Ada", game().blackUsername());
        command(human, CONNECT, "human"); move(human, "human", "e7e5");
        await(() -> attempts.get() >= 3);
    }

    @Test void illegalEngineMoveDoesNotCorruptPosition() throws Exception {
        start(WHITE, (game, player) -> ChessUci.move("e2e5"));
        Session human = connect("human"); await(() -> has(human, "ERROR"));
        assertEquals(new ChessGame(), game().game());
    }
}
