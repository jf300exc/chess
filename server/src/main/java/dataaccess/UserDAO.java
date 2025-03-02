package dataaccess;

import model.UserData;

public interface UserDAO {
    Boolean findUser(UserData userData);

    void addUser(UserData userData);

    void clear();
}
