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
import java.util.Comparator;
import java.util.stream.Collectors;

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
    private final CliConsole console;
    private final BoardInteraction interaction = new BoardInteraction();
    private ChessGame.TeamColor playerTeam;
    private String userAuthToken;
    private int currentGameID;

    public enum UserType {
        PLAYER,
        OBSERVER
    }

    public GamePlay() {
        this(CliConsole.open(true, true));
    }

    public GamePlay(CliConsole console) {
        this.console = console;
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
        try {
            String messageType = json.get("serverMessageType").getAsString();
            switch (messageType) {
                case "LOAD_GAME" -> processLoadGameMessage(message);
                case "ERROR" -> processErrorMessage(message);
                case "NOTIFICATION" -> processNotificationMessage(message);
                default -> Terminal.addLogMessage("Received Message: " + message);
            }
        } catch (RuntimeException e) {
            Terminal.addNotification("Received an invalid server message.");
        }
    }

    void processLoadGameMessage(String message) {
        LoadGameMessage loadGameMessage = gson.fromJson(message, LoadGameMessage.class);
        GameData gameData = loadGameMessage.getGame();
        interaction.clear();
        Terminal.setChessGame(gameData.game(), gameData.gameName());
        String status = gameData.game().isGameOver() ? "Game over"
                : "It is " + gameData.game().getTeamTurn() + "'s turn";
        Terminal.addLogMessage(status);
    }

    void processErrorMessage(String message) {
        ErrorMessage errorMessage = gson.fromJson(message, ErrorMessage.class);
        interaction.clear();
        Terminal.drawHighlights(null);
        Terminal.addNotification("Error: " + errorMessage.getErrorMessage());
    }

    void processNotificationMessage(String message) {
        var notification = gson.fromJson(message, NotificationMessage.class);
        Terminal.addNotification(notification.getMessage());
        if (notification.getMessage().endsWith(" has resigned")) {
            ChessGame game = Terminal.getChessGame();
            if (game != null) {
                interaction.clear();
                game.setGameOver(true);
                Terminal.setChessGame(game, null);
                Terminal.addLogMessage("Game over");
            }
        }
    }

    public void playGame(UserGameCommand connectRequest, String playerColor) throws Exception {
        this.userType = UserType.PLAYER;
        this.playerTeam = ChessGame.TeamColor.valueOf(playerColor);
        runSession(connectRequest, playerColor);
    }

    public void observeGame(UserGameCommand connectRequest) throws Exception {
        this.userType = UserType.OBSERVER;
        this.playerTeam = null;
        runSession(connectRequest, "WHITE");
    }

    private void runSession(UserGameCommand connectRequest, String perspective) throws Exception {
        this.userAuthToken = connectRequest.getAuthToken();
        this.currentGameID = connectRequest.getGameID();
        interaction.clear();
        try {
            Terminal.start(console, perspective);
            ws.connectClient();
            ws.sendCommand(connectRequest);
            waitForTerminal();
            runGamePlayUI();
        } catch (org.jline.reader.EndOfFileException e) {
            leaveGame();
        } finally {
            ws.closeClient();
            Terminal.stop();
            interaction.clear();
        }
    }

    private String userTypePromptString() {
        if (userType == UserType.PLAYER) {
            return "[PLAYING]";
        }
        return "[OBSERVING]";
    }

    private void runGamePlayUI() throws Exception {
        for (;;) {
            var prompt = userTypePromptString();
            var userInput = Terminal.getGameInput(prompt + " >>> ", ws::isSessionOpen);
            try {
                if (userInput.cancel()) {
                    interaction.clear();
                    Terminal.drawHighlights(null);
                } else if (userInput.square() != null) {
                    handleBoardClick(userInput.square());
                } else if (userInput.text() == null) {
                    leaveGame();
                    break;
                } else if (!matchGamePlayCommand(userInput.text())) {
                    break;
                }
            } catch (org.jline.reader.EndOfFileException e) {
                throw e;
            } catch (Exception e) {
                interaction.clear();
                Terminal.drawHighlights(null);
                if (!ws.isSessionOpen()) {
                    throw e;
                }
                Terminal.addNotification("Command failed: " + e.getMessage());
            }
        }
    }

    private void handleBoardClick(ChessPosition square) throws Exception {
        BoardInteraction.Result result = interaction.click(Terminal.getChessGame(), square, playerTeam);
        Terminal.drawHighlights(result.selected());
        if (result.message() != null) {
            Terminal.addLogMessage(result.message());
        }
        if (result.move() != null) {
            gamePlayMakeMove(CliInputParser.formatMove(result.move()));
        }
    }

    private boolean matchGamePlayCommand(String command) throws Exception {
        String normalizedCommand = CliInputParser.normalizeCommand(command);
        if (normalizedCommand.isBlank()) {
            return true;
        }
        switch (normalizedCommand) {
            case "h", "help" -> displayGamePlayHelp();
            case "redraw", "redraw board", "redraw chess board" -> redrawBoard();
            case "status" -> displayGameStatus();
            case "flip" -> {
                interaction.clear();
                Terminal.flipBoard();
            }
            case "l", "leave" -> {
                leaveGame();
                return false;
            }
            case "m", "move", "make move" -> gamePlayMakeMove(null);
            case "resign" -> resignGame();
            case "highlight", "highlight moves", "highlight legal moves" -> highlightMoves();
            default -> {
                if (normalizedCommand.startsWith("move ") || normalizedCommand.startsWith("make move ")) {
                    int separator = normalizedCommand.indexOf(' ');
                    String moveInput = normalizedCommand.substring(separator + 1);
                    if (normalizedCommand.startsWith("make move ")) {
                        moveInput = normalizedCommand.substring("make move ".length());
                    }
                    gamePlayMakeMove(moveInput);
                } else if (normalizedCommand.startsWith("highlight ")) {
                    String position = normalizedCommand.length() > "highlight".length()
                            ? normalizedCommand.substring("highlight".length()).trim() : null;
                    highlightMoves(position);
                } else if (normalizedCommand.equals("moves") || normalizedCommand.startsWith("moves ")) {
                    String position = normalizedCommand.length() > "moves".length()
                            ? normalizedCommand.substring("moves".length()).trim() : null;
                    listLegalMoves(position);
                } else if (normalizedCommand.equals("legal moves") || normalizedCommand.startsWith("legal moves ")) {
                    String position = normalizedCommand.length() > "legal moves".length()
                            ? normalizedCommand.substring("legal moves".length()).trim() : null;
                    listLegalMoves(position);
                } else {
                    matchArbitraryCommand(normalizedCommand);
                }
            }
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
                    Status                  Shows turn, role, and check state.
                    Redraw                  Redraws the chess board.
                    Flip                    Flip the board to the opposite perspective.
                    Moves <square>          Lists legal moves from a square (Ex. moves e2).
                    Move <move>             Make a move (Ex. move e2e4 or move e7e8=Q).
                    Highlight <square>      Highlights legal moves on the board.
                    Leave                   Leave the game.
                    Resign                  Forfeit the game.

                Mouse: click your piece, then its highlighted destination. Right-click/Esc cancels.
                Observers can click pieces to inspect moves. Promotion prompts for Q/R/N/B.
                Labels: w=White b=Black; K king Q queen R rook B bishop N knight P pawn.
                Commands ignore case. Press Enter at a follow-up prompt to cancel.""";
        Terminal.showHelp(helpMessage);
    }

    private void redrawBoard() {
        Terminal.refresh();
    }

    private void waitForTerminal() throws Exception {
        long deadline = System.nanoTime() + java.util.concurrent.TimeUnit.SECONDS.toNanos(5);
        while (Terminal.notReadyForInput()) {
            if (!ws.isSessionOpen() || System.nanoTime() >= deadline) {
                throw new java.io.IOException("No game data received. Check the connection and try joining again.");
            }
            Thread.sleep(10);
        }
    }

    private void leaveGame() throws Exception {
        var leaveCommand = new UserGameCommand(CommandType.LEAVE, userAuthToken, currentGameID);
        ws.sendCommand(leaveCommand);
    }

    private void gamePlayMakeMove(String inlineMove) throws Exception {
        if (userType == UserType.OBSERVER) {
            Terminal.addLogMessage("Cannot Make Move as OBSERVER");
            return;
        }
        if (inlineMove == null) {
            Terminal.addLogMessage("Enter Move (Ex: a2a4 or e7e8=Q)");
        }
        String userInput;
        ChessMove move = null;
        while (move == null) {
            userInput = inlineMove == null ? Terminal.getInput("Enter Move: ") : inlineMove;
            if (userInput.trim().isEmpty()) {
                Terminal.addLogMessage("Aborting Move");
                return;
            }
            move = validateMoveString(userInput);
            if (inlineMove != null && move == null) {
                return;
            }
        }
        ChessGame gameCopy = Terminal.getChessGame();
        if (gameCopy == null) {
            Terminal.addLogMessage("Invalid Game State. Try Again.");
            return;
        }
        if (gameCopy.isGameOver() || gameCopy.getTeamTurn() != playerTeam) {
            interaction.clear();
            Terminal.addLogMessage(gameCopy.isGameOver() ? "Game over." : "It is " + gameCopy.getTeamTurn() + "'s turn.");
            return;
        }
        if (move.getPromotionPiece() == null && moveIsPromotion(move, gameCopy)) {
            move = getPromotionMoveFromUser(move);
            if (move == null) {
                interaction.clear();
                return;
            }
        }
        try {
            gameCopy.makeMove(move);
        } catch (InvalidMoveException e) {
            interaction.clear();
            Terminal.addLogMessage("Illegal move: " + e.getMessage());
            return;
        }
        var moveCommand = new MakeMoveCommand(CommandType.MAKE_MOVE, userAuthToken, currentGameID, move);
        ws.sendCommand(moveCommand);
    }

    private ChessMove validateMoveString(String move) {
        ChessMove resultMove = CliInputParser.parseMove(move);
        if (resultMove == null) {
            Terminal.addLogMessage("Invalid move. Use coordinates such as 'a2a4', 'e2 e4', or 'e7e8=Q'.");
        }
        return resultMove;
    }

    private boolean moveIsPromotion(ChessMove move, ChessGame game) {
        try {
            ChessMove promotionAttemptMove = new ChessMove(move.getStartPosition(), move.getEndPosition(), ChessPiece.PieceType.QUEEN);
            game.copy().makeMove(promotionAttemptMove);
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
            userInput = CliInputParser.normalizeCommand(Terminal.getInput("Promote [Q/R/N/B, Enter cancels]: "));
            if (userInput.isBlank()) {
                Terminal.addLogMessage("Promotion cancelled.");
                return null;
            }
            promotionType = switch (userInput) {
                case "1", "q", "queen" -> ChessPiece.PieceType.QUEEN;
                case "2", "r", "rook" -> ChessPiece.PieceType.ROOK;
                case "3", "n", "knight" -> ChessPiece.PieceType.KNIGHT;
                case "4", "b", "bishop" -> ChessPiece.PieceType.BISHOP;
                default -> null;
            };
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
        highlightMoves(null);
    }

    private void highlightMoves(String inlinePosition) {
        ChessPosition startPosition = null;
        while (startPosition == null) {
            String positionString = inlinePosition == null ? Terminal.getInput("Enter Start Position (Ex: a1): ") : inlinePosition;
            if (positionString.isBlank()) {
                Terminal.addLogMessage("Aborting Highlighting");
                return;
            }
            startPosition = validatePositionString(positionString);
            if (inlinePosition != null && startPosition == null) {
                return;
            }
        }
        ChessGame game = Terminal.getChessGame();
        if (game == null || game.getBoard().getPiece(startPosition) == null) {
            Terminal.addLogMessage("No piece at " + startPosition + ".");
            return;
        }
        Terminal.drawHighlights(startPosition);
    }

    private ChessPosition validatePositionString(String positionString) {
        ChessPosition position = CliInputParser.parsePosition(positionString);
        if (position == null) {
            Terminal.addLogMessage("Invalid Position");
        }
        return position;
    }

    private void listLegalMoves(String positionInput) {
        if (positionInput == null || positionInput.isBlank()) {
            Terminal.addLogMessage("Usage: moves <square> (Ex. moves e2)");
            return;
        }
        ChessPosition position = validatePositionString(positionInput);
        if (position == null) {
            return;
        }
        ChessGame game = Terminal.getChessGame();
        if (game == null) {
            Terminal.addLogMessage("Game state is not ready. Try again.");
            return;
        }
        ChessPiece piece = game.getBoard().getPiece(position);
        if (piece == null) {
            Terminal.addLogMessage("No piece at " + position + ".");
            return;
        }
        if (piece.getTeamColor() != game.getTeamTurn()) {
            Terminal.addLogMessage("It is " + game.getTeamTurn() + "'s turn.");
            return;
        }
        var moves = game.validMoves(position);
        if (moves == null || moves.isEmpty()) {
            Terminal.addLogMessage("No legal moves from " + position + ".");
            return;
        }
        String formattedMoves = moves.stream()
                .sorted(Comparator.comparing(CliInputParser::formatMove))
                .map(CliInputParser::formatMove)
                .collect(Collectors.joining(", "));
        Terminal.addLogMessage("Legal moves from " + position + ": " + formattedMoves);
    }

    private void displayGameStatus() {
        ChessGame game = Terminal.getChessGame();
        if (game == null) {
            Terminal.addLogMessage("Game state is not ready. Try again.");
            return;
        }
        Terminal.addLogMessage("Role: " + userType + (playerTeam == null ? "" : " as " + playerTeam)
                + " | View: " + Terminal.getPlayerColor() + " | " + Terminal.statusLine(game));
    }
}
