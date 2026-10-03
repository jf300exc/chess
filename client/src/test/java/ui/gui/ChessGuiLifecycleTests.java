package ui.gui;

import chess.ChessGame;
import model.GameData;
import org.junit.jupiter.api.*;

import javax.swing.*;
import java.awt.GraphicsEnvironment;

import static org.junit.jupiter.api.Assertions.*;

class ChessGuiLifecycleTests {
    private ChessGui gui;

    @BeforeEach
    void setup() throws Exception {
        Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "Requires a desktop or xvfb-run");
        SwingUtilities.invokeAndWait(() -> gui = new ChessGui("localhost", 8080));
    }

    @AfterEach
    void cleanup() throws Exception {
        if (gui != null) {
            SwingUtilities.invokeAndWait(() -> {
                // Realize the frame so disposal fires windowClosed and releases its executor.
                gui.pack();
                gui.dispose();
            });
            SwingUtilities.invokeAndWait(() -> { });
        }
    }

    @Test
    void resignationUpdatesTheDisplayedGameAndDisablesMoves() throws Exception {
        ChessGame position = new ChessGame();
        SwingUtilities.invokeAndWait(() -> {
            try {
                var player = ChessGui.class.getDeclaredField("playerColor");
                player.setAccessible(true);
                player.set(gui, ChessGame.TeamColor.WHITE);
                var load = ChessGui.class.getDeclaredMethod("loadGame", GameData.class);
                load.setAccessible(true);
                load.invoke(gui, new GameData(71, "white", "black", "Match", position));
                assertTrue((boolean) boardField("interactive"));
            } catch (ReflectiveOperationException e) {
                throw new RuntimeException(e);
            }
        });

        gui.onMessage("{\"serverMessageType\":\"NOTIFICATION\",\"message\":\"black has resigned\"}");
        SwingUtilities.invokeAndWait(() -> {
            assertTrue(position.isGameOver());
            assertEquals("Game over", ((JLabel) field("turnLabel")).getText());
            assertFalse((boolean) boardField("interactive"));
        });
    }

    @Test
    void messagesFromAPreviousConnectionAreIgnored() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            try {
                var version = ChessGui.class.getDeclaredField("navigationVersion");
                version.setAccessible(true);
                version.setLong(gui, 1);
                var receive = ChessGui.class.getDeclaredMethod("handleMessage", String.class, long.class);
                receive.setAccessible(true);
                receive.invoke(gui, "{\"serverMessageType\":\"NOTIFICATION\",\"message\":\"old game update\"}", 0L);
            } catch (ReflectiveOperationException e) {
                throw new RuntimeException(e);
            }
        });
        SwingUtilities.invokeAndWait(() -> assertTrue(((DefaultListModel<?>) field("activityModel")).isEmpty()));
    }

    private Object field(String name) {
        try {
            var field = ChessGui.class.getDeclaredField(name);
            field.setAccessible(true);
            return field.get(gui);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    private Object boardField(String name) {
        try {
            var field = ChessBoardPanel.class.getDeclaredField(name);
            field.setAccessible(true);
            return field.get(field("boardPanel"));
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }
}
