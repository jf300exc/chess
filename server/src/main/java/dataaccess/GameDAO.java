package dataaccess;

import model.GameData;

import java.util.List;

public interface GameDAO {
    List<GameData> findGameData();
}
