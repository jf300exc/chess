package ui;

import adapters.*;
import chess.*;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;
import websocket.commands.UserGameCommand;
import websocket.messages.LoadGameMessage;

import java.util.Map;
import java.util.Objects;
import java.util.Scanner;

public class GamePlay implements WebSocketListener {
    private final Scanner scanner = new Scanner(System.in);
    private final Gson gson = new GsonBuilder()
            .registerTypeAdapter(ChessGame.class, new ChessGameAdapter())
            .registerTypeAdapter(ChessBoard.class, new ChessBoardAdapter())
            .registerTypeAdapter(ChessPiece.class, new ChessPieceAdapter())
            .registerTypeAdapter(ChessPosition.class, new ChessPositionAdapter())
            .registerTypeAdapter(
                    new TypeToken<Map<ChessGame.TeamColor, Map<ChessBoard.CastlePieceTypes, Map<ChessBoard.CastleType, Boolean>>>>(){}.getType(),
                    new CastleRequirementsAdapter())
            .create();

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
        System.out.println("\n\nReceived Websocket Message");
        JsonObject json;
        try {
            json = JsonParser.parseString(message).getAsJsonObject();
        } catch (Exception e) {
            System.out.println("Received String: " + message);
            return;
        }
        String messageType = json.get("serverMessageType").getAsString();
        System.out.println("Received Message: " + messageType);
        switch (messageType) {
            case "LOAD_GAME" -> processLoadGameMessage(message);
            case "ERROR" -> processErrorMessage(message);
            case "NOTIFICATION" -> processNotificationMessage(message);
        }
    }

    void processLoadGameMessage(String message) {
        System.out.println("\n\n\n");
        LoadGameMessage loadGameMessage = gson.fromJson(message, LoadGameMessage.class);
        if (userType == UserType.PLAYER) {
            System.out.println(BoardDraw.drawBoard(loadGameMessage.getGameData().game(), ChessGame.TeamColor.BLACK));
        } else {
            System.out.println(BoardDraw.drawBoard(loadGameMessage.getGameData().game(), ChessGame.TeamColor.WHITE));
        }
    }

    void processErrorMessage(String message) {
        throw new RuntimeException("Not implemented");
    }

    void processNotificationMessage(String message) {
        throw new RuntimeException("Not implemented");
    }

    public void playGame(UserGameCommand connectRequest) throws Exception {
        this.userType = UserType.PLAYER;
        ws.connectClient();
        ws.sendString("Connection Request");
        ws.sendCommand(connectRequest);
//        runGamePlayUI();
        Terminal.start();
        ws.closeClient();
    }

    public void observeGame(UserGameCommand connectRequest) throws Exception {
        this.userType = UserType.OBSERVER;
        ws.connectClient();
        ws.sendString("Connection Request");
        ws.sendCommand(connectRequest);
        runGamePlayUI();
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
//        throw new RuntimeException("Not implemented");
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
