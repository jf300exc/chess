import UI.CommandLine;
import UI.ServerFacade;
import chess.*;
import server.Server;

public class Main {
    public static void main(String[] args) {
        System.out.println("♕ Welcome to 240 Chess Client. Type Help to get started. ♕");
        Server server = new Server();
        ServerFacade facade = new ServerFacade(server.run(8080));
        CommandLine commandLine = new CommandLine(facade);
        commandLine.run();
    }
}