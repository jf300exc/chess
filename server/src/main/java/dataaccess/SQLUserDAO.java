package dataaccess;

import model.UserData;
import org.mindrot.jbcrypt.BCrypt;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class SQLUserDAO implements UserDAO {

    @Override
    public Collection<UserData> getAllUserData() {
        List<UserData> users = new ArrayList<>();

        String query = "SELECT * FROM users_data";
        try (var conn = DatabaseManager.getConnection();
             var statement = conn.createStatement();
             var resultSet = statement.executeQuery(query)) {

            while (resultSet.next()) {
                String username = resultSet.getString("username");
                String password = resultSet.getString("password");
                String passwordHash = resultSet.getString("passwordHash");
                users.add(new UserData(username, password, passwordHash));
            }

        } catch (DataAccessException | SQLException e) {
            System.err.println("SQLUserDAO: getAllUserData: " + e.getMessage());
        }
        return users;
    }

    @Override
    public void addUser(UserData userData) {
        String query = """
                INSERT INTO users_data (username, password, passwordHash)
                VALUES (?, ?, ?)
                """;
        try (var conn = DatabaseManager.getConnection();
             var statement = conn.prepareStatement(query)) {
            statement.setString(1, userData.username());
            statement.setString(2, userData.password());

            // Hash Password
            String passwordHash = hashPassword(userData.password());
            statement.setString(3, passwordHash);
        } catch (DataAccessException | SQLException e) {
            System.err.println("SQLUserDAO: addUser: " + e.getMessage());
        }
    }

    @Override
    public UserData findUserDataByUsername(String username) {
        String query = """
                SELECT * FROM users_data WHERE username = ?
                """;
        UserData userData = null;

        try (var conn = DatabaseManager.getConnection();
             var statement = conn.prepareStatement(query)) {
            statement.setString(1, username);

            try (var resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    String password = resultSet.getString("password");
                    String email = resultSet.getString("email");

                    userData = new UserData(username, password, email);
                }
            }
        } catch (DataAccessException | SQLException e) {
            System.err.println("SQLUserDAO: addUser: " + e.getMessage());
        }

        return userData;
    }

    @Override
    public void clear() {

    }

    private String hashPassword(String password) {
        return BCrypt.hashpw(password, BCrypt.gensalt());
    }
}
