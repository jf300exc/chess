package ui;

import model.GameEntry;
import model.StockfishOptions;
import chess.ChessGame;
import websocket.commands.UserGameCommand;
import websocket.commands.UserGameCommand.CommandType;
import requests.*;

import java.util.*;

public class CommandLine {
    private final CliConsole console;
    private final List<GameEntry> gamesList = new ArrayList<>();
    private final ServerFacade serverFacade;

    public final GamePlay gamePlay;

    private LoginState loginState;
    private String username;

    public CommandLine(ServerFacade serverFacade) {
        this(serverFacade, CliConsole.open(true, true));
    }

    public CommandLine(ServerFacade serverFacade, CliConsole console) {
        this.serverFacade = serverFacade;
        this.console = console;
        this.gamePlay = new GamePlay(console);
        loginState = LoginState.LOGGED_OUT;
    }

    public enum LoginState{
        LOGGED_OUT,
        LOGGED_IN,
    }

    private String loginStatePromptString() {
        return '[' + loginState.toString() + ']';
    }

    private String getUserInput(String prompt) {
        String input = console.readLine(Objects.requireNonNullElseGet(prompt, () -> loginStatePromptString() + " >>> "));
        if (input == null) {
            throw new org.jline.reader.EndOfFileException();
        }
        return input.trim();
    }

    private static void displayHelpBeforeLogin() {
        String helpMessage = """
                Available commands for PreLogin UI:
                    Help                 Displays this help message.
                    Quit                 Exits the program.
                    Login                Prompts for user credentials and attempts to login.
                    Register             Prompts for new user credentials and logs in.

                Commands are case-insensitive. Shortcuts: h, q, l, r.""";
        System.out.println(helpMessage);
    }

    private boolean matchPreLoginCommand(String command) {
        String normalizedCommand = CliInputParser.normalizeCommand(command);
        if (normalizedCommand.isBlank()) {
            return true;
        }
        boolean noExit = true;
        switch (normalizedCommand) {
            case "h", "help" -> displayHelpBeforeLogin();
            case "q", "quit" -> noExit = false;
            case "r", "reg", "register" -> processRegisterRequest();
            case "l", "login" -> processLoginRequest();
            default -> matchArbitraryCommand(normalizedCommand);
        }
        return noExit;
    }

    private static void displayHelpAfterLogin() {
        String helpMessage = """
                Available commands for PostLogin UI:
                    Help                 Displays this help message.
                    Logout               Logs out and returns to preLogin UI.
                    Create Game          Prompts for new game information and attempts to create a new game.
                    Stockfish            Add an AI opponent to an empty seat before the first move.
                    List Games           Lists all the games that exist on the server.
                    Play Game            Prompts for player information and joins a game.
                    Observe Game         Prompts for a game to view. Used after List Games.
                    Clear                Clears the terminal screen.

                Commands are case-insensitive. Shortcuts: h, c, l, p, o, q.""";
        System.out.println(helpMessage);
    }

    private void matchPostLoginCommand(String command) throws Exception {
        String normalizedCommand = CliInputParser.normalizeCommand(command);
        if (normalizedCommand.isBlank()) {
            return;
        }
        switch (normalizedCommand) {
            case "h", "help" -> displayHelpAfterLogin();
            case "logout" -> processLogoutRequest();
            case "c", "create", "create game" -> processCreateGameRequest();
            case "stockfish", "ai" -> processAddStockfishRequest();
            case "l", "list", "list games" -> processListGamesRequest();
            case "p", "play", "play game" -> processPlayGameRequest();
            case "o", "observe", "observe game" -> processObserveGameRequest();
            case "clear" -> matchArbitraryCommand(normalizedCommand);
            case "q", "quit" -> System.out.println("Unavailable. Logout first.");
            default -> matchArbitraryCommand(normalizedCommand);
        }
    }

    private void matchArbitraryCommand(String command) {
        if (CliInputParser.normalizeCommand(command).equals("clear")) {
            System.out.print(EscapeSequences.ERASE_SCREEN);
        } else {
            System.out.println("Unknown command. Type 'help' for available commands.");
        }
    }

    public void run() throws Exception {
        try {
            for (;;) {
                String userInput = getUserInput(null);
                if (loginState == LoginState.LOGGED_OUT) {
                    if (!matchPreLoginCommand(userInput)) {
                        break;
                    }
                } else {
                    matchPostLoginCommand(userInput);
                }
            }
        } catch (org.jline.reader.EndOfFileException e) {
            // EOF/Ctrl-C from either the main prompt or a credentials prompt exits cleanly.
        }
    }

    private void processRegisterRequest() {
        String username = getUserInput("Username: ");
        String password = getUserInput("Password: ");
        String email = getUserInput("Email: ");
        RegisterRequest registerRequest = new RegisterRequest(username, password, email);
        RegisterResult result = serverFacade.registerClient(registerRequest);
        if (result == null) {
            System.out.println("Failed. Try again with different credentials.");
        } else {
            System.out.println("Successfully registered.");
            serverFacade.setAuthToken(result.authToken());
            username = result.username();
            loginState = LoginState.LOGGED_IN;
        }
    }

