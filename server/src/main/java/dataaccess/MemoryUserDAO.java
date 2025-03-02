package dataaccess;

import model.UserData;

import java.util.HashSet;
import java.util.Set;

public class MemoryUserDAO implements UserDAO {
    private final Set<String> allUsernames = new HashSet<>();
    private final Set<UserData> allUserData = new HashSet<>();

    @Override
    public Boolean getUser(UserData userData) {
        if (allUsernames.contains(userData.username())) {
            return false;
        }
        return !allUserData.contains(userData);
    }

    @Override
    public void createUser(UserData userData) {
        allUsernames.add(userData.username());
        allUserData.add(userData);
    }
}
