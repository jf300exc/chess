package ui;

import javax.websocket.*;
import java.net.URI;

public class WebSocketClient extends Endpoint {

    private Session session;

    public WebSocketClient(int port) throws Exception {
        URI uri = new URI("ws:://localhost:" + port + "/ws");
        WebSocketContainer container = ContainerProvider.getWebSocketContainer();
        this.session = container.connectToServer(this, uri);

        this.session.addMessageHandler(new MessageHandler.Whole<String>() {
            public void onMessage(String message) {
                System.out.println("Message Received: " + message);
                // TODO: Add ServerMessage functionality
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
