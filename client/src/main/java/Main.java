import ui.CommandLine;
import ui.ServerFacade;
import ui.Terminal;
import ui.WebSocketClient;

public class Main {

    private static final int PORT = 8080;

    public static void main(String[] args) {
//        ServerFacade httpFacade = new ServerFacade(PORT);
//
//        try {
//            CommandLine commandLine = new CommandLine(httpFacade);
//            WebSocketClient webSocketClient = new WebSocketClient(PORT, commandLine.gamePlay);
//            commandLine.gamePlay.setWebSocket(webSocketClient);
//            System.out.println("♕ Welcome to 240 Chess Client. Type Help to get started. ♕");
//            commandLine.run();
//        } catch (Exception e) {
//            System.err.println("Failed to initialize WebSocket");
//            System.err.println(e.getMessage());
//        }
        Terminal.start();
    }
}