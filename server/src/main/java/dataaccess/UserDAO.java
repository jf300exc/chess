package dataaccess;

import model.UserData;

public interface UserDAO {
    void addUser(UserData userData);

    UserData findUserDataByUsername(String username);

    void clear();
}
