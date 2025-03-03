package model;

public record GameEntry(int gameID, String whiteUsername, String blackUsername, String gameName) {
    public GameEntry(GameData gameData) {
        this(gameData.gameID(), gameData.whiteUsername(), gameData.blackUsername(), gameData.gameName());
    }
}
