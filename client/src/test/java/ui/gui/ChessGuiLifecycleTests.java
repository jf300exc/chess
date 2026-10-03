package ui.gui;

import chess.ChessGame;
import model.GameData;
import org.junit.jupiter.api.*;

import javax.swing.*;
import java.awt.CardLayout;
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

    @Test
    void activityFollowsNewMessagesWhenAlreadyAtTheBottom() throws Exception {
        showActivity(30);
        SwingUtilities.invokeAndWait(() -> {
            JScrollBar scrollbar = activityScroll().getVerticalScrollBar();
            assertTrue(scrollbar.getMaximum() > scrollbar.getVisibleAmount());
            scrollbar.setValue(scrollbar.getMaximum());
        });

        receiveActivity("Newest message");

        SwingUtilities.invokeAndWait(() -> {
            JScrollBar scrollbar = activityScroll().getVerticalScrollBar();
            assertEquals(scrollbar.getMaximum(), scrollbar.getValue() + scrollbar.getVisibleAmount());
            JList<?> list = (JList<?>) field("activityList");
            assertEquals(list.getModel().getSize() - 1, list.getLastVisibleIndex());
        });
    }

    @Test
    void activityPreservesTheScrollPositionWhenReadingOlderMessages() throws Exception {
        showActivity(30);
        final int[] previous = new int[1];
        SwingUtilities.invokeAndWait(() -> {
            activityScroll().getVerticalScrollBar().setValue(100);
            previous[0] = activityScroll().getViewport().getViewPosition().y;
        });

        receiveActivity("Newest message");

        SwingUtilities.invokeAndWait(() ->
                assertEquals(previous[0], activityScroll().getViewport().getViewPosition().y));
    }

    @Test
    void activityKeepsTheSameOlderMessageVisibleWhenHistoryIsTrimmed() throws Exception {
        showActivity(100);
        final String[] previous = new String[1];
        SwingUtilities.invokeAndWait(() -> {
            JList<?> list = (JList<?>) field("activityList");
            activityScroll().getVerticalScrollBar().setValue(list.getCellBounds(20, 20).y + 3);
            previous[0] = (String) list.getModel().getElementAt(list.getFirstVisibleIndex());
        });

        receiveActivity("Newest message");

        SwingUtilities.invokeAndWait(() -> {
            JList<?> list = (JList<?>) field("activityList");
            assertEquals(100, list.getModel().getSize());
            assertEquals(previous[0], list.getModel().getElementAt(list.getFirstVisibleIndex()));
        });
    }

    private JScrollPane activityScroll() {
        return (JScrollPane) field("activityScroll");
    }

    private void showActivity(int messages) throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            ((CardLayout) field("cardLayout")).show((JPanel) field("cards"), "game");
            gui.setVisible(true);
        });
        for (int i = 0; i < messages; i++) {
            receiveActivity("Message " + i);
        }
    }

    private void receiveActivity(String message) throws Exception {
        gui.onMessage("{\"serverMessageType\":\"NOTIFICATION\",\"message\":\"" + message + "\"}");
        SwingUtilities.invokeAndWait(() -> { });
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
