package service;

import Requests.ListGamesRequest;
import Requests.ListGamesResult;
import dataaccess.GameDAO;
import dataaccess.MemoryGameDAO;
import model.GameData;
import model.GameEntry;

import java.util.ArrayList;
import java.util.List;

public class GameService {
    private static final GameDAO gamedao = new MemoryGameDAO();
    private static final AuthService authService = new AuthService();

    public ListGamesResult listGames(ListGamesRequest listGamesRequest) {
        ListGamesResult result;
        if (!authService.matchAuthTokenToAuthData(listGamesRequest.authToken())) {
            result = new ListGamesResult(null, "Error: unauthorized");
        } else {
            List<GameEntry> gameList = new ArrayList<>();
            List<GameData> fullGameData = gamedao.findGameData();
            for (GameData game : fullGameData) {
                gameList.add(new GameEntry(game));
            }
            result = new ListGamesResult(gameList, "");
        }
        return result;
    }
}
