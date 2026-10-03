package ui;

import model.GameEntry;
import org.junit.jupiter.api.Test;
import requests.JoinGameRequest;
import requests.JoinGameResult;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CommandLineSelectionTests {
    @Test
    @SuppressWarnings("unchecked")
    void joiningUsesTheSelectedGameIdRatherThanItsListNumber() throws Exception {
        var originalInput = System.in;
        AtomicReference<JoinGameRequest> request = new AtomicReference<>();
        try {
            System.setIn(new ByteArrayInputStream("2\n1\n".getBytes(StandardCharsets.UTF_8)));
            ServerFacade facade = new ServerFacade(8080) {
                @Override
                public JoinGameResult joinGameClient(JoinGameRequest join) {
                    request.set(join);
                    return null; // Stop before opening a live gameplay connection.
                }
            };
            CommandLine cli = new CommandLine(facade);
            var gamesField = CommandLine.class.getDeclaredField("gamesList");
            gamesField.setAccessible(true);
            ((List<GameEntry>) gamesField.get(cli)).addAll(List.of(
                    new GameEntry(71, null, null, "First"), new GameEntry(902, null, null, "Second")));
            var play = CommandLine.class.getDeclaredMethod("processPlayGameRequest");
            play.setAccessible(true);

            play.invoke(cli);

            assertEquals("902", request.get().gameID());
            assertEquals("WHITE", request.get().playerColor());
        } finally {
            System.setIn(originalInput);
        }
    }
}
