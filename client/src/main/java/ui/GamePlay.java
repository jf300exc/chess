package ui;

public class GamePlay implements WebSocketListener {
    private WebSocketClient ws;
    private UserType userType;

    enum UserType {
        PLAYER,
        OBSERVER
    }

    public void setWebSocket(WebSocketClient ws) {
        this.ws = ws;
    }

    @Override
    public void onMessage(String message) {
        String received = "Websocket Message: " + message;
        System.out.println(received);
        // TODO: Forward to UI mechanics
    }

    public void playGame() throws Exception {
        this.userType = UserType.PLAYER;
        runGamePlayUI();
        ws.connectClient();
        ws.send("Play Game Request Successful");
        ws.closeClient();

    }

    public void observeGame() throws Exception {
        this.userType = UserType.OBSERVER;
        runGamePlayUI();
        ws.connectClient();
        ws.send("Observe Game Request Successful");
        ws.closeClient();
    }

    private void runGamePlayUI() throws Exception {

    }
}
