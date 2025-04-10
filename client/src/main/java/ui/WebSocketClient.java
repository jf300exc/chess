package ui;

import javax.websocket.*;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

public class WebSocketClient extends Endpoint {

    private final WebSocketListener listener;
    private final int port;
    private Session session;

    public WebSocketClient(int port, WebSocketListener listener) {
        this.port = port;
        this.listener = listener;
    }

//    public WebSocketClient(int port, WebSocketListener listener) throws Exception {
//        this.listener = listener;
//        URI uri = new URI("ws://localhost:" + port + "/ws");
//        WebSocketContainer container = ContainerProvider.getWebSocketContainer();
//        this.session = container.connectToServer(this, uri);
//        this.session.addMessageHandler(new MessageHandler.Whole<String>() {
//            public void onMessage(String message) {
//                listener.onMessage(message);
//            }
//        });
//    }

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
                listener.onMessage(message);
            }
        });

    }

    public void closeClient() throws IOException {
        if (session != null && session.isOpen()) {
            session.close();
        }
    }

    public void send(String message) throws Exception {
        if (session != null && session.isOpen()) {
            this.session.getBasicRemote().sendText(message);
        } else {
            System.err.println("Can't send message: No WebSocket Session");
        }
    }

    @Override
    public void onOpen(Session session, EndpointConfig endpointConfig) {
        System.out.println("WebSocket connection opened");
    }

    @Override
    public void onClose(Session session, CloseReason closeReason) {
        System.out.println("WebSocket connection closed");
    }
}
