package ui;

import adapters.*;
import chess.ChessBoard;
import chess.ChessGame;
import chess.ChessPiece;
import chess.ChessPosition;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import javax.websocket.*;
import java.io.IOException;
import java.net.URI;
import java.util.Map;

public class WebSocketClient extends Endpoint {
    private final WebSocketListener listener;
    private final int port;
    private final Gson gson;
    private Session session;


    public WebSocketClient(int port, WebSocketListener listener) {
        this.port = port;
        this.listener = listener;
        gson = new GsonBuilder()
                .registerTypeAdapter(ChessGame.class, new ChessGameAdapter())
                .registerTypeAdapter(ChessBoard.class, new ChessBoardAdapter())
                .registerTypeAdapter(ChessPiece.class, new ChessPieceAdapter())
                .registerTypeAdapter(ChessPosition.class, new ChessPositionAdapter())
                .registerTypeAdapter(
                        new TypeToken<Map<ChessGame.TeamColor, Map<ChessBoard.CastlePieceTypes, Map<ChessBoard.CastleType, Boolean>>>>(){}.getType(),
                        new CastleRequirementsAdapter())
                .create();
    }

    @Override
    public void onOpen(Session session, EndpointConfig endpointConfig) {
//        System.out.println("WebSocket connection opened");
    }

    @Override
    public void onClose(Session session, CloseReason closeReason) {
//        System.out.println("WebSocket connection closed");
    }

    public void connectClient() throws Exception {
        if (session != null && session.isOpen()) {
            System.err.println("Can't connect to WebSocket: Already Connected");
            return;
        }
        URI uri = new URI("ws://localhost:" + port + "/ws");
        WebSocketContainer container = ContainerProvider.getWebSocketContainer();
        this.session = container.connectToServer(this, uri);
        this.session.addMessageHandler(new MessageHandler.Whole<String>() {
            public void onMessage(String message) {
//                System.out.println("Message Received: message");
                listener.onMessage(message);
            }
        });
    }

    public boolean isSessionOpen() {
        return session != null && session.isOpen();
    }

    public void closeClient() throws IOException {
        try {
            if (isSessionOpen()) {
                session.close();
            }
        } catch (IOException e) {
            System.out.println("Error closing WebSocket");
            Terminal.stop();
        }
    }

    public void sendString(String message) throws Exception {
        if (isSessionOpen()) {
            this.session.getBasicRemote().sendText(message);
        } else {
            System.err.println("Can't send message: No WebSocket Session");
        }
    }

    public void sendCommand(Object command) throws Exception {
        var serial = convertToJson(command);
        sendString(serial);
    }

    private String convertToJson(Object command) {
        return gson.toJson(command);
    }
}
