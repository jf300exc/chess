package dataaccess;

import model.GameData;

import java.util.Collection;

public interface GameDAO {
    Collection<GameData> findGameData();

    GameData findGameDataByID(String gameID);

    void addGameData(GameData gameData);

    void removeGameDataByGameID(GameData gameData);

    default void saveGame(GameData game) {
        removeGameDataByGameID(game);
        addGameData(game);
    }

    /** Persist a terminal board and settle ratings once. Null winner means a draw. */
    default void finishGame(GameData game, chess.ChessGame.TeamColor winner) { saveGame(game); }

    void clear();
}
