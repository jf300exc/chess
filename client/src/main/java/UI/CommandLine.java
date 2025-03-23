package UI;

import requests.RegisterRequest;

import java.util.Scanner;

public class CommandLine {
    private final ServerFacade serverFacade;
    private final Scanner scanner = new Scanner(System.in);

    private LoginState loginState;

    public CommandLine(ServerFacade serverFacade) {
        this.serverFacade = serverFacade;
        loginState = LoginState.LOGGED_OUT;
    }

    public enum LoginState{
        LOGGED_OUT,
        LOGGED_IN,
    }

    public void setLoginState(LoginState loginState) {
        this.loginState = loginState;
    }

    public LoginState getLoginState() {
        return loginState;
    }

    private String loginStatePromptString() {
        return '[' + loginState.toString() + ']';
    }

    private String getUserInput(String prompt) {
        if (prompt != null) {
            System.out.println(prompt);
        }
        System.out.print(loginStatePromptString() + " >>> ");
        return scanner.nextLine().trim();
    }

    private static void displayHelpBeforeLogin() {
        String helpMessage = """
                Available commands for PreLogin UI:
                    Help        Displays this help message.
                    Quit        Exits the program.
                    Login       Prompts for user credentials and attempts to login.
                    Register    Prompts for new user credentials and logs in.
                """;
        System.out.println(helpMessage);
    }

    private void matchPreLoginCommand(String command) {
        if (command != null) {
            switch (command) {
                case "Help" -> displayHelpBeforeLogin();
                case "Quit" -> System.exit(0);
                case "Register" -> processRegisterRequest();
//                case "Login" -> processLoginRequest();
                default -> matchArbitraryCommand(command);
            }
        }
    }

    private void matchArbitraryCommand(String command) {
        if (command != null) {
            switch (command) {
                case "clear" -> System.out.print("\033[H\033[2J");
            }
        }
    }

    public void run() {
        String userInput = null;
        while (userInput == null || !userInput.isEmpty()) {
            userInput = getUserInput(null);
            if (loginState == LoginState.LOGGED_OUT) {
                matchPreLoginCommand(userInput);
            }
        }
    }

    public void processRegisterRequest() {
        String username = getUserInput("Username: ");
        String password = getUserInput("Password: ");
        String email = getUserInput("Email: ");
        RegisterRequest registerRequest = new RegisterRequest(username, password, email);
        serverFacade.registerClient(registerRequest);
    }
}
