package dataaccess;

import model.UserData;

import java.util.Collection;

public interface UserDAO {
    Collection<UserData> getAllUserData();

    void addUser(UserData userData);

    UserData findUserDataByUsername(String username);

    void clear();
}
