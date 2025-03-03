package dataaccess;

import model.UserData;

import java.util.*;

public class MemoryUserDAO implements UserDAO {
    private final Map<String, UserData> userDataByUsername = new HashMap<>();

    @Override
    public void addUser(UserData userData) {
        userDataByUsername.put(userData.username(), userData);
    }

    @Override
    public UserData findUserDataByUsername(String username) {
        return userDataByUsername.get(username);
    }

    @Override
    public void clear() {
        userDataByUsername.clear();
    }
}
