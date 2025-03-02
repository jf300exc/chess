package dataaccess;

import model.AuthData;

import java.util.HashSet;
import java.util.Set;

public class MemoryAuthDAO implements AuthDAO {
    private final Set<AuthData> authData = new HashSet<>();

    @Override
    public void addAuth(AuthData authData) {
        this.authData.add(authData);
    }

    @Override
    public void clear() {
        authData.clear();
    }
}
