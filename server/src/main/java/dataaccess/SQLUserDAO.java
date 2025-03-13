package dataaccess;

import model.UserData;
import org.mindrot.jbcrypt.BCrypt;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class SQLUserDAO implements UserDAO {

    @Override
    public Collection<UserData> getAllUserData() {
        List<UserData> users = new ArrayList<>();

        String query = "SELECT * FROM user_data";
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
                INSERT INTO user_data (username, passwordHash, email)
                VALUES (?, ?, ?)
                """;
        try (var conn = DatabaseManager.getConnection();
             var statement = conn.prepareStatement(query)) {
            statement.setString(1, userData.username());

            // Hash Password
            String passwordHash = hashPassword(userData.password());
            statement.setString(2, passwordHash);

            statement.setString(3, userData.email());

            statement.executeUpdate();
        } catch (DataAccessException | SQLException e) {
            System.err.println("SQLUserDAO: addUser: " + e.getMessage());
        }
    }

    @Override
    public UserData findUserDataByUsername(String username) {
        UserData userData = null;

        String query = """
                SELECT * FROM user_data WHERE username = ?
                """;
        try (var conn = DatabaseManager.getConnection();
             var statement = conn.prepareStatement(query)) {
            statement.setString(1, username);

            try (var resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    String password = resultSet.getString("passwordHash");
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
        String query = "TRUNCATE TABLE user_data";

        try (var conn = DatabaseManager.getConnection();
             var statement = conn.prepareStatement(query)) {
            statement.executeUpdate();
        } catch (DataAccessException | SQLException e) {
            System.err.println("SQLUserDAO: clear: " + e.getMessage());
        }
    }

    private String hashPassword(String password) {
        return BCrypt.hashpw(password, BCrypt.gensalt());
    }
}
