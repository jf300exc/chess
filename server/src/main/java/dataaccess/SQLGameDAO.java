package dataaccess;

import adapters.*;
import chess.ChessBoard;
import chess.ChessGame;
import chess.ChessPiece;
import chess.ChessPosition;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import model.GameData;
import model.StockfishPlayer;
import model.StockfishOptions;
import service.EloRating;

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
                .registerTypeAdapter(
                        new TypeToken<Map<TeamColor, Map<CastlePieceTypes, Map<CastleType, Boolean>>>>(){}.getType(),
                        new CastleRequirementsAdapter())
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

                gameDataList.add(new GameData(gameID, whiteUsername, blackUsername, gameName, chessGame,
                        gson.fromJson(resultSet.getString("stockfish"), StockfishPlayer.class)));
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

                    gameData = new GameData(gameIDint, whiteUsername, blackUsername, gameName, chessGame,
                            gson.fromJson(resultSet.getString("stockfish"), StockfishPlayer.class));
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
                INSERT INTO game_data (gameID, whiteUsername, blackUsername, gameName, game, stockfish)
                VALUES (?,?,?,?,?,?)
                """;
        try (var conn = DatabaseManager.getConnection();
             var statement = conn.prepareStatement(query)) {
            statement.setInt(1, gameData.gameID());
            statement.setString(2, gameData.whiteUsername());
            statement.setString(3, gameData.blackUsername());
            statement.setString(4, gameData.gameName());

            String gameString = serializeChessGame(gameData.game());
            statement.setString(5, gameString);
            statement.setString(6, gameData.stockfish() == null ? null : gson.toJson(gameData.stockfish()));

            statement.executeUpdate();
        } catch (DataAccessException | SQLException e) {
            System.err.println("SQLGameDAO: addGameData: " + e.getMessage());
        }
    }

    @Override
    public void removeGameDataByGameID(GameData gameData) {
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

    @Override
    public void saveGame(GameData game) {
        try (var conn = DatabaseManager.getConnection()) { save(conn, game); }
        catch (DataAccessException | SQLException e) { throw new IllegalStateException("Could not save game", e); }
    }

    private void save(java.sql.Connection conn, GameData game) throws SQLException {
        try (var statement = conn.prepareStatement("UPDATE game_data SET whiteUsername=?, blackUsername=?, game=?, stockfish=? WHERE gameID=?")) {
            statement.setString(1, game.whiteUsername());
            statement.setString(2, game.blackUsername());
            statement.setString(3, serializeChessGame(game.game()));
            statement.setString(4, game.stockfish() == null ? null : gson.toJson(game.stockfish()));
            statement.setInt(5, game.gameID());
            if (statement.executeUpdate() == 0) { throw new SQLException("Game no longer exists"); }
        }
    }

    @Override
    public void finishGame(GameData game, TeamColor winner) {
        if (!game.game().isGameOver()) { throw new IllegalArgumentException("Game must be over before rating it"); }
        try (var conn = DatabaseManager.getConnection()) {
            conn.setAutoCommit(false);
            try {
                try (var query = conn.prepareStatement("SELECT rated FROM game_data WHERE gameID=? FOR UPDATE")) {
                    query.setInt(1, game.gameID());
                    try (var row = query.executeQuery()) {
                        if (!row.next() || row.getBoolean(1)) { conn.rollback(); return; }
                    }
                }
                // Lock accounts in a stable order to avoid deadlocks across simultaneous matches.
                var ratings = new java.util.TreeMap<String, Integer>();
                for (String name : new String[]{game.whiteUsername(), game.blackUsername()}) {
                    if (name != null && !(game.stockfish() != null && name.equals(StockfishPlayer.USERNAME))) {
                        ratings.put(name, 0);
                    }
                }
                for (String name : ratings.keySet()) {
                    try (var query = conn.prepareStatement("SELECT elo FROM user_data WHERE username=? FOR UPDATE")) {
                        query.setString(1, name);
                        try (var row = query.executeQuery()) { if (row.next()) { ratings.put(name, row.getInt(1)); } }
                    }
                }
                boolean rated = game.whiteUsername() != null && game.blackUsername() != null
                        && !game.whiteUsername().equals(game.blackUsername())
                        && (game.stockfish() == null || game.stockfish().mode() == StockfishOptions.Mode.ELO)
                        && ratings.values().stream().allMatch(elo -> elo > 0);
                if (rated) {
                    int white = game.stockfish() != null && game.stockfish().color() == TeamColor.WHITE
                            ? game.stockfish().elo() : ratings.get(game.whiteUsername());
                    int black = game.stockfish() != null && game.stockfish().color() == TeamColor.BLACK
                            ? game.stockfish().elo() : ratings.get(game.blackUsername());
                    double whiteScore = winner == null ? 0.5 : winner == TeamColor.WHITE ? 1 : 0;
                    for (var entry : ratings.entrySet()) {
                        boolean isWhite = entry.getKey().equals(game.whiteUsername());
                        int next = EloRating.after(entry.getValue(), isWhite ? black : white, isWhite ? whiteScore : 1 - whiteScore);
                        try (var update = conn.prepareStatement("UPDATE user_data SET elo=? WHERE username=?")) {
                            update.setInt(1, next); update.setString(2, entry.getKey()); update.executeUpdate();
                        }
                    }
                }
                save(conn, game);
                try (var update = conn.prepareStatement("UPDATE game_data SET rated=TRUE WHERE gameID=?")) {
                    update.setInt(1, game.gameID()); update.executeUpdate();
                }
                conn.commit();
            } catch (SQLException | RuntimeException e) { conn.rollback(); throw e; }
        } catch (DataAccessException | SQLException e) { throw new IllegalStateException("Could not finish game", e); }
    }

    public String serializeChessGame(ChessGame chessGame) {
        return gson.toJson(chessGame);
    }

    public ChessGame deserializeChessGame(String chessGameJSON) {
        return gson.fromJson(chessGameJSON, ChessGame.class);
    }
}
