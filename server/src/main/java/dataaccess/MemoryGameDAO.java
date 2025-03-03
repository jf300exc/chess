package dataaccess;

import model.GameData;

import java.util.ArrayList;
import java.util.List;

public class MemoryGameDAO implements GameDAO {
    private final List<GameData> gameDataList = new ArrayList<>();

    @Override
    public List<GameData> findGameData() {
        return gameDataList;
    }
}
