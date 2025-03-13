package server;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dataaccess.DataAccessException;
import dataaccess.DatabaseManager;
import handler.Handler;
import spark.*;

import java.util.HashMap;
import java.util.Map;

import static spark.Spark.*;

public class Server {
    private final Handler handler = new Handler();
    private final Gson errorMessage = new Gson();
    private final Map<String, String> errorValues = new HashMap<>();

    public Server() {
        // Create the database if it does not exist
        DatabaseManager.configureDatabase();
    }

    public int run(int desiredPort) {
        Spark.port(desiredPort);

        Spark.staticFiles.location("web");

        // Lambda function to handle registration
        post("/user", this::handleUserRegistration);

        // Lambda function to login user
        post("/session", this::handleLoginUser);

        // Lambda function to log out user
        delete("/session", this::handleLogoutUser);

        // Lambda function to list games
        get("/game", this::handleListGames);

        // Lambda function to create game
        post("/game", this::handleCreateGame);

        // Lambda function to join game
        put("/game", this::handleJoinGame);

        // Lambda function to clear Database
        delete("/db", this::handleClearDatabase);

        Spark.awaitInitialization();
        return Spark.port();
    }

    private Object handleUserRegistration(Request request, Response response) {
        String jsonBody = request.body();
        String result;
        try {
            result = handler.registerUser(jsonBody);
            response.status(200);
        } catch (DataAccessException e) {
            String message = e.getMessage();
            if (message.equals("Error: bad request")) {
                errorValues.put("message", message);
                response.status(400);
            } else if (message.equals("Error: already taken")) {
                errorValues.put("message", message);
                response.status(403);
            } else {
                errorValues.put("message", "Error: unknown error occurred " + message);
                response.status(500);
            }
            result = dumpMapToJson();
        }
        return result;
    }

    private Object handleLoginUser(Request request, Response response) {
        String jsonBody = request.body();
        String result;
        try {
            result = handler.logInUser(jsonBody);
            response.status(200);
        } catch (DataAccessException e) {
            result = handleLoginLogoutException(e, response);
        }
        return result;
    }

    private Object handleLogoutUser(Request request, Response response) {
        String authToken = request.headers("Authorization");
        String result;
        try {
            result = handler.logOutUser(authToken);
            response.status(200);
        } catch (DataAccessException e) {
            result = handleLoginLogoutException(e, response);
        }
        return result;
    }

    private Object handleListGames(Request request, Response response) {
        String authToken = request.headers("Authorization");
        String result;
        try {
            result = handler.listGames(authToken);
            response.status(200);
        } catch (DataAccessException e) {
            result = handleLoginLogoutException(e, response);
        }
        return result;
    }

    private Object handleCreateGame(Request request, Response response) {
        String authToken = request.headers("Authorization");
        String jsonBody = request.body();
        String result;
        try {
            result = handler.createGame(authToken, jsonBody);
            response.status(200);
        } catch (DataAccessException e) {
            result = handleLoginLogoutException(e, response);
        }
        return result;
    }

    private Object handleJoinGame(Request request, Response response) {
        String authToken = request.headers("Authorization");
        String jsonBody = request.body();
        String result;
        try {
            result = handler.joinGame(authToken, jsonBody);
            response.status(200);
        } catch (DataAccessException e) {
            String message = e.getMessage();
            switch (message) {
                case "Error: bad request", "Error: game does not exist" -> {
                    errorValues.put("message", message);
                    response.status(400);
                }
                case "Error: unauthorized" -> {
                    errorValues.put("message", message);
                    response.status(401);
                }
                case "Error: already taken" -> {
                    errorValues.put("message", message);
                    response.status(403);
                }
                default -> {
                    errorValues.put("message", "Error: unknown error occurred " + message);
                    response.status(500);
                }
            }
            result = dumpMapToJson();
        }
        return result;
    }

    private Object handleClearDatabase(Request request, Response response) {
        handler.clearDatabase();
        response.status(200);
        return "{}";
    }

    public void stop() {
        Spark.stop();
        Spark.awaitStop();
    }

    private String dumpMapToJson() {
        JsonElement element = errorMessage.toJsonTree(errorValues);
        JsonObject jsonObject = element.getAsJsonObject();
        errorValues.clear();
        return jsonObject.toString();
    }

    private String handleLoginLogoutException(DataAccessException e, Response response) {
        String message = e.getMessage();
        if (message.equals("Error: bad request")) {
            errorValues.put("message", message);
            response.status(400);
        } else if (message.equals("Error: unauthorized")) {
            errorValues.put("message", message);
            response.status(401);
        } else {
            errorValues.put("message", "Error: unknown error occurred " + message);
            response.status(500);
        }
        return dumpMapToJson();
    }
}
