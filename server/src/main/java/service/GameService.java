package service;

import Requests.*;
import chess.ChessGame;
import dataaccess.GameDAO;
import dataaccess.MemoryGameDAO;
import model.AuthData;
import model.GameData;
import model.GameEntry;

import java.util.ArrayList;
import java.util.Collection;

public class GameService {
    private static final GameDAO gamedao = new MemoryGameDAO();
    private static final AuthService authService = new AuthService();
    private static int gameIDCounter = 1;

    public ListGamesResult listGames(ListGamesRequest listGamesRequest) {
        ListGamesResult result;
        if (authService.isAuthTokenUnavailable(listGamesRequest.authToken())) {
            result = new ListGamesResult(null, "Error: unauthorized");
        } else {
            Collection<GameEntry> gameList = new ArrayList<>();
            Collection<GameData> fullGameData = gamedao.findGameData();
            for (GameData game : fullGameData) {
                gameList.add(new GameEntry(game));
            }
            result = new ListGamesResult(gameList, "");
        }
        return result;
    }

    public CreateGameResult createGame(CreateGameRequest createGameRequest) {
        CreateGameResult result;
        if (authService.isAuthTokenUnavailable(createGameRequest.authToken())) {
            result = new CreateGameResult(null, "Error: unauthorized");
        } else {
            String newGameName = createGameRequest.gameName();
            ChessGame game = new ChessGame();
            GameData gameData = new GameData(gameIDCounter, null, null, newGameName, game);
            gamedao.addGameData(gameData);
            String gameID = String.valueOf(gameIDCounter++);
            result = new CreateGameResult(gameID, "");
        }
        return result;
    }

    public JoinGameResult joinGame(JoinGameRequest joinGameRequest) {
        // Authorization and getting authData for username
        AuthData authData = authService.findAuthDataByAuthToken(joinGameRequest.authToken());
        if (authData == null) {
            return new JoinGameResult("Error: unauthorized");
        }

        // If the game is not found, then bad request
        GameData gameData;
        try {
            gameData = gamedao.findGameDataByID(joinGameRequest.gameID());
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
            gameData = GameData.updateGameData(playerColor, username, gameData);
            gamedao.addGameData(gameData);
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
        gamedao.clear();
    }
}
