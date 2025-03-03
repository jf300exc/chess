package service;

import Requests.LogoutRequest;
import Requests.LogoutResult;
import dataaccess.AuthDAO;
import dataaccess.MemoryAuthDAO;
import model.AuthData;

import java.util.UUID;

public class AuthService {
    static final AuthDAO authdao = new MemoryAuthDAO();

    public AuthData createAuth(String username) {
        String token = generateToken();
        AuthData newAuthData = new AuthData(token, username);
        authdao.addAuth(newAuthData);
        return newAuthData;
    }

    public boolean isAuthTokenUnavailable(String authToken) {
        return authdao.findAuthDataByAuthToken(authToken) == null;
    }

    public AuthData findAuthDataByAuthToken(String authToken) {
        return authdao.findAuthDataByAuthToken(authToken);
    }

    public LogoutResult logout(LogoutRequest logoutRequest) {
        LogoutResult result;
        AuthData authData = authdao.findAuthDataByAuthToken(logoutRequest.authToken());
        if (authData == null) {
            result = new LogoutResult("Error: unauthorized");
        } else {
            authdao.deleteAuth(authData);
            result = new LogoutResult("");
        }
        return result;
    }

    public void clearAuthDataBase() {
        authdao.clear();
    }

    private static String generateToken() {
        return UUID.randomUUID().toString();
    }
}
