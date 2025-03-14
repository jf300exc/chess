package dataaccess;

import java.sql.ResultSet;
import java.sql.SQLException;

public class GameIDCounter {
    public static int getNewGameID() {
        String query = "SELECT MAX(gameID) FROM game_data";

        int gameID = 0;

        try (var conn = DatabaseManager.getConnection();
             var statement = conn.prepareStatement(query)) {

            try (var resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    gameID = getInt(resultSet);
                }
            }

        } catch (DataAccessException | SQLException e) {
            System.err.println("GameIDCounter: getNewGameID: " + e.getMessage());
        }

        return ++gameID;
    }

    private static int getInt(ResultSet result) {
        try {
            Integer maxGameId = result.getObject(1, Integer.class);
            return maxGameId == null ? 0 : maxGameId;
        } catch (SQLException e) {
            System.err.println("GameIDCounter: getInt: " + e.getMessage());
            return 0;
        }
    }

}
