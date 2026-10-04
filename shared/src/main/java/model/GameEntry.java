package model;

public record GameEntry(int gameID, String whiteUsername, String blackUsername, String gameName, StockfishPlayer stockfish) {
    public GameEntry(int gameID, String whiteUsername, String blackUsername, String gameName) {
        this(gameID, whiteUsername, blackUsername, gameName, null);
    }

    public GameEntry(GameData gameData) {
        this(gameData.gameID(), gameData.whiteUsername(), gameData.blackUsername(), gameData.gameName(), gameData.stockfish());
    }
}
