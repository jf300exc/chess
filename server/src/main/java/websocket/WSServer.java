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
import dataaccess.GameDAO;
import dataaccess.SQLGameDAO;
import model.GameData;
import org.eclipse.jetty.websocket.api.annotations.*;
import org.eclipse.jetty.websocket.api.*;
import websocket.commands.MakeMoveCommand;
import websocket.commands.UserGameCommand;
import websocket.messages.LoadGameMessage;
import websocket.messages.NotificationMessage;
import websocket.messages.ServerMessage.*;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;

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
            .create();
    private final GameDAO gameDAO = new SQLGameDAO();
    private final Map<Integer, Set<Session>> connectedGamePlayers = new ConcurrentHashMap<>();
    private final Map<Integer, Set<Session>> connectedGameObservers = new ConcurrentHashMap<>();

    @OnWebSocketConnect
    public void onConnect(Session session) {
        System.out.println("Websocket Connected with " + session);
    }

    @OnWebSocketMessage
    public void onMessage(Session session, String message) throws Exception {
        System.out.println("Websocket message received: " + message);
        String response;
        JsonObject json;
        try {
            json = JsonParser.parseString(message).getAsJsonObject();
        } catch (Exception e) {
            System.out.println("Received String: " + message);
            return;
        }

        String commandType = json.get("commandType").getAsString();
        if (commandType.equals("CONNECT")) {
            System.out.println("Connected to " + session);
            UserGameCommand command = gson.fromJson(json, UserGameCommand.class);
            processConnectCommand(session, command);
        } else if (commandType.equals("MAKE_MOVE")) {
            System.out.println("Received MakeMoveCommand");
            MakeMoveCommand command = gson.fromJson(json, MakeMoveCommand.class);
            processMakeMoveCommand(session, command);
        } else if (commandType.equals("LEAVE")) {
            System.out.println("Received LeaveCommand");
            UserGameCommand command = gson.fromJson(message, UserGameCommand.class);
            processLeaveCommand(session, command);
        } else if (commandType.equals("RESIGN")) {
            System.out.println("Received ResignCommand");
            UserGameCommand command = gson.fromJson(message, UserGameCommand.class);
            processResignCommand(session, command);
        }
    }

    @OnWebSocketClose
    public void onClose(Session session, int statusCode, String reason) {
        System.out.println("Websocket Closed: " + reason);
    }

    public void sendMessage(Session session, String message) throws IOException {
        System.out.println("Sending Message: " + message);
        session.getRemote().sendString(message);
    }

    private void processConnectCommand(Session session, UserGameCommand command) throws IOException {
        String gameIDStr = Integer.toString(command.getGameID());
        GameData gameData = gameDAO.findGameDataByID(gameIDStr);
        LoadGameMessage message = new LoadGameMessage(ServerMessageType.LOAD_GAME, gameData);
        sendMessage(session, convertToJson(message));
        // TODO: Send a notification to all players connect and observing
        var message = new NotificationMessage(ServerMessageType.NOTIFICATION, )
        for (var playerSession : connectedGamePlayers.get(command.getGameID())) {
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
