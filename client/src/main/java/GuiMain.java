import ui.gui.ChessGui;

import javax.swing.*;
import java.awt.*;

/** Desktop entry point. The original {@code Main} remains the CLI entry point. */
public class GuiMain {
    public static void main(String[] args) {
        if (GraphicsEnvironment.isHeadless()) {
            System.err.println("The chess GUI needs a graphical desktop (DISPLAY is not available). Use `make cli` instead.");
            System.exit(2);
        }

        String host = option(args, "--host", System.getenv().getOrDefault("CHESS_HOST", "localhost"));
        int port = port(args, System.getenv().getOrDefault("CHESS_PORT", "8080"));
        SwingUtilities.invokeLater(() -> {
            ChessGui.installLookAndFeel();
            new ChessGui(host, port).setVisible(true);
        });
    }

    private static String option(String[] args, String name, String fallback) {
        for (int i = 0; i < args.length - 1; i++) {
            if (name.equals(args[i])) {
                return args[i + 1];
            }
        }
        return fallback;
    }

    private static int port(String[] args, String fallback) {
        String value = option(args, "--port", fallback);
        try {
            int port = Integer.parseInt(value);
            if (port < 1 || port > 65535) {
                throw new NumberFormatException();
            }
            return port;
        } catch (NumberFormatException e) {
            System.err.println("Invalid port '" + value + "'. Expected a number from 1 to 65535.");
            System.exit(2);
            return 8080;
        }
    }
}
