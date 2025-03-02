package handler;

import Requests.LoginRequest;
import Requests.RegisterRequest;
import Requests.RegisterResult;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import dataaccess.DataAccessException;
import service.AuthService;
import service.UserService;

public class Handler {
    private static final Gson gson = new Gson();
    private final UserService userService = new UserService();
    private final AuthService authService = new AuthService();

    public String registerUser(String json) throws DataAccessException {
        RegisterRequest request = gson.fromJson(json, RegisterRequest.class);
        if (request.username().isBlank() || request.password().isBlank() || request.email().isBlank()) {
            throw new DataAccessException("Error: bad request");
        }
        RegisterResult result = userService.register(request);
        if (!result.message().isEmpty()) {
            throw new DataAccessException(result.message());
        }
        JsonObject jsonObject = gson.toJsonTree(result).getAsJsonObject();
        jsonObject.entrySet().removeIf(key -> key.getValue().getAsString().isEmpty());

        return jsonObject.toString();
    }

    public String login(String json) throws DataAccessException {
        RegisterRequest request = gson.fromJson(json, LoginRequest.class);
        if
    }

    public void clearDatabase() {
        userService.clearUserDataBase();
        authService.clearAuthDataBase();
    }
}
