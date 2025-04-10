package ui;

import websocket.commands.UserGameCommand;

import java.util.Objects;
import java.util.Scanner;

public class GamePlay implements WebSocketListener {
    private final Scanner scanner = new Scanner(System.in);
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

    public void playGame(UserGameCommand connectRequest) throws Exception {
        this.userType = UserType.PLAYER;
        ws.connectClient();
        ws.sendString("Play Game Request Successful");
        ws.sendCommand(connectRequest);
        runGamePlayUI();
        ws.closeClient();
    }

    public void observeGame() throws Exception {
        this.userType = UserType.OBSERVER;
        runGamePlayUI();
        ws.connectClient();
        ws.sendString("Observe Game Request Successful");
        ws.closeClient();
    }

    private String userTypePromptString() {
        if (userType == UserType.PLAYER) {
            return "[PLAYING]";
        }
        return "[OBSERVING]";
    }

    private String getUserInput(String prompt) {
        System.out.print(Objects.requireNonNullElseGet(prompt, () -> userTypePromptString() + " >>> "));
        return scanner.nextLine().trim();
    }

    private void runGamePlayUI() throws Exception {
        for (;;) {
            String userInput = getUserInput(null);
            if (!matchGamePlayCommand(userInput)) {
                break;
            }
        }
    }

    private boolean matchGamePlayCommand(String command) throws Exception {
        if (command.isBlank()) {
            return true;
        }
        switch (command) {
            case "Help" -> displayGamePlayHelp();
            case "Redraw Chess Board" -> redrawBoard();
            case "Leave" -> {
                leaveGame();
                return false;
            }
            case "Make Move" -> gamePlayMakeMove();
            case "Resign" -> resignGame();
            case "Highlight", "Highlight Legal Moves" -> highlightMoves();
            default -> matchArbitraryCommand(command);
        }
        return true;
    }

    private void matchArbitraryCommand(String command) {
        System.out.println("Unknown command.");
    }

    private static void displayGamePlayHelp() {
        String helpMessage = """
                Available commands for GamePlay UI:
                    Help                    Displays this help message.
                    Redraw Chess Board      Redraws the chess board.
                    Leave                   Leave the game.
                    Make Move               Make a move. (Ex. a2a4)
                    Resign                  Forfeit the game.
                    Highlight Legal Moves   Highlights available moves.""";
        System.out.println(helpMessage);
    }

    private void redrawBoard() {
        throw new RuntimeException("Not implemented");
    }

    private void leaveGame() {
        throw new RuntimeException("Not implemented");
    }

    private void gamePlayMakeMove() {
        throw new RuntimeException("Not implemented");
    }

    private void resignGame() {
        throw new RuntimeException("Not implemented");
    }

    private void highlightMoves() {
        throw new RuntimeException("Not implemented");
    }
}
