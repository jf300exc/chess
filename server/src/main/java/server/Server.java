package server;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dataaccess.DataAccessException;
import handler.Handler;
import spark.*;

import java.util.HashMap;
import java.util.Map;

import static spark.Spark.*;

public class Server {
    private final Handler handler = new Handler();
    private final Gson errorMessage = new Gson();
    private final Map<String, String> errorValues = new HashMap<>();

    public int run(int desiredPort) {
        Spark.port(desiredPort);

        Spark.staticFiles.location("web");

        // Lambda function to handle registration
        post("/user", (request, response) -> {
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
        });

        // Lambda function to login user
        post("/session", (request, response) -> {
            String jsonBody = request.body();
            String result;
            try {
                result = handler.logInUser(jsonBody);
                response.status(200);
            } catch (DataAccessException e) {
                result = handleLoginLogoutException(e, response);
            }
            return result;
        });

        // Lambda function to log out user
        delete("/session", (request, response) -> {
            String authToken = request.headers("Authorization");
            String result;
            try {
                result = handler.logOutUser(authToken);
                response.status(200);
            } catch (DataAccessException e) {
                result = handleLoginLogoutException(e, response);
            }
            return result;
        });

        // Lambda function to list games
        get("/game", (request, response) -> {
            String authToken = request.headers("Authorization");
            String result;
            try {
                result = handler.listGames(authToken);
                response.status(200);
            } catch (DataAccessException e) {
                result = handleLoginLogoutException(e, response);
            }
            return result;
        });

        // Lambda function to create game
        post("/game", (request, response) -> {
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
        });

        // Lambda function to join game
        put("/game", (request, response) -> {
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
        });

        // Lambda function to clear Database
        delete("/db", (request, response) -> {
            handler.clearDatabase();
            response.status(200);
            return "{}";
        });

        Spark.awaitInitialization();
        return Spark.port();
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
