package ui;

import javax.websocket.*;
import java.net.URI;

public class WebSocketClient extends Endpoint {

    private final WebSocketListener listener;
    private Session session;

    public WebSocketClient(int port, WebSocketListener listener) throws Exception {
        this.listener = listener;
        URI uri = new URI("ws://localhost:" + port + "/ws");
        WebSocketContainer container = ContainerProvider.getWebSocketContainer();
        this.session = container.connectToServer(this, uri);
        this.session.addMessageHandler(new MessageHandler.Whole<String>() {
            public void onMessage(String message) {
                listener.onMessage(message);
            }
        });
    }

    public void send(String message) throws Exception {
        this.session.getBasicRemote().sendText(message);
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
