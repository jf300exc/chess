package handler;

import Requests.*;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dataaccess.DataAccessException;
import service.AuthService;
import service.GameService;
import service.UserService;

public class Handler {
    private static final Gson gson = new Gson();
    private final UserService userService = new UserService();
    private final AuthService authService = new AuthService();
    private final GameService gameService = new GameService();

    public String registerUser(String json) throws DataAccessException {
        RegisterRequest request = gson.fromJson(json, RegisterRequest.class);
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
        LoginRequest request = gson.fromJson(json, LoginRequest.class);
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
        JsonObject jsonObject = gson.fromJson(json, JsonObject.class);
        String gameName = "";
        if (jsonObject.has("gameName")) {
            gameName = jsonObject.get("gameName").getAsString();
        }
        if (isStringBlank(gameName) || isStringBlank(authToken)) {
            throw new DataAccessException("Error: bad request");
        }
        CreateGameRequest request = new CreateGameRequest(authToken, gameName);
        CreateGameResult result = gameService.createGame(request);
        if (!result.message().isEmpty()) {
            throw new DataAccessException(result.message());
        }
        return filterEmptyFields(result);
    }

    public String joinGame(String authToken, String json) throws DataAccessException {
        JsonObject jsonObject = gson.fromJson(json, JsonObject.class);
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

    public void clearDatabase() {
        userService.clearUserDataBase();
        authService.clearAuthDataBase();
        gameService.clearGameDataBase();
    }

    private String filterEmptyFields(Object obj) {
        JsonObject jsonObject = gson.toJsonTree(obj).getAsJsonObject();
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
