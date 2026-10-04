import ui.CommandLine;
import ui.CliConsole;
import ui.PieceSymbols;
import ui.ServerFacade;
import ui.Terminal;
import ui.WebSocketClient;

/** Command-line client entry point. */
public class ClientMain {
    private static final int PORT = 8080;

    public static void main(String[] args) {
        // Board images use fonts and raster drawing, never a desktop window.
        System.setProperty("java.awt.headless", "true");
        String host = readOption(args, "--host", System.getenv().getOrDefault("CHESS_HOST", "localhost"));
        int port = readPort(args, System.getenv().getOrDefault("CHESS_PORT", Integer.toString(PORT)));
        ServerFacade httpFacade = new ServerFacade(host, port);

        boolean textOnly = java.util.Arrays.asList(args).contains("--text");
        boolean noMouse = java.util.Arrays.asList(args).contains("--no-mouse");
        boolean noGraphics = java.util.Arrays.asList(args).contains("--no-graphics");
        try (CliConsole console = CliConsole.open(textOnly, noMouse, noGraphics, PieceSymbols.fromArgs(args))) {
            CommandLine commandLine = new CommandLine(httpFacade, console);
            WebSocketClient webSocketClient = new WebSocketClient(host, port, commandLine.gamePlay);
            commandLine.gamePlay.setWebSocket(webSocketClient);
            System.out.printf("♕ Welcome to 240 Chess Client (%s:%d). Type Help to get started. ♕%n", host, port);
            commandLine.run();
        } catch (Exception e) {
            System.err.println("Chess client error: " + e.getMessage());
            Terminal.stop();
        }
    }

    private static String readOption(String[] args, String option, String fallback) {
        for (int i = 0; i < args.length - 1; i++) {
            if (option.equals(args[i])) {
                return args[i + 1];
            }
        }
        return fallback;
    }

    private static int readPort(String[] args, String fallback) {
        String value = readOption(args, "--port", fallback);
        try {
            int port = Integer.parseInt(value);
            if (port < 1 || port > 65535) {
                throw new NumberFormatException();
            }
            return port;
        } catch (NumberFormatException e) {
            System.err.println("Invalid port '" + value + "'. Expected a number from 1 to 65535.");
            System.exit(2);
            return PORT;
        }
    }
}
