package ui;

import adapters.*;
import chess.*;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;
import model.GameData;
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

    public enum UserType {
        PLAYER,
        OBSERVER
    }

    public void setWebSocket(WebSocketClient ws) {
        this.ws = ws;
    }

    @Override
    public void onMessage(String message) {
        JsonObject json;
        try {
            json = JsonParser.parseString(message).getAsJsonObject();
        } catch (Exception e) {
            Terminal.addLogMessage("Received String: " + message);
            return;
        }
        String messageType = json.get("serverMessageType").getAsString();
        switch (messageType) {
            case "LOAD_GAME" -> processLoadGameMessage(message);
            case "ERROR" -> processErrorMessage(message);
            case "NOTIFICATION" -> processNotificationMessage(message);
            default -> Terminal.addLogMessage("Received Message: " + message);
        }
    }

    void processLoadGameMessage(String message) {
        LoadGameMessage loadGameMessage = gson.fromJson(message, LoadGameMessage.class);
        GameData gameData = loadGameMessage.getGame();
        Terminal.setChessGame(gameData.game(), gameData.gameName());
        String currentTeamTurn = gameData.game().getTeamTurn().toString();
        Terminal.addLogMessage("It is " + currentTeamTurn + "'s turn");
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
        this.userAuthToken = connectRequest.getAuthToken();
        this.currentGameID = connectRequest.getGameID();
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
            var prompt = userTypePromptString();
            var userInput = Terminal.getInput(prompt + " >>> ");
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
            case "help", "Help" -> displayGamePlayHelp();
            case "redraw", "redraw board", "Redraw Chess Board" -> redrawBoard();
            case "l", "leave", "Leave" -> {
                leaveGame();
                return false;
            }
            case "m", "move", "make move", "Make Move" -> gamePlayMakeMove();
            case "resign", "Resign" -> resignGame();
            case "highlight", "Highlight", "highlight moves", "Highlight Legal Moves" -> highlightMoves();
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
    }

    private void redrawBoard() {
        Terminal.refresh();
        while (Terminal.notReadyForInput()) {
            Thread.onSpinWait();
        }
    }

    private void leaveGame() throws Exception {
        var leaveCommand = new UserGameCommand(CommandType.LEAVE, userAuthToken, currentGameID);
        ws.sendCommand(leaveCommand);
    }

    private void gamePlayMakeMove() throws Exception {
        if (userType == UserType.OBSERVER) {
            Terminal.addLogMessage("Cannot Make Move as OBSERVER");
            return;
        }
        Terminal.addLogMessage("Enter Move (Ex: a2a4)");
        String userInput;
        ChessMove move = null;
        while (move == null) {
            userInput = Terminal.getInput("Enter Move: ");
            if (userInput.trim().isEmpty()) {
                Terminal.addLogMessage("Aborting Move");
                return;
            }
            move = validateMoveString(userInput);
        }
        ChessGame gameCopy = Terminal.getChessGame();
        if (gameCopy == null) {
            Terminal.addLogMessage("Invalid Game State. Try Again.");
            return;
        }
        if (moveIsPromotion(move, gameCopy)) {
            move = getPromotionMoveFromUser(move);
        }
        var moveCommand = new MakeMoveCommand(CommandType.MAKE_MOVE, userAuthToken, currentGameID, move);
        ws.sendCommand(moveCommand);
    }

    private ChessMove validateMoveString(String move) {
        ChessMove resultMove;
        move = move.trim();
        if (move.length() != 4) {
            resultMove = null;
        } else {
            char startColChar = move.charAt(0);
            char startRowChar = move.charAt(1);
            char endColChar = move.charAt(2);
            char endRowChar = move.charAt(3);
            if (startColChar < 'a' || startColChar > 'h') {
                resultMove = null;
            }
            else if (startRowChar < '1' || startRowChar > '8') {
                resultMove = null;
            }
            else if (endColChar < 'a' || endColChar > 'h') {
                resultMove = null;
            }
            else if (endRowChar < '1' || endRowChar > '8') {
                resultMove = null;
            } else {
                ChessPosition startPosition = new ChessPosition(startRowChar - '0', startColChar - 'a' + 1);
                ChessPosition endPosition = new ChessPosition(endRowChar - '0', endColChar - 'a' + 1);
                resultMove = new ChessMove(startPosition, endPosition, null);
            }
        }
        if (resultMove == null) {
            Terminal.addLogMessage("Invalid Move Syntax. Try a move such as 'a2a4'");
        }
        return resultMove;
    }

    private boolean moveIsPromotion(ChessMove move, ChessGame game) {
        try {
            ChessMove promotionAttemptMove = new ChessMove(move.getStartPosition(), move.getEndPosition(), ChessPiece.PieceType.QUEEN);
            game.makeMove(promotionAttemptMove);
        } catch (InvalidMoveException e) {
            return false;
        }
        return true;
    }

    private ChessMove getPromotionMoveFromUser(ChessMove move) {
        // Reassign move object
        String userInput = null;
        ChessPiece.PieceType promotionType = null;
        while (userInput == null || userInput.isBlank() || promotionType == null) {
            Terminal.addLogMessage("Promotion Move Required");
            Terminal.addLogMessage("1. QUEEN");
            Terminal.addLogMessage("2. ROOK");
            Terminal.addLogMessage("3. KNIGHT");
            Terminal.addLogMessage("4. BISHOP");
            userInput = Terminal.getInput("Choose Piece Number: ").trim();
            if (userInput.length() != 1 || userInput.charAt(0) < '1' || userInput.charAt(0) > '4') {
                userInput = null;
            } else {
                promotionType = switch (userInput) {
                    case "1" -> ChessPiece.PieceType.QUEEN;
                    case "2" -> ChessPiece.PieceType.ROOK;
                    case "3" -> ChessPiece.PieceType.KNIGHT;
                    case "4" -> ChessPiece.PieceType.BISHOP;
                    default -> null;
                };
            }
        }
        return new ChessMove(move.getStartPosition(), move.getEndPosition(), promotionType);
    }

    private void resignGame() throws Exception {
        if (userType == UserType.OBSERVER) {
            Terminal.addLogMessage("Cannot Resign as OBSERVER");
            return;
        }
        String confirmation = Terminal.getInput("Confirm Resign? y/n: ");
        if (confirmation.equalsIgnoreCase("y")) {
            var resignCommand = new UserGameCommand(CommandType.RESIGN, userAuthToken, currentGameID);
            ws.sendCommand(resignCommand);
        } else {
            Terminal.addLogMessage("Aborting Resign");
        }
    }

    private void highlightMoves() {

        String positionString = null;
        ChessPosition startPosition = null;
        while (positionString == null || positionString.isBlank() || startPosition == null) {
            Terminal.addLogMessage("Enter Start Position (Ex: a1)");
            positionString = Terminal.getInput("Enter Start Position: ");
            if (positionString.trim().isEmpty()) {
                Terminal.addLogMessage("Aborting Highlighting");
                return;
            }
            startPosition = validatePositionString(positionString);
        }
        Terminal.drawHighlights(startPosition);
    }

    private ChessPosition validatePositionString(String positionString) {
        positionString = positionString.trim();
        ChessPosition position;
        if (positionString.length() != 2) {
            position = null;
        } else {
            char startColChar = positionString.charAt(0);
            char startRowChar = positionString.charAt(1);
            if (startColChar < 'a' || startColChar > 'h') {
                position = null;
            } else if (startRowChar < '1' || startRowChar > '8') {
                position = null;
            } else {
                position = new ChessPosition(startRowChar - '0', startColChar - 'a' + 1);
            }
        }
        if (position == null) {
            Terminal.addLogMessage("Invalid Position");
        }
        return position;
    }
}
