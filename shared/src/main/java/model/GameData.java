package model;

import chess.ChessGame;

public record GameData(int gameID, String whiteUsername, String blackUsername, String gameName, ChessGame game) {
    public static GameData updateGameData(String color, String username, GameData previous) {
        if (color.equals("WHITE")) {
            return new GameData(previous.gameID(), username, previous.blackUsername(),
                    previous.gameName(), previous.game());
        } else {
            return new GameData(previous.gameID(), previous.whiteUsername(), username,
                    previous.gameName(), previous.game());
        }
    }
}
