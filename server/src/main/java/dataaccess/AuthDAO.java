package dataaccess;

import model.AuthData;

import java.util.Collection;

public interface AuthDAO {
    void addAuth(AuthData authData);

    AuthData findAuthDataByAuthToken(String authToken);

    Collection<AuthData> getAllAuthData();

    void deleteAuth(AuthData authData);

    void clear();
}
