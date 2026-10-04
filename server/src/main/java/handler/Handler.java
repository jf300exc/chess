package handler;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dataaccess.DataAccessException;
import requests.*;
import service.AuthService;
import service.GameService;
import service.UserService;

public class Handler {
    private static final Gson GSON = new Gson();
    private final UserService userService = new UserService();
    private final AuthService authService = new AuthService();
    private final GameService gameService = new GameService();

    public String registerUser(String json) throws DataAccessException {
        RegisterRequest request = GSON.fromJson(json, RegisterRequest.class);
        if (isStringBlank(request.username()) || isStringBlank(request.password()) || isStringBlank(request.email())) {
            throw new DataAccessException("Error: bad request");
        }
        RegisterResult result = userService.register(request);
        if (!result.message().isEmpty()) {
            throw new DataAccessException(result.message());
        }
        return filterEmptyFields(result);
    }

    public String logInUser(String json) throws DataAccessException {
        LoginRequest request = GSON.fromJson(json, LoginRequest.class);
        if (isStringBlank(request.username()) || isStringBlank(request.password())) {
            throw new DataAccessException("Error: bad request");
        }
        LoginResult result = userService.login(request);
        if (!result.message().isEmpty()) {
            throw new DataAccessException(result.message());
        }
        return filterEmptyFields(result);
    }

    public String logOutUser(String authToken) throws DataAccessException {
        LogoutRequest request = new LogoutRequest(authToken);
        if (isStringBlank(request.authToken())) {
            throw new DataAccessException("Error: bad request");
        }
        LogoutResult result = authService.logout(request);
        if (!result.message().isEmpty()) {
            throw new DataAccessException(result.message());
        }
        return filterEmptyFields(result);
    }

    public String listGames(String authToken) throws DataAccessException {
        ListGamesRequest request = new ListGamesRequest(authToken);
        if (isStringBlank(request.authToken())) {
            throw new DataAccessException("Error: bad request");
        }
        ListGamesResult result = gameService.listGames(request);
        if (!result.message().isEmpty()) {
            throw new DataAccessException(result.message());
        }
        return filterEmptyFields(result);
    }

    public String createGame(String authToken, String json) throws DataAccessException {
        CreateGameRequest request;
        try {
            JsonObject body = GSON.fromJson(json, JsonObject.class);
            String gameName = body.get("gameName").getAsString();
            if (isStringBlank(gameName) || isStringBlank(authToken)) { throw new IllegalArgumentException(); }
            request = new CreateGameRequest(authToken, gameName, stockfishOptions(body.get("stockfish")));
        } catch (RuntimeException e) { throw new DataAccessException("Error: bad request"); }
        CreateGameResult result = gameService.createGame(request);
        if (!result.message().isEmpty()) {
            throw new DataAccessException(result.message());
        }
        return filterEmptyFields(result);
    }

    public String joinGame(String authToken, String json) throws DataAccessException {
        JsonObject jsonObject = GSON.fromJson(json, JsonObject.class);
        String playerColor = "", gameID = "";
        if (jsonObject.has("playerColor")) {
            playerColor = jsonObject.get("playerColor").getAsString();
        }
        if (jsonObject.has("gameID")) {
            gameID = jsonObject.get("gameID").getAsString();
        }
        if (isStringBlank(playerColor) || isStringBlank(gameID) || isStringBlank(authToken)) {
            throw new DataAccessException("Error: bad request");
        }
        JoinGameRequest request = new JoinGameRequest(authToken, playerColor, gameID);
        JoinGameResult result = gameService.joinGame(request);
        if (!result.message().isEmpty()) {
            throw new DataAccessException(result.message());
        }
        return filterEmptyFields(result);
    }

    public String profile(String authToken) throws DataAccessException {
        var profile = gameService.profile(authToken);
        if (profile == null) { throw new DataAccessException("Error: unauthorized"); }
        return GSON.toJson(profile);
    }

    public String addStockfish(String authToken, String json) throws DataAccessException {
        AddStockfishRequest request;
        try {
            var body = GSON.fromJson(json, JsonObject.class);
            request = new AddStockfishRequest(authToken, body.get("gameID").getAsString(), stockfishOptions(body.get("stockfish")));
        } catch (RuntimeException e) { throw new DataAccessException("Error: bad request"); }
        JoinGameResult result = gameService.addStockfish(request);
        if (!result.message().isEmpty()) { throw new DataAccessException(result.message()); }
        websocket.WSServer.refreshStockfishGame(Integer.parseInt(request.gameID()));
        return filterEmptyFields(result);
    }

    private model.StockfishOptions stockfishOptions(JsonElement element) {
        if (element == null || element.isJsonNull()) { return null; }
        JsonObject json = element.getAsJsonObject();
        for (String field : new String[]{"elo", "skillLevel"}) {
            JsonElement value = json.get(field);
            if (value != null && !value.isJsonNull()) {
                if (!value.isJsonPrimitive() || !value.getAsJsonPrimitive().isNumber()) { throw new IllegalArgumentException(); }
                value.getAsBigDecimal().intValueExact(); // Reject fractional values instead of Gson's integer truncation.
            }
        }
        var options = GSON.fromJson(json, model.StockfishOptions.class);
        options.validate();
        return options;
    }

    public void clearDatabase() {
        userService.clearUserDataBase();
        authService.clearAuthDataBase();
        gameService.clearGameDataBase();
    }

    private String filterEmptyFields(Object obj) {
        JsonObject jsonObject = GSON.toJsonTree(obj).getAsJsonObject();
        jsonObject.entrySet().removeIf(entry -> isEmptyString(entry.getValue()));
        return jsonObject.toString();
    }

    private boolean isEmptyString (JsonElement value) {
        return value.isJsonPrimitive() && value.getAsJsonPrimitive().isString() &&
                value.getAsString().isEmpty();
    }

    private boolean isStringBlank(String str) {
        return str == null || str.isBlank();
    }
}
