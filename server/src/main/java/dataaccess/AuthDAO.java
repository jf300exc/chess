package dataaccess;

import model.AuthData;

public interface AuthDAO {
    void addAuth(AuthData authData);

    AuthData findAuthDataByAuthToken(String authToken);

    void deleteAuth(AuthData authData);

    void clear();
}
