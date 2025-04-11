package websocket;

import adapters.*;
import chess.ChessBoard;
import chess.ChessGame;
import chess.ChessPiece;
import chess.ChessPosition;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;
import dataaccess.AuthDAO;
import dataaccess.GameDAO;
import dataaccess.SQLAuthDAO;
import dataaccess.SQLGameDAO;
import model.AuthData;
import model.GameData;
import org.eclipse.jetty.websocket.api.annotations.*;
import org.eclipse.jetty.websocket.api.*;
import websocket.commands.MakeMoveCommand;
import websocket.commands.UserGameCommand;
import websocket.messages.ErrorMessage;
import websocket.messages.LoadGameMessage;
import websocket.messages.NotificationMessage;
import websocket.messages.ServerMessage.*;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@WebSocket
public class WSServer {
    private final Gson gson = new GsonBuilder()
            .registerTypeAdapter(ChessGame.class, new ChessGameAdapter())
            .registerTypeAdapter(ChessBoard.class, new ChessBoardAdapter())
            .registerTypeAdapter(ChessPiece.class, new ChessPieceAdapter())
            .registerTypeAdapter(ChessPosition.class, new ChessPositionAdapter())
            .registerTypeAdapter(
                    new TypeToken<Map<ChessGame.TeamColor, Map<ChessBoard.CastlePieceTypes, Map<ChessBoard.CastleType, Boolean>>>>(){}.getType(),
                    new CastleRequirementsAdapter())
//            .setPrettyPrinting()
            .create();

    private final GameDAO gameDAO = new SQLGameDAO();
    private final AuthDAO authDAO = new SQLAuthDAO();

    private static final Map<Integer, Set<Session>> connectedGamePlayers = new ConcurrentHashMap<>();
    private static final Map<Integer, Set<Session>> connectedGameObservers = new ConcurrentHashMap<>();
    private static final Map<Session, Set<Integer>> gameIDBySession = new ConcurrentHashMap<>();

    @OnWebSocketConnect
    public void onConnect(Session session) {
        System.out.println("Websocket Connected with " + session);
    }

    @OnWebSocketMessage
    public void onMessage(Session session, String message) throws Exception {
        System.out.println("Websocket message received: " + message);
        JsonObject json;
        try {
            json = JsonParser.parseString(message).getAsJsonObject();
        } catch (Exception e) {
            System.out.println("Received String: " + message);
            return;
        }

        String commandType = json.get("commandType").getAsString();
        switch (commandType) {
            case "CONNECT" -> {
                System.out.println("Connected to " + session);
                UserGameCommand command = gson.fromJson(json, UserGameCommand.class);
                processConnectCommand(session, command);
            }
            case "MAKE_MOVE" -> {
                System.out.println("Received MakeMoveCommand");
                MakeMoveCommand command = gson.fromJson(json, MakeMoveCommand.class);
                processMakeMoveCommand(session, command);
            }
            case "LEAVE" -> {
                System.out.println("Received LeaveCommand");
                UserGameCommand command = gson.fromJson(message, UserGameCommand.class);
                processLeaveCommand(session, command);
            }
            case "RESIGN" -> {
                System.out.println("Received ResignCommand");
                UserGameCommand command = gson.fromJson(message, UserGameCommand.class);
                processResignCommand(session, command);
            }
        }
    }

    @OnWebSocketClose
    public void onClose(Session session, int statusCode, String reason) {
        System.out.println("Websocket Closed. Reason: " + reason);
        if (gameIDBySession.containsKey(session)) {
            int size = gameIDBySession.get(session).size();
            System.out.println("  Connection was in " + size + " games");
            for (int gameID : gameIDBySession.get(session)) {
                System.out.println("  Removing session from gameID: " + gameID);
                connectedGamePlayers.get(gameID).remove(session);
                connectedGameObservers.get(gameID).remove(session);
            }
        } else {
            System.out.println("  Connection was in no games.");
        }
    }

    public void sendMessage(Session session, String message) throws IOException {
        System.out.println("Sending Message: " + message);
        session.getRemote().sendString(message);
    }

