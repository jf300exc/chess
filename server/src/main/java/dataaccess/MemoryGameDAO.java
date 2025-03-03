package dataaccess;

import model.GameData;

import java.util.*;

public class MemoryGameDAO implements GameDAO {
    private final Map<Integer, GameData> gameDataMap = new HashMap<>();

    @Override
    public Collection<GameData> findGameData() {
        return gameDataMap.values();
    }

    @Override
    public GameData findGameDataByID(String gameID) {
        return gameDataMap.get(Integer.parseInt(gameID));
    }

    @Override
    public void addGameData(GameData gameData) {
        gameDataMap.put(gameData.gameID(), gameData);
    }
}
