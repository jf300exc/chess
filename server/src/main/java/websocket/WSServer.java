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
import org.eclipse.jetty.websocket.api.annotations.*;
import org.eclipse.jetty.websocket.api.*;
import websocket.commands.MakeMoveCommand;
import websocket.commands.UserGameCommand;

import java.util.Map;

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

    @OnWebSocketConnect
    public void onConnect(Session session) throws Exception {
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
        if (commandType.equals("MAKE_MOVE")) {
            System.out.println("  Found MakeMoveCommand");
            MakeMoveCommand command = gson.fromJson(json, MakeMoveCommand.class);
            System.out.println("  Make Move: " + command.getCommandType());
            response = "\nWebSocket response: received make move command";
        } else {
            System.out.println("  Found UserGameCommand");
            UserGameCommand command = gson.fromJson(message, UserGameCommand.class);
            System.out.println("  UserGameCommand: " + command.getCommandType());
            response = "\nWebSocket response: received UserGameCommand";
        }

        System.out.println("  Sending response: " + response);
        session.getRemote().sendString(response);
    }

    @OnWebSocketClose
    public void onClose(Session session, int statusCode, String reason) {
        System.out.println("Websocket Closed: " + reason);
    }
}
