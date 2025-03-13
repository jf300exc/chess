package dataaccess;

import model.AuthData;

import java.util.Collection;
import java.util.List;

public class SQLAuthDAO implements AuthDAO {

    @Override
    public void addAuth(AuthData authData) {

    }

    @Override
    public AuthData findAuthDataByAuthToken(String authToken) {
        return null;
    }

    @Override
    public Collection<AuthData> getAllAuthData() {
        return List.of();
    }

    @Override
    public void deleteAuth(AuthData authData) {

    }

    @Override
    public void clear() {

    }
}
