package UI;

import com.google.gson.Gson;
import requests.*;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;

public class ServerFacade {
    private final String serverUrl;
    private String authToken = null;

    public ServerFacade(int port) {
        this.serverUrl = "http://localhost:" + port;
    }

    public void setAuthToken(String authToken) {
        this.authToken = authToken;
    }

    public String getAuthToken() {
        return authToken;
    }

    public RegisterResult registerClient(RegisterRequest registerRequest) {
        var path = "/user";
        try {
            return makeRequest("POST", path, registerRequest, RegisterResult.class);
        } catch (ResponseException e) {
            return null;
        }
    }

    public LoginResult loginClient(LoginRequest loginRequest) {
        var path = "/session";
        try {
            return makeRequest("POST", path, loginRequest, LoginResult.class);
        } catch (ResponseException e) {
            return null;
        }
    }

    public LogoutResult logoutClient(LogoutRequest logoutRequest) {
        var path = "/session";
        try {
            return makeRequest("DELETE", path, logoutRequest, LogoutResult.class);
        } catch (ResponseException e) {
            return null;
        }
    }

    public CreateGameResult createGameClient(CreateGameRequest createGameRequest) {
        var path = "/game";
        try {
            return makeRequest("POST", path, createGameRequest, CreateGameResult.class);
        } catch (ResponseException e) {
            return null;
        }
    }

    public ListGamesResult listGamesClient(ListGamesRequest listGamesRequest) {
        var path = "/game";
        try {
            return makeRequest("GET", path, listGamesRequest, ListGamesResult.class);
        } catch (ResponseException e) {
            return null;
        }
    }

    public JoinGameResult joinGameClient(JoinGameRequest joinGameRequest) {
        var path = "/game";
        try {
            return makeRequest("PUT", path, joinGameRequest, JoinGameResult.class);
        } catch (ResponseException e) {
            return null;
        }
    }

    private <T> T makeRequest(String method, String path, Object request, Class<T> responseClass) throws ResponseException {
        try {
            URL url = (new URI(serverUrl + path)).toURL();
            HttpURLConnection http = (HttpURLConnection) url.openConnection();
            http.setRequestMethod(method);

            if (!method.equals("GET")) {
                http.setDoOutput(true);
            }

            if (authToken != null && !authToken.isEmpty()) {
                http.setRequestProperty("Authorization", authToken);
            }

            if (request != null && !method.equals("GET")) {
                writeBody(request, http);
            }

            http.connect();
            throwIfNotSuccessful(http);
            return readBody(http, responseClass);
        } catch (ResponseException e) {
            throw e;
        } catch (Exception e) {
            throw new ResponseException(500, e.getMessage());
        }
    }

    private void writeBody(Object request, HttpURLConnection http) throws IOException {
        if (request != null) {
            http.addRequestProperty("Content-Type", "application/json");
            String reqData = new Gson().toJson(request);
            try (OutputStream reqBody = http.getOutputStream()) {
                reqBody.write(reqData.getBytes());
            }
        }
    }

    private void throwIfNotSuccessful(HttpURLConnection http) throws IOException {
        var status = http.getResponseCode();
        if (!isSuccessful(status)) {
            try(InputStream respError = http.getErrorStream()) {
                if (respError != null) {
                    throw ResponseException.fromJson(respError);
                }
            }
            throw new ResponseException(status, "other failure: " + status);
        }
    }

    private static <T> T readBody(HttpURLConnection http, Class<T> responseClass) throws IOException {
        T response = null;
        if (http.getContentLength() < 0) {
            try (InputStream respBody = http.getInputStream()) {
                InputStreamReader reader = new InputStreamReader(respBody);
                if (responseClass != null) {
                    response = new Gson().fromJson(reader, responseClass);
                }
            }
        }
        return response;
    }

    private boolean isSuccessful(int status) {
        return status == 200;
    }
}
