package model;

import chess.ChessGame;

public record GameData(int gameID, String whiteUsername, String blackUsername, String gameName,
                       ChessGame game, StockfishPlayer stockfish) {
    public GameData(int gameID, String whiteUsername, String blackUsername, String gameName, ChessGame game) {
        this(gameID, whiteUsername, blackUsername, gameName, game, null);
    }

    public static GameData updateGameDataUsers(String color, String username, GameData previous) {
        return new GameData(previous.gameID(), color.equals("WHITE") ? username : previous.whiteUsername(),
                color.equals("BLACK") ? username : previous.blackUsername(), previous.gameName(), previous.game(), previous.stockfish());
    }
}
