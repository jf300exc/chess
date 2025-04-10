import ui.CommandLine;
import ui.ServerFacade;
import ui.WebSocketClient;

public class Main {

    private static final int PORT = 8080;

    public static void main(String[] args) {
        ServerFacade facade = new ServerFacade(PORT);
        WebSocketClient webSocketClient = new WebSocketClient(PORT);
        System.out.println("♕ Welcome to 240 Chess Client. Type Help to get started. ♕");
        CommandLine commandLine = new CommandLine(facade);
        commandLine.run();
    }
}