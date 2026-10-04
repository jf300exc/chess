import server.Server;
import server.ConnectionInfo;

/** Multiplayer server entry point. */
public class ServerMain {
    public static void main(String[] args) {
        int port = readPort(args);
        System.out.println("♕ 240 Chess Server starting on port " + port);
        int listeningPort = new Server().run(port);
        ConnectionInfo.print(listeningPort);
    }

    private static int readPort(String[] args) {
        String configured = args.length > 0 ? args[0] : System.getenv().getOrDefault("CHESS_PORT", "8080");
        try {
            int port = Integer.parseInt(configured);
            if (port < 1 || port > 65535) {
                throw new NumberFormatException();
            }
            return port;
        } catch (NumberFormatException e) {
            System.err.println("Invalid port '" + configured + "'. Expected a number from 1 to 65535.");
            System.exit(2);
            return 8080;
        }
    }
}
