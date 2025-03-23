package UI;

import java.util.Scanner;

public class CommandLine {
    private LoginState loginState;
    private final Scanner scanner = new Scanner(System.in);

    public CommandLine() {
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

    public String getUserInput(String prompt) {
        if (prompt != null) {
            System.out.println(prompt);
        }
        System.out.print(loginStatePromptString() + " >>> ");
        return scanner.nextLine().trim();
    }


    public static void displayHelpBeforeLogin() {
        String helpMessage = """
                Available commands for PreLogin UI:
                    Help        Displays this help message.
                    Quit        Exits the program.
                    Login       Prompts for user credentials and attempts to login.
                    Register    Prompts for new user credentials and logs in.
                """;
        System.out.println(helpMessage);
    }

    public void matchPreLoginCommand(String command) {
        if (command != null) {
            switch (command) {
                case "Help" -> displayHelpBeforeLogin();
                case "Quit" -> System.exit(0);
//                case "Login" -> ProcessLoginRequest();
//                case "Register" -> ProcessRegisterRequest();
                default -> matchArbitraryCommand(command);
            }
        }
    }

    public void matchArbitraryCommand(String command) {
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
}
