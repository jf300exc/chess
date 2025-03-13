package dataaccess;

import model.GameData;

import java.util.Collection;
import java.util.List;

public class SQLGameDAO implements  GameDAO {

    @Override
    public Collection<GameData> findGameData() {
        return List.of();
    }

    @Override
    public GameData findGameDataByID(String gameID) {
        return null;
    }

    @Override
    public void addGameData(GameData gameData) {

    }

    @Override
    public void clear() {

    }
}
