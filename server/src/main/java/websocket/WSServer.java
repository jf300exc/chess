package websocket;

import org.eclipse.jetty.websocket.api.annotations.*;
import org.eclipse.jetty.websocket.api.*;

@WebSocket
public class WSServer {

    @OnWebSocketConnect
    public void onConnect(Session session) throws Exception {
        System.out.println("Websocket Connected with " + session);
    }

    @OnWebSocketMessage
    public void onMessage(Session session, String message) throws Exception {
        System.out.println("Websocket message received: " + message);
        String response = "WebSocket response: " + message;
        System.out.println("  Sending response: " + response);
        session.getRemote().sendString(response);
    }

    @OnWebSocketClose
    public void onClose(Session session, int statusCode, String reason) {
        System.out.println("Websocket Closed: " + reason);
    }
}