    private void processConnectCommand(Session session, UserGameCommand command) throws IOException {
        System.out.println("Received ConnectCommand");
        int gameID = command.getGameID();
        String gameIDStr = Integer.toString(gameID);


        // Validate Connect Command
        System.out.println("Retrieving game data for gameID: " + gameIDStr);
        GameData gameData = gameDAO.findGameDataByID(gameIDStr);
        if (gameData == null) {
            System.out.println("Received invalid gameID: " + gameIDStr);
            ErrorMessage error = new ErrorMessage(ServerMessageType.ERROR, "Invalid game ID: " + gameIDStr);
            sendMessage(session, gson.toJson(error));
            return;
        }
        System.out.println("Retrieving AuthData of session with authToken: " + command.getAuthToken());
        AuthData authData = authDAO.findAuthDataByAuthToken(command.getAuthToken());
        if (authData == null) {
            System.out.println("Received invalid authToken: " + command.getAuthToken());
            ErrorMessage error = new ErrorMessage(ServerMessageType.ERROR, "Invalid authToken");
            sendMessage(session, gson.toJson(error));
            return;
        }

        // Proceed
        System.out.println("Preparing Load Game message");
        LoadGameMessage message = new LoadGameMessage(ServerMessageType.LOAD_GAME, gameData);
        System.out.println("Sending Load Game message");
        sendMessage(session, convertToJson(message));

        if (!connectedGamePlayers.containsKey(gameID)) {
            System.out.println("Initializing Player Set for gameID: " + gameID);
            connectedGamePlayers.put(gameID, Collections.synchronizedSet(new HashSet<>()));
        } else {
            System.out.println("Player Set for gameID exists: " + gameID);
        }
        if (!connectedGameObservers.containsKey(gameID)) {
            System.out.println("Initializing Observer Set for gameID: " + gameID);
            connectedGameObservers.put(gameID, Collections.synchronizedSet(new HashSet<>()));
        } else {
            System.out.println("Observer Set for gameID exists: " + gameID);
        }
        if (!gameIDBySession.containsKey(session)) {
            System.out.println("Initializing gameID Set for session");
            Set<Integer> gameIDSet = Collections.synchronizedSet(new HashSet<>());
            gameIDSet.add(gameID);
            gameIDBySession.put(session, gameIDSet);
        } else {
            System.out.println("gameID Set for session exists: " + gameID);
        }

        String notificationMessage;
        boolean isObserving;
        if (Objects.equals(authData.username(), gameData.blackUsername()) ||
                    Objects.equals(authData.username(), gameData.whiteUsername())) {
            System.out.println("Found Username as Game Player: Preparing player joined notification");
            notificationMessage = authData.username() + " has joined the game";
            isObserving = false;
        } else {
            System.out.println("Did not find username as Game Player: Preparing observer joined notification");
            notificationMessage = authData.username() + " is observing this game";
            isObserving = true;
        }

        var notification = new NotificationMessage(ServerMessageType.NOTIFICATION, notificationMessage);
        System.out.println("Sending Notifications to players if any");
        for (Session playerSession : connectedGamePlayers.get(gameID)) {
            sendMessage(playerSession, gson.toJson(notification));
        }
        System.out.println("Sending Notifications to observers if any");
        for (Session observerSession : connectedGameObservers.get(gameID)) {
            sendMessage(observerSession, gson.toJson(notification));
        }

        if (isObserving) {
            System.out.println("Adding Observer to gameID: " + gameID);
            connectedGameObservers.get(gameID).add(session);
        } else {
            System.out.println("Adding Player to gameID: " + gameID);
            connectedGamePlayers.get(gameID).add(session);
        }
    }

    private void processMakeMoveCommand(Session session, MakeMoveCommand command) throws IOException {
        throw new RuntimeException("Not implemented yet");
    }

    private void processLeaveCommand(Session session, UserGameCommand command) throws IOException {
        throw new RuntimeException("Not implemented yet");
    }

    private void processResignCommand(Session session, UserGameCommand command) throws IOException {
        throw new RuntimeException("Not implemented yet");
    }

    private String convertToJson(Object o) {
        return gson.toJson(o);
    }
}
