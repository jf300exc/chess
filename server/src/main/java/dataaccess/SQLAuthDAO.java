package dataaccess;

import model.AuthData;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class SQLAuthDAO implements AuthDAO {

    @Override
    public void addAuth(AuthData authData) {

    }

    @Override
    public AuthData findAuthDataByAuthToken(String authToken) {
        return null;
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

    }

    @Override
    public void clear() {

    }
}