    private void processLoginRequest() {
        String username = getUserInput("Username: ");
        String password = getUserInput("Password: ");
        LoginRequest loginRequest = new LoginRequest(username, password);
        LoginResult result = serverFacade.loginClient(loginRequest);
        if (result == null) {
            System.out.println("Failed. Try again with different credentials.");
        } else {
            System.out.println("Successfully logged in.");
            serverFacade.setAuthToken(result.authToken());
            username = result.username();
            loginState = LoginState.LOGGED_IN;
        }
    }

    private void processLogoutRequest() {
        LogoutRequest logoutRequest = new LogoutRequest(serverFacade.getAuthToken());
        LogoutResult result = serverFacade.logoutClient(logoutRequest);
        if (result == null) {
            System.out.println("Failed to logout. Try again.");
        } else {
            System.out.println("Successfully logged out.");
            serverFacade.setAuthToken(null);
            loginState = LoginState.LOGGED_OUT;
        }
    }

    private void processCreateGameRequest() throws Exception {
        String gameName = getUserInput("Game Name: ");
        String opponent = getUserInput("Opponent [human/stockfish] (human): ");
        if (!opponent.isBlank() && !opponent.equalsIgnoreCase("human") && !opponent.equalsIgnoreCase("stockfish")) {
            System.out.println("Choose human or stockfish."); return;
        }
        StockfishOptions options = opponent.equalsIgnoreCase("stockfish") ? stockfishOptions() : null;
        if (opponent.equalsIgnoreCase("stockfish") && options == null) { return; }
        String authToken = serverFacade.getAuthToken();
        CreateGameRequest createGameRequest = new CreateGameRequest(authToken, gameName, options);
        CreateGameResult result = serverFacade.createGameClient(createGameRequest);
        if (result == null) {
            System.out.println("Failed to create game: " + serverFacade.getLastError());
        } else {
            System.out.println("Successfully created game.");
            if (options != null) {
                String human = options.color() == ChessGame.TeamColor.WHITE ? "BLACK" : "WHITE";
                gamePlay.playGame(new UserGameCommand(CommandType.CONNECT, authToken, Integer.parseInt(result.gameID())), human);
            }
        }
    }

    private StockfishOptions stockfishOptions() {
        String human = getUserInput("Your color [white/black] (white): ");
        if (human.isBlank()) { human = "white"; }
        if (!human.equalsIgnoreCase("white") && !human.equalsIgnoreCase("black")) {
            System.out.println("Choose white or black."); return null;
        }
        var ai = human.equalsIgnoreCase("white") ? ChessGame.TeamColor.BLACK : ChessGame.TeamColor.WHITE;
        String mode = getUserInput("Difficulty [elo/skill] (elo matches your rating): ");
        if (mode.isBlank() || mode.equalsIgnoreCase("elo")) {
            return new StockfishOptions(ai, StockfishOptions.Mode.ELO, null, null);
        }
        if (mode.equalsIgnoreCase("skill")) {
            try {
                int level = Integer.parseInt(getUserInput("Stockfish level [0-20]: "));
                var options = new StockfishOptions(ai, StockfishOptions.Mode.SKILL, null, level);
                options.validate(); return options;
            } catch (IllegalArgumentException e) { System.out.println("Enter a whole number from 0 to 20."); return null; }
        }
        System.out.println("Choose elo or skill."); return null;
    }

    private void processAddStockfishRequest() throws Exception {
        GameEntry game = getGameFromUserInput();
        if (game == null) { return; }
        StockfishOptions options = stockfishOptions();
        if (options == null) { return; }
        String human = options.color() == ChessGame.TeamColor.WHITE ? "BLACK" : "WHITE";
        String seat = human.equals("WHITE") ? game.whiteUsername() : game.blackUsername();
        if (seat == null) {
            if (serverFacade.joinGameClient(new JoinGameRequest(serverFacade.getAuthToken(), human, "" + game.gameID())) == null) {
                System.out.println(serverFacade.getLastError()); return;
            }
        } else if (!seat.equals(username)) { System.out.println("Choose your own seat or an empty seat."); return; }
        var result = serverFacade.addStockfishClient(new AddStockfishRequest(serverFacade.getAuthToken(), "" + game.gameID(), options));
        if (result == null) { System.out.println(serverFacade.getLastError()); return; }
        gamePlay.playGame(new UserGameCommand(CommandType.CONNECT, serverFacade.getAuthToken(), game.gameID()), human);
    }

