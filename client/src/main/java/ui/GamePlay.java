package ui;

import adapters.*;
import chess.*;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;
import websocket.commands.*;
import websocket.commands.UserGameCommand.*;
import websocket.messages.ErrorMessage;
import websocket.messages.LoadGameMessage;
import websocket.messages.NotificationMessage;

import java.util.Map;

public class GamePlay implements WebSocketListener {
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
    private String userAuthToken;
    private int currentGameID;

    enum UserType {
        PLAYER,
        OBSERVER
    }

    public void setWebSocket(WebSocketClient ws) {
        this.ws = ws;
    }

    @Override
    public void onMessage(String message) {
//        Terminal.addLogMessage("Received WebSocket message");
        JsonObject json;
        try {
            json = JsonParser.parseString(message).getAsJsonObject();
        } catch (Exception e) {
            Terminal.addLogMessage("Received String: " + message);
            return;
        }
        String messageType = json.get("serverMessageType").getAsString();
//        Terminal.addLogMessage("Received Message: " + messageType);
        switch (messageType) {
            case "LOAD_GAME" -> processLoadGameMessage(message);
            case "ERROR" -> processErrorMessage(message);
            case "NOTIFICATION" -> processNotificationMessage(message);
            default -> Terminal.addLogMessage("Received Message: " + message);
        }
    }

    void processLoadGameMessage(String message) {
        LoadGameMessage loadGameMessage = gson.fromJson(message, LoadGameMessage.class);
        ChessGame game = loadGameMessage.getGame().game();
        Terminal.setChessGame(game);
    }

    void processErrorMessage(String message) {
        ErrorMessage errorMessage = gson.fromJson(message, ErrorMessage.class);
        Terminal.addNotification("Error: " + errorMessage.getErrorMessage());
    }

    void processNotificationMessage(String message) {
        var notification = gson.fromJson(message, NotificationMessage.class);
        Terminal.addNotification(notification.getMessage());
    }

    public void playGame(UserGameCommand connectRequest, String playerColor) throws Exception {
        this.userType = UserType.PLAYER;
        this.userAuthToken = connectRequest.getAuthToken();
        this.currentGameID = connectRequest.getGameID();
        ws.connectClient();
        Terminal.start(playerColor);
        ws.sendCommand(connectRequest);
        runGamePlayUI();
        ws.closeClient();
        Terminal.addLogMessage("Stopping Terminal");
        Terminal.stop();
    }

    public void observeGame(UserGameCommand connectRequest) throws Exception {
        this.userType = UserType.OBSERVER;
        ws.connectClient();
        ws.sendString("Connection Request");
        Terminal.start("WHITE");
        ws.sendCommand(connectRequest);
        runGamePlayUI();
        ws.closeClient();
        Terminal.addLogMessage("Stopping Terminal");
        Terminal.stop();
    }

    private String userTypePromptString() {
        if (userType == UserType.PLAYER) {
            return "[PLAYING]";
        }
        return "[OBSERVING]";
    }

    private void runGamePlayUI() throws Exception {
        while (Terminal.notReadyForInput()) {
            Thread.onSpinWait();
        }
        for (;;) {
            var userInput = Terminal.getInput("[GAMEPLAY] >>> ");
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
        Terminal.addLogMessage("Unknown command: " + command);
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
        String[] messages = helpMessage.split("\n");
        for (String message : messages) {
            Terminal.addLogMessage(message);
        }
//        System.out.println(helpMessage);
    }

    private void redrawBoard() throws Exception {
        Terminal.refresh();
        while (Terminal.notReadyForInput()) {
            Thread.onSpinWait();
        }
    }

    private void leaveGame() throws Exception {
        var leaveCommand = new UserGameCommand(CommandType.LEAVE, userAuthToken, currentGameID);
        ws.sendCommand(leaveCommand);
    }

    private void gamePlayMakeMove() {
        throw new RuntimeException("Not implemented");
    }

    private void resignGame() throws Exception {
        var resignCommand = new UserGameCommand(CommandType.RESIGN, userAuthToken, currentGameID);
        ws.sendCommand(resignCommand);
    }

    private void highlightMoves() {
        throw new RuntimeException("Not implemented");
    }
}
