import UI.CommandLine;
import UI.ServerFacade;

public class Main {
    public static void main(String[] args) {
        ServerFacade facade = new ServerFacade(8080);
        System.out.println("♕ Welcome to 240 Chess Client. Type Help to get started. ♕");
        CommandLine commandLine = new CommandLine(facade);
        commandLine.run();
    }
}