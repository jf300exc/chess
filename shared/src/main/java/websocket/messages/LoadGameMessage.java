package websocket.messages;

import model.GameData;

import java.util.Objects;

public class LoadGameMessage extends ServerMessage {
    private final GameData gameData;

    public LoadGameMessage(ServerMessageType type, GameData game) {
        super(type);
        this.gameData = game;
    }

    public GameData getGameData() {
        return gameData;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }
        LoadGameMessage that = (LoadGameMessage) o;
        return Objects.equals(gameData, that.gameData);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), gameData);
    }
}
