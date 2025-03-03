package dataaccess;

import model.AuthData;

import java.util.HashMap;
import java.util.Map;

public class MemoryAuthDAO implements AuthDAO {
    private final Map<String, AuthData> authData = new HashMap<>();

    @Override
    public void addAuth(AuthData authData) {
        this.authData.put(authData.authToken(), authData);
    }

    @Override
    public AuthData findAuthDataByAuthToken(String authToken) {
        return authData.get(authToken);
    }

    @Override
    public void deleteAuth(AuthData authData) {
        this.authData.remove(authData.authToken());
    }

    @Override
    public void clear() {
        authData.clear();
    }
}
