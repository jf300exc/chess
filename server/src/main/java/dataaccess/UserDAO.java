package dataaccess;

import model.UserData;

public interface UserDAO {
    Boolean getUser(UserData userData);

    void createUser(UserData userData);
}
