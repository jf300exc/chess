package client;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import requests.ListGamesRequest;
import requests.LoginRequest;
import ui.ServerFacade;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

class ServerFacadeTransportTests {
    private HttpServer server;
    private ServerFacade facade;

    @BeforeEach
    void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        server.start();
        facade = new ServerFacade("localhost", server.getAddress().getPort());
    }

    @AfterEach
    void stopServer() {
        server.stop(0);
    }

    @Test
    void decodesSuccessfulJsonResponse() {
        server.createContext("/session", exchange -> send(exchange, 200,
                "{\"username\":\"ada\",\"authToken\":\"token-1\"}"));

        var result = facade.loginClient(new LoginRequest("ada", "secret"));

        Assertions.assertNotNull(result);
        Assertions.assertEquals("ada", result.username());
        Assertions.assertEquals("token-1", result.authToken());
        Assertions.assertNull(facade.getLastError());
    }

    @Test
    void sendsAuthorizationHeader() {
        server.createContext("/game", exchange -> {
            if (!"token-2".equals(exchange.getRequestHeaders().getFirst("Authorization"))) {
                send(exchange, 401, "{\"message\":\"Error: unauthorized\"}");
                return;
            }
            send(exchange, 200, "{\"games\":[]}");
        });
        facade.setAuthToken("token-2");

        var result = facade.listGamesClient(new ListGamesRequest("token-2"));

        Assertions.assertNotNull(result);
        Assertions.assertTrue(result.games().isEmpty());
    }

    @Test
    void exposesReadableServerError() {
        server.createContext("/session", exchange -> send(exchange, 401,
                "{\"message\":\"Error: unauthorized\"}"));

        var result = facade.loginClient(new LoginRequest("ada", "wrong"));

        Assertions.assertNull(result);
        Assertions.assertEquals("unauthorized", facade.getLastError());
    }

    private static void send(HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        exchange.sendResponseHeaders(status, bytes.length);
        try (var output = exchange.getResponseBody()) {
            output.write(bytes);
        }
    }
}
