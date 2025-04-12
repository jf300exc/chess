package websocket;

import adapters.*;
import chess.*;
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
                System.out.println("Command:\n" + json);
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
        System.out.println("Retrieving AuthData of session with authToken: " + command.getAuthToken());
        AuthData authData = authDAO.findAuthDataByAuthToken(command.getAuthToken());
        if (authData == null) {
            System.out.println("Received invalid authToken: " + command.getAuthToken());
            ErrorMessage error = new ErrorMessage(ServerMessageType.ERROR, "Invalid authToken");
            sendMessage(session, gson.toJson(error));
            return;
        }
        System.out.println("Retrieving game data for gameID: " + gameIDStr);
        GameData gameData = gameDAO.findGameDataByID(gameIDStr);
        if (gameData == null) {
            System.out.println("Received invalid gameID: " + gameIDStr);
            ErrorMessage error = new ErrorMessage(ServerMessageType.ERROR, "Invalid game ID: " + gameIDStr);
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
        if (getPlayerColorFromUsername(gameData, authData.username()) == null) {
            System.out.println("Did not find username as Game Player: Preparing observer joined notification");
            notificationMessage = authData.username() + " is observing this game";
            isObserving = true;
        } else {
            System.out.println("Found Username as Game Player: Preparing player joined notification");
            notificationMessage = authData.username() + " has joined the game";
            isObserving = false;
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
        ErrorMessage errorMessage = null;
        int gameID = command.getGameID();
        GameData gameData = gameDAO.findGameDataByID(Integer.toString(gameID));
        ChessMove move = command.getMove();
        ChessGame.TeamColor playerColor = null;

        // Validate
        AuthData authData = authDAO.findAuthDataByAuthToken(command.getAuthToken());
        if (authData == null) {
            System.out.println("Received invalid authToken: " + command.getAuthToken());
            errorMessage = new ErrorMessage(ServerMessageType.ERROR, "Invalid authToken");
        } else if (gameData == null) {
            System.out.println("Received invalid gameID: " + gameID);
            errorMessage = new ErrorMessage(ServerMessageType.ERROR, "Invalid game ID: " + gameID);
        } else if ((playerColor = getPlayerColorFromUsername(gameData, authData.username())) == null) {
            System.out.println("Move attempted with observer");
            errorMessage = new ErrorMessage(ServerMessageType.ERROR, "Can't make a move as an observer");
        } else if (gameData.game().isGameOver()) {
            System.out.println("Move attempted when game over");
            errorMessage = new ErrorMessage(ServerMessageType.ERROR, "Game is over.");
        } else if (gameData.game().getTeamTurn() != playerColor) {
            System.out.println("Move attempted with wrong player");
            errorMessage = new ErrorMessage(ServerMessageType.ERROR, "It's not your turn");
        } else {
            System.out.println("Attempting Move");
            try {
                gameData.game().makeMove(move);
            } catch (InvalidMoveException e) {
                errorMessage = new ErrorMessage(ServerMessageType.ERROR, e.getMessage());
            }
        }
        if (errorMessage != null) {
            sendMessage(session, gson.toJson(errorMessage));
            return;
        }

        // Move successful
        gameDAO.removeGameDataByGameID(gameData);
        gameDAO.addGameData(gameData);

        // Send LOAD_GAME Message to all Clients
        var loadGameMessage = new LoadGameMessage(ServerMessageType.LOAD_GAME, gameData);
        // Send Notification Message to all OTHER Clients
        ChessPiece.PieceType pieceType = gameData.game().getBoard().getPiece(move.getEndPosition()).getPieceType();
        var moveMessage = getMoveString(authData.username(), move, pieceType);
        var notificationMessage = new NotificationMessage(ServerMessageType.NOTIFICATION, moveMessage);

        // Checkmate StaleMate Notifications
        NotificationMessage secondNotification = null;
        ChessGame.TeamColor opponentColor = getOtherTeamColor(playerColor);
        String opponentUsername = getOtherPlayerUsername(gameData, playerColor);
        if (gameData.game().isInCheck(opponentColor)) {
            secondNotification = new NotificationMessage(ServerMessageType.NOTIFICATION, opponentUsername + " is in check");
        }
        if (gameData.game().isInCheckmate(opponentColor)) {
            secondNotification = new NotificationMessage(ServerMessageType.NOTIFICATION, opponentUsername + " is in checkmate");
        }
        if (gameData.game().isInStalemate(opponentColor)) {
            secondNotification = new NotificationMessage(ServerMessageType.NOTIFICATION, opponentUsername + " is in stalemate");
        }
        for (Session playerSession : connectedGamePlayers.get(gameID)) {
            sendMessage(playerSession, gson.toJson(loadGameMessage));
            if (!session.equals(playerSession)) {
                sendMessage(playerSession, gson.toJson(notificationMessage));
            }
            if (secondNotification != null) {
                sendMessage(playerSession, gson.toJson(secondNotification));
            }
        }
        for (Session observerSession : connectedGameObservers.get(gameID)) {
            sendMessage(observerSession, gson.toJson(loadGameMessage));
            sendMessage(observerSession, gson.toJson(notificationMessage));
            if (secondNotification != null) {
                sendMessage(observerSession, gson.toJson(secondNotification));
            }
        }

    }

    private void processLeaveCommand(Session session, UserGameCommand command) throws IOException {
        ErrorMessage errorMessage = null;
        int gameID = command.getGameID();
        GameData gameData = gameDAO.findGameDataByID(Integer.toString(gameID));
        AuthData authData = authDAO.findAuthDataByAuthToken(command.getAuthToken());
        if (authData == null) {
            System.out.println("Leave attempted with invalid authToken");
            errorMessage = new ErrorMessage(ServerMessageType.ERROR, "Invalid authToken");
        } else if (gameData == null) {
            System.out.println("Leave attempted with invalid gameID");
            errorMessage = new ErrorMessage(ServerMessageType.ERROR, "Invalid game ID: " + gameID);
        }
        if (errorMessage != null) {
            sendMessage(session, gson.toJson(errorMessage));
            return;
        }
        // Proceed
        ChessGame.TeamColor playerColor = null;
        if ((playerColor = getPlayerColorFromUsername(gameData, authData.username())) != null) {
            gameDAO.removeGameDataByGameID(gameData);
            switch (playerColor) {
                case WHITE -> gameData = GameData.updateGameDataUsers("WHITE", null, gameData);
                case BLACK -> gameData = GameData.updateGameDataUsers("BLACK", null, gameData);
            }
            gameDAO.addGameData(gameData);
        }
        System.out.println("Removing player from gameID: " + gameID);

        // Don't store this session for this gameID
        connectedGamePlayers.get(gameID).remove(session);
        connectedGameObservers.get(gameID).remove(session);

        // Don't store this gameID for this Session
        gameIDBySession.get(session).remove(gameID);

        var notificationMessage = new NotificationMessage(ServerMessageType.NOTIFICATION, authData.username() + " left the game.");
        for (Session playerSession : connectedGamePlayers.get(gameID)) {
                sendMessage(playerSession, gson.toJson(notificationMessage));
        }
        for (Session observerSession : connectedGameObservers.get(gameID)) {
                sendMessage(observerSession, gson.toJson(notificationMessage));
        }
    }

    private void processResignCommand(Session session, UserGameCommand command) throws IOException {
        ErrorMessage errorMessage = null;
        int gameID = command.getGameID();
        GameData gameData = gameDAO.findGameDataByID(Integer.toString(gameID));
        AuthData authData = authDAO.findAuthDataByAuthToken(command.getAuthToken());
        if (authData == null) {
            System.out.println("Resignation attempted with invalid authToken");
            errorMessage = new ErrorMessage(ServerMessageType.ERROR, "Invalid authToken");
        } else if (gameData == null) {
            System.out.println("Resignation attempted with invalid gameID");
            errorMessage = new ErrorMessage(ServerMessageType.ERROR, "Invalid game ID: " + gameID);
        } else if (gameData.game().isGameOver()) {
            System.out.println("Resignation attempted when game over");
            errorMessage = new ErrorMessage(ServerMessageType.ERROR, "Game is already over");
        } else if (getPlayerColorFromUsername(gameData, authData.username()) == null) {
            System.out.println("Resignation attempted with observer");
            errorMessage = new ErrorMessage(ServerMessageType.ERROR, "Can't resign as an observer. Try to leave instead.");
        } else if (!gameData.game().isGameOver()) {
            gameDAO.removeGameDataByGameID(gameData);
            gameData.game().setGameOver(true);
            gameDAO.addGameData(gameData);
        }
        // Send error
        if (errorMessage != null) {
            sendMessage(session, gson.toJson(errorMessage));
            return;
        }
        // Continue with notifications
        System.out.println("Resignation of user: " + authData.username());
        var notificationMessage = new NotificationMessage(ServerMessageType.NOTIFICATION, authData.username() + " has resigned");
        for (Session playerSession : connectedGamePlayers.get(gameID)) {
            sendMessage(playerSession, gson.toJson(notificationMessage));
        }
        for (Session observerSession : connectedGameObservers.get(gameID)) {
            sendMessage(observerSession, gson.toJson(notificationMessage));
        }
    }

    private boolean validateAuthData(String authToken) {
        return authToken != null && authDAO.findAuthDataByAuthToken(authToken) != null;
    }

    private boolean validateGameID(int gameID) {
        return validateGameID(Integer.toString(gameID));
    }

    private boolean validateGameID(String gameIDStr) {
        return gameDAO.findGameDataByID(gameIDStr) != null;
    }

    private String getMoveString(String username, ChessMove move, ChessPiece.PieceType pieceType) {
        String message = username + " moved " + pieceType.toString() + ": ";
        message = message + move.getStartPosition().toString();
        message = message + " -> " + move.getEndPosition().toString();
        return message;
    }

    private String getOtherPlayerUsername(GameData gameData, ChessGame.TeamColor playerColor) {
        return switch (playerColor) {
            case WHITE -> gameData.blackUsername();
            case BLACK -> gameData.whiteUsername();
        };
    }

    private ChessGame.TeamColor getOtherTeamColor(ChessGame.TeamColor teamColor) {
        return switch (teamColor) {
            case WHITE -> ChessGame.TeamColor.BLACK;
            case BLACK -> ChessGame.TeamColor.WHITE;
        };
    }

    private ChessGame.TeamColor getPlayerColorFromUsername(GameData gameData, String username) {
        if (username.equals(gameData.blackUsername())) {
            return ChessGame.TeamColor.BLACK;
        } else if (username.equals(gameData.whiteUsername())) {
            return ChessGame.TeamColor.WHITE;
        } else {
            return null;
        }
    }

    private String convertToJson(Object o) {
        return gson.toJson(o);
    }
}
