package ui;

import model.GameEntry;
import websocket.commands.UserGameCommand;
import websocket.commands.UserGameCommand.CommandType;
import requests.*;

import java.util.*;

public class CommandLine {
    private final Scanner scanner = new Scanner(System.in);
    private final List<GameEntry> gamesList = new ArrayList<>();
    private final ServerFacade serverFacade;

    public final GamePlay gamePlay = new GamePlay();

    private LoginState loginState;

    public CommandLine(ServerFacade serverFacade) {
        this.serverFacade = serverFacade;
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
        System.out.print(Objects.requireNonNullElseGet(prompt, () -> loginStatePromptString() + " >>> "));
        return scanner.nextLine().trim();
    }

    private static void displayHelpBeforeLogin() {
        String helpMessage = """
                Available commands for PreLogin UI:
                    Help        Displays this help message.
                    Quit        Exits the program.
                    Login       Prompts for user credentials and attempts to login.
                    Register    Prompts for new user credentials and logs in.""";
        System.out.println(helpMessage);
    }

    private boolean matchPreLoginCommand(String command) {
        if (command.isBlank()) {
            return true;
        }
        boolean noExit = true;
        switch (command) {
            case "Help" -> displayHelpBeforeLogin();
            case "Quit" -> noExit = false;
            case "Register" -> processRegisterRequest();
            case "Login" -> processLoginRequest();
            default -> matchArbitraryCommand(command);
        }
        return noExit;
    }

    private static void displayHelpAfterLogin() {
        String helpMessage = """
                Available commands for PostLogin UI:
                    Help            Displays this help message.
                    Logout          Logs out and returns to preLogin UI.
                    Create Game     Prompts for new game information and attempts to create a new game.
                    List Games      Lists all the games that exist on the server.
                    Play Game       Prompts the user for player information and attempts to join a game.
                    Observe Game    Prompts the user for a game ID to view a game. Used after List Games.""";
        System.out.println(helpMessage);
    }

    private void matchPostLoginCommand(String command) throws Exception {
        if (command.isBlank()) {
            return;
        }
        switch (command) {
            case "Help" -> displayHelpAfterLogin();
            case "Logout" -> processLogoutRequest();
            case "Create Game" -> processCreateGameRequest();
            case "List Games" -> processListGamesRequest();
            case "Play Game" -> processPlayGameRequest();
            case "Observe Game" -> processObserveGameRequest();
            case "Quit" -> System.out.println("Unavailable. Logout first.");
            default -> matchArbitraryCommand(command);
        }
    }

    private void matchArbitraryCommand(String command) {
        if (command.equals("clear")) {
            System.out.print(EscapeSequences.ERASE_SCREEN);
        } else {
            System.out.println("Unknown command");
        }
    }

    public void run() throws Exception {
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

    private void processCreateGameRequest() {
        String gameName = getUserInput("Game Name: ");
        String authToken = serverFacade.getAuthToken();
        CreateGameRequest createGameRequest = new CreateGameRequest(authToken, gameName);
        CreateGameResult result = serverFacade.createGameClient(createGameRequest);
        if (result == null) {
            System.out.println("Failed to create game. Try again.");
        } else {
            System.out.println("Successfully created game.");
        }
    }

    private void processListGamesRequest() {
        ListGamesRequest listGamesRequest = new ListGamesRequest(serverFacade.getAuthToken());
        ListGamesResult result = serverFacade.listGamesClient(listGamesRequest);
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
        System.out.println(gameNumber + ": " + gameEntry.gameName());
        if (gameEntry.whiteUsername() != null) {
            System.out.println("  WHITE username: " + gameEntry.whiteUsername());
        }
        if (gameEntry.blackUsername() != null) {
            System.out.println("  BLACK username: " + gameEntry.blackUsername());
        }
    }

    private void processPlayGameRequest() throws Exception {
        String gameIDStr = getGameIDFromUserInput();
        if (gameIDStr == null) {
            return;
        }

        String playerColor = null;
        int gameIndex = Integer.parseInt(gameIDStr) - 1;
        GameEntry gameEntry = gamesList.get(gameIndex);
        if (gameEntry.whiteUsername() != null && gameEntry.blackUsername() == null) {
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
        JoinGameRequest joinGameRequest = new JoinGameRequest(authToken, playerColor, gameIDStr);
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
        String gameIDStr = getGameIDFromUserInput();
        if (gameIDStr == null) {
            return;
        }
        String authToken = serverFacade.getAuthToken();
        int gameIndex = Integer.parseInt(gameIDStr) - 1;
        GameEntry gameEntry = gamesList.get(gameIndex);
        var connectRequest = new UserGameCommand(CommandType.CONNECT, authToken, gameEntry.gameID());
        gamePlay.observeGame(connectRequest);
    }

    private String getGameIDFromUserInput() {
        if (gamesList.isEmpty()) {
            System.out.println("No games loaded. Run 'List Games' to choose a game.");
            return null;
        }

        String gameNumStr = getUserInput("Game Number: ");
        int gameNum;
        try {
            gameNum = Integer.parseInt(gameNumStr);
        } catch (NumberFormatException e) {
            gameNum = -1;
        }
        if (gameNum <= 0 || gameNum > gamesList.size()) {
            System.out.println("Invalid game number. Try again.");
            return null;
        }
        int gameID = gamesList.get(gameNum - 1).gameID();
        return Integer.toString(gameID);
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
