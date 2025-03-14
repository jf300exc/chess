package service;

import dataaccess.GameIDCounter;
import requests.*;
import chess.ChessGame;
import dataaccess.GameDAO;
import dataaccess.SQLGameDAO;
import model.AuthData;
import model.GameData;
import model.GameEntry;

import java.util.ArrayList;
import java.util.Collection;

public class GameService {
    private static final GameDAO GAMEDAO = new SQLGameDAO();
    private static final AuthService AUTH_SERVICE = new AuthService();

    public ListGamesResult listGames(ListGamesRequest listGamesRequest) {
        ListGamesResult result;
        if (AUTH_SERVICE.isAuthTokenUnavailable(listGamesRequest.authToken())) {
            result = new ListGamesResult(null, "Error: unauthorized");
        } else {
            Collection<GameEntry> gameList = new ArrayList<>();
            Collection<GameData> fullGameData = GAMEDAO.findGameData();
            for (GameData game : fullGameData) {
                gameList.add(new GameEntry(game));
            }
            result = new ListGamesResult(gameList, "");
        }
        return result;
    }

    public CreateGameResult createGame(CreateGameRequest createGameRequest) {
        CreateGameResult result;
        if (AUTH_SERVICE.isAuthTokenUnavailable(createGameRequest.authToken())) {
            result = new CreateGameResult(null, "Error: unauthorized");
        } else {
            String newGameName = createGameRequest.gameName();
            ChessGame game = new ChessGame();
            int gameIDCounter = GameIDCounter.getNewGameID();
            GameData gameData = new GameData(gameIDCounter, null, null, newGameName, game);
            GAMEDAO.addGameData(gameData);
            String gameID = String.valueOf(gameIDCounter);
            result = new CreateGameResult(gameID, "");
        }
        return result;
    }

    public JoinGameResult joinGame(JoinGameRequest joinGameRequest) {
        // Authorization and getting authData for username
        AuthData authData = AUTH_SERVICE.findAuthDataByAuthToken(joinGameRequest.authToken());
        if (authData == null) {
            return new JoinGameResult("Error: unauthorized");
        }

        // If the game is not found, then bad request
        GameData gameData;
        try {
            gameData = GAMEDAO.findGameDataByID(joinGameRequest.gameID());
        } catch (NumberFormatException e) {
            return new JoinGameResult("Error: bad request");
        }

        // Prepare the result
        return updateGame(joinGameRequest.playerColor(), authData.username(), gameData);
    }

    private static JoinGameResult updateGame(String playerColor, String username, GameData gameData) {
        JoinGameResult result;
        if (gameData == null) {
            result = new JoinGameResult("Error: game does not exist");
        } else if (playerColor.equals("WHITE") || playerColor.equals("BLACK")) {
            result = attemptAddPlayer(playerColor, username, gameData);
        } else {
            result = new JoinGameResult("Error: bad request");
        } return result;
    }

    private static JoinGameResult attemptAddPlayer(String playerColor, String username, GameData gameData) {
        JoinGameResult result;
        if (playerColorUnavailable(playerColor, gameData)) {
            result = new JoinGameResult("Error: already taken");
        } else {
            GAMEDAO.removeGameData(gameData);
            gameData = GameData.updateGameData(playerColor, username, gameData);
            GAMEDAO.addGameData(gameData);
            result = new JoinGameResult("");
        }
        return result;
    }

    private static boolean playerColorUnavailable(String playerColor, GameData gameData) {
        if (playerColor.equals("WHITE")) {
            return gameData.whiteUsername() != null;
        }
        return gameData.blackUsername() != null;
    }

    public void clearGameDataBase() {
        GAMEDAO.clear();
    }
}