    private void processListGamesRequest() {
        ListGamesRequest listGamesRequest = new ListGamesRequest(serverFacade.getAuthToken());
        ListGamesResult result = serverFacade.listGamesClient(listGamesRequest);
        var profile = serverFacade.profileClient();
        if (profile != null) { System.out.println(profile.username() + " · " + profile.elo() + " Elo"); }
        // Clear the saved game list
        gamesList.clear();
        if (result == null) {
            System.out.println("Failed to list games. Try again.");
        } else {
            // List the games that currently exist on the server
            int i = 1;
            if (result.games().isEmpty()) {
                System.out.println("No games found. Create a game with `Create Game`");
            } else {
                for (GameEntry gameEntry : result.games()) {
                    printGameInfo(gameEntry, i++);
                    gamesList.add(gameEntry);
                }
            }
        }
    }

    private void printGameInfo(GameEntry gameEntry, int gameNumber) {
        System.out.println(gameNumber + ": " + gameEntry.gameName() + " (id " + gameEntry.gameID() + ")");
        if (gameEntry.stockfish() != null) { System.out.println("  " + gameEntry.stockfish().description()); }
        if (gameEntry.whiteUsername() != null) {
            System.out.println("  WHITE username: " + gameEntry.whiteUsername());
        }
        if (gameEntry.blackUsername() != null) {
            System.out.println("  BLACK username: " + gameEntry.blackUsername());
        }
    }

    private void processPlayGameRequest() throws Exception {
        GameEntry gameEntry = getGameFromUserInput();
        if (gameEntry == null) {
            return;
        }

        String playerColor = null;
        if (username != null && (username.equals(gameEntry.whiteUsername()) || username.equals(gameEntry.blackUsername()))) {
            playerColor = username.equals(gameEntry.whiteUsername()) ? "WHITE" : "BLACK";
            gamePlay.playGame(new UserGameCommand(CommandType.CONNECT, serverFacade.getAuthToken(), gameEntry.gameID()), playerColor);
            return;
        } else if (gameEntry.whiteUsername() != null && gameEntry.blackUsername() == null) {
            System.out.println("White username is already in use.");
            String confirmation = getUserInput("Join as BLACK? y/n: ");
            if (confirmation.equalsIgnoreCase("y")) {
                playerColor = "BLACK";
            } else {
                System.out.println("Aborting");
            }
        } else if (gameEntry.whiteUsername() == null && gameEntry.blackUsername() != null) {
            System.out.println("Black username is already in use.");
            String confirmation = getUserInput("Join as WHITE? y/n: ");
            if (confirmation.equalsIgnoreCase("y")) {
                playerColor = "WHITE";
            } else {
                System.out.println("Aborting");
            }
        } else if (gameEntry.whiteUsername() != null) {
            System.out.println("Game full.");
        } else {
            playerColor = getPlayerColorFromUserInput();
        }

        // Abort if no playerColor is chosen
        if (playerColor == null) {
            return;
        }

        String authToken = serverFacade.getAuthToken();
        JoinGameRequest joinGameRequest = new JoinGameRequest(authToken, playerColor, Integer.toString(gameEntry.gameID()));
        JoinGameResult result = serverFacade.joinGameClient(joinGameRequest);

        if (result == null) {
            System.out.println("Failed to join game. Try again.");
            return;
        }

        System.out.println("Successfully joined game as " + playerColor);
        var connectRequest = new UserGameCommand(CommandType.CONNECT, authToken, gameEntry.gameID());
        gamePlay.playGame(connectRequest, playerColor);
    }

    private void processObserveGameRequest() throws Exception {
        GameEntry gameEntry = getGameFromUserInput();
        if (gameEntry == null) {
            return;
        }
        String authToken = serverFacade.getAuthToken();
        var connectRequest = new UserGameCommand(CommandType.CONNECT, authToken, gameEntry.gameID());
        gamePlay.observeGame(connectRequest);
    }

    private GameEntry getGameFromUserInput() {
        if (gamesList.isEmpty()) {
            System.out.println("No games loaded. Run 'List Games' to choose a game.");
            return null;
        }

        String gameNumStr = getUserInput("Game Number (or id <ID>): ");
        boolean explicitId = CliInputParser.normalizeCommand(gameNumStr).startsWith("id ");
        if (explicitId) {
            gameNumStr = gameNumStr.substring(gameNumStr.indexOf(' ') + 1).trim();
        }
        int gameNum;
        try {
            gameNum = Integer.parseInt(gameNumStr);
        } catch (NumberFormatException e) {
            gameNum = -1;
        }
        if (!explicitId && gameNum >= 1 && gameNum <= gamesList.size()) {
            return gamesList.get(gameNum - 1);
        }
        for (GameEntry game : gamesList) {
            if (game.gameID() == gameNum) {
                return game;
            }
        }
        System.out.println("Invalid game number or ID. Try again.");
        return null;
    }

    private String getPlayerColorFromUserInput() {
        System.out.println("""
                Choose a player color
                  1. WHITE
                  2. BLACK""");
        String playerColorNum = getUserInput("Pick color number: ");
        String playerColor;
        if (playerColorNum.trim().equals("1")) {
            playerColor = "WHITE";
        } else if (playerColorNum.trim().equals("2")) {
            playerColor = "BLACK";
        } else {
            System.out.println("Invalid color number. Try again.");
            playerColor = null;
        }
        return playerColor;
    }
}
