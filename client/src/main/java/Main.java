import UI.CommandLine;
import chess.*;

public class Main {
    public static void main(String[] args) {
        System.out.println("♕ Welcome to 240 Chess Client. Type Help to get started. ♕");
        CommandLine commandLine = new CommandLine();
        commandLine.run();
    }
}