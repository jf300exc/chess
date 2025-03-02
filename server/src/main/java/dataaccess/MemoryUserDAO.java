package dataaccess;

import model.UserData;

import java.util.HashSet;
import java.util.Set;

public class MemoryUserDAO implements UserDAO {
    private final Set<String> allUsernames = new HashSet<>();
    private final Set<UserData> allUserData = new HashSet<>();

    @Override
    public Boolean findUser(UserData userData) {
        return allUsernames.contains(userData.username());
    }

    @Override
    public void addUser(UserData userData) {
        allUsernames.add(userData.username());
        allUserData.add(userData);
    }

    @Override
    public void clear() {
        allUsernames.clear();
        allUserData.clear();
    }
}
