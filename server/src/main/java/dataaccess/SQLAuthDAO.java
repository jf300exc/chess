package dataaccess;

import model.AuthData;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class SQLAuthDAO implements AuthDAO {

    @Override
    public void addAuth(AuthData authData) {
        String query = """
                INSERT INTO auth_data(authToken,username)
                VALUES (?, ?)
                """;
        try (var conn = DatabaseManager.getConnection();
             var statement = conn.prepareStatement(query)) {
            statement.setString(1, authData.authToken());
            statement.setString(2, authData.username());

            statement.executeUpdate();
        } catch (DataAccessException | SQLException e) {
            System.err.println("SQLAuthDAO: addAuth" + e.getMessage());
        }
    }

    @Override
    public AuthData findAuthDataByAuthToken(String authToken) {
        AuthData authData = null;

        String query = """
                SELECT * FROM auth_data WHERE authToken = ?
                """;
        try (var conn = DatabaseManager.getConnection();
             var statement = conn.prepareStatement(query)) {
            statement.setString(1, authToken);

            try (var resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    String authTokenData = resultSet.getString("authToken");
                    String username = resultSet.getString("username");

                    authData = new AuthData(authTokenData, username);
                }
            }
        } catch (DataAccessException | SQLException e) {
            System.err.println("SQLAuthDAO: findAuthDataByAuthToken" + e.getMessage());
        }

        return authData;
    }

    @Override
    public Collection<AuthData> getAllAuthData() {
        List<AuthData> authDataList = new ArrayList<>();

        String query = "SELECT * FROM auth_data";
        try (var conn = DatabaseManager.getConnection();
             var statement = conn.createStatement();
             var resultSet = statement.executeQuery(query)) {

            while (resultSet.next()) {
                String authToken = resultSet.getString("authToken");
                String username = resultSet.getString("username");
                authDataList.add(new AuthData(authToken, username));
            }

        } catch (DataAccessException | SQLException e) {
            System.err.println("SQLAuthDAO: getAllAuthData: " + e.getMessage());
        }
        return authDataList;
    }

    @Override
    public void deleteAuth(AuthData authData) {
        String query = """
                DELETE FROM auth_data WHERE authToken = ?
                """;
        try (var conn = DatabaseManager.getConnection();
             var statement = conn.prepareStatement(query)) {
            statement.setString(1, authData.authToken());

            statement.executeUpdate();
        } catch (DataAccessException | SQLException e) {
            System.err.println("SQLAuthDAO: deleteAuth" + e.getMessage());
        }
    }

    @Override
    public void clear() {
        String query = "TRUNCATE TABLE auth_data";

        try (var conn = DatabaseManager.getConnection();
             var statement = conn.prepareStatement(query)) {
            statement.executeUpdate();
        } catch (DataAccessException | SQLException e) {
            System.err.println("SQLAuthDAO: clear: " + e.getMessage());
        }
    }
}
