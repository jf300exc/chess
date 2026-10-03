package ui;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import requests.*;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

/**
 * HTTP API client shared by the command-line and desktop clients.
 *
 * <p>A single {@link HttpClient} is retained for the life of the facade so TCP
 * connections can be reused. Public methods retain the original null-on-error
 * contract; callers that want a user-facing explanation can read
 * {@link #getLastError()}.</p>
 */
public class ServerFacade {
    private static final Gson GSON = new Gson();
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(10);

    private final URI serverUri;
    private final HttpClient httpClient;
    private volatile String authToken;
    private volatile String lastError;

    public ServerFacade(int port) {
        this("localhost", port);
    }

    public ServerFacade(String host, int port) {
        this.serverUri = URI.create("http://" + host + ":" + port);
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .version(HttpClient.Version.HTTP_1_1)
                .build();
    }

    public void setAuthToken(String authToken) {
        this.authToken = authToken;
    }

    public String getAuthToken() {
        return authToken;
    }

    public String getLastError() {
        return lastError;
    }

    public RegisterResult registerClient(RegisterRequest request) {
        return request("POST", "/user", request, RegisterResult.class);
    }

    public LoginResult loginClient(LoginRequest request) {
        return request("POST", "/session", request, LoginResult.class);
    }

    public LogoutResult logoutClient(LogoutRequest request) {
        return request("DELETE", "/session", request, LogoutResult.class);
    }

    public CreateGameResult createGameClient(CreateGameRequest request) {
        return request("POST", "/game", request, CreateGameResult.class);
    }

    public ListGamesResult listGamesClient(ListGamesRequest request) {
        return request("GET", "/game", null, ListGamesResult.class);
    }

    public JoinGameResult joinGameClient(JoinGameRequest request) {
        return request("PUT", "/game", request, JoinGameResult.class);
    }

    private <T> T request(String method, String path, Object body, Class<T> responseClass) {
        lastError = null;
        try {
            HttpRequest.BodyPublisher publisher = body == null
                    ? HttpRequest.BodyPublishers.noBody()
                    : HttpRequest.BodyPublishers.ofString(GSON.toJson(body), StandardCharsets.UTF_8);
            HttpRequest.Builder builder = HttpRequest.newBuilder(serverUri.resolve(path))
                    .timeout(REQUEST_TIMEOUT)
                    .header("Accept", "application/json")
                    .method(method, publisher);
            if (body != null) {
                builder.header("Content-Type", "application/json; charset=utf-8");
            }
            String token = authToken;
            if (token != null && !token.isBlank()) {
                builder.header("Authorization", token);
            }

            HttpResponse<String> response = httpClient.send(
                    builder.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                lastError = readError(response.body(), response.statusCode());
                return null;
            }
            if (responseClass == null || response.body() == null || response.body().isBlank()) {
                return null;
            }
            return GSON.fromJson(response.body(), responseClass);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            lastError = "Request interrupted";
            return null;
        } catch (Exception e) {
            lastError = connectionMessage(e);
            return null;
        }
    }

    private String readError(String body, int status) {
        try {
            JsonObject json = JsonParser.parseString(body).getAsJsonObject();
            if (json.has("message")) {
                return json.get("message").getAsString().replaceFirst("^Error:\\s*", "");
            }
        } catch (Exception ignored) {
            // Fall through to a concise HTTP error when the server returns non-JSON.
        }
        return "Server returned HTTP " + status;
    }

    private String connectionMessage(Exception error) {
        String detail = error.getMessage();
        if (detail == null || detail.isBlank()) {
            detail = error.getClass().getSimpleName();
        }
        return "Cannot reach " + serverUri + ": " + detail;
    }
}
