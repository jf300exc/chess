package dataaccess;

import model.GameData;

import java.util.Collection;

public interface GameDAO {
    Collection<GameData> findGameData();

    GameData findGameDataByID(String gameID);

    void addGameData(GameData gameData);

    void removeGameData(GameData gameData);

    void clear();
}
