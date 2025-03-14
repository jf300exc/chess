package dataaccess;

import chess.ChessBoard;
import chess.ChessGame;
import chess.ChessPiece;
import chess.ChessPosition;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import model.GameData;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import chess.ChessGame.TeamColor;
import chess.ChessBoard.*;


public class SQLGameDAO implements GameDAO {
    private final Gson gson;

    public SQLGameDAO() {
        gson = new GsonBuilder()
                .registerTypeAdapter(ChessGame.class, new ChessGameAdapter())
                .registerTypeAdapter(ChessBoard.class, new ChessBoardAdapter())
                .registerTypeAdapter(ChessPiece.class, new ChessPieceAdapter())
                .registerTypeAdapter(ChessPosition.class, new ChessPositionAdapter())
                .registerTypeAdapter(new TypeToken<Map<TeamColor, Map<CastlePieceTypes, Map<CastleType, Boolean>>>>(){}.getType(), new CastleRequirementsAdapter())
                .create();
    }

    @Override
    public Collection<GameData> findGameData() {
        List<GameData> gameDataList = new ArrayList<>();

        String query = "SELECT * FROM game_data";
        try (var conn = DatabaseManager.getConnection();
             var statement = conn.createStatement();
             var resultSet = statement.executeQuery(query)) {

            while (resultSet.next()) {
                int gameID = resultSet.getInt("gameID");

                String whiteUsername = resultSet.getString("whiteUsername");
                String blackUsername = resultSet.getString("blackUsername");
                String gameName = resultSet.getString("gameName");

                String game = resultSet.getString("game");
                ChessGame chessGame = deserializeChessGame(game);

                gameDataList.add(new GameData(gameID, whiteUsername, blackUsername, gameName, chessGame));
            }

        } catch (DataAccessException | SQLException e) {
            System.err.println("SQLGameDAO: getAllGameData: " + e.getMessage());
        }
        return gameDataList;
    }

    @Override
    public GameData findGameDataByID(String gameID) {
        GameData gameData = null;
        int gameIDint = Integer.parseInt(gameID);

        String query = "SELECT * FROM game_data WHERE gameID = ?";

        try (var conn = DatabaseManager.getConnection();
             var statement = conn.prepareStatement(query)) {
            statement.setInt(1, gameIDint);

            try (var resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    String whiteUsername = resultSet.getString("whiteUsername");
                    String blackUsername = resultSet.getString("blackUsername");
                    String gameName = resultSet.getString("gameName");
                    String game = resultSet.getString("game");
                    ChessGame chessGame = deserializeChessGame(game);

                    gameData = new GameData(gameIDint, whiteUsername, blackUsername, gameName, chessGame);
                }
            }
        } catch (DataAccessException | SQLException e) {
            System.err.println("SQLGameDAO: findGameDataByID: " + e.getMessage());
        }

        return gameData;
    }

    @Override
    public void addGameData(GameData gameData) {
        String query = """
                INSERT INTO game_data (gameID, whiteUsername, blackUsername, gameName, game)
                VALUES (?,?,?,?,?)
                """;
        try (var conn = DatabaseManager.getConnection();
             var statement = conn.prepareStatement(query)) {
            statement.setInt(1, gameData.gameID());
            statement.setString(2, gameData.whiteUsername());
            statement.setString(3, gameData.blackUsername());
            statement.setString(4, gameData.gameName());

            String gameString = serializeChessGame(gameData.game());
            statement.setString(5, gameString);

            statement.executeUpdate();
        } catch (DataAccessException | SQLException e) {
            System.err.println("SQLGameDAO: addGameData: " + e.getMessage());
        }
    }

    @Override
    public void removeGameData(GameData gameData) {
        String query = """
                DELETE FROM game_data WHERE gameID = ?
                """;
        try (var conn = DatabaseManager.getConnection();
             var statement = conn.prepareStatement(query)) {
            statement.setInt(1, gameData.gameID());
            statement.executeUpdate();
        } catch (DataAccessException | SQLException e) {
            System.err.println("SQLGameDAO: removeGameData: " + e.getMessage());
        }
    }

    @Override
    public void clear() {
        String query = "TRUNCATE TABLE game_data";

        try (var conn = DatabaseManager.getConnection();
             var statement = conn.prepareStatement(query)) {
            statement.executeUpdate();
        } catch (DataAccessException | SQLException e) {
            System.err.println("SQLGameDAO: clear: " + e.getMessage());
        }
    }

    public String serializeChessGame(ChessGame chessGame) {
        return gson.toJson(chessGame);
    }

    public ChessGame deserializeChessGame(String chessGameJSON) {
        return gson.fromJson(chessGameJSON, ChessGame.class);
    }
}
