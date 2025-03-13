package service;

import dataaccess.SQLAuthDAO;
import requests.LogoutRequest;
import requests.LogoutResult;
import dataaccess.AuthDAO;
import dataaccess.MemoryAuthDAO;
import model.AuthData;

import java.util.UUID;

public class AuthService {
    static final AuthDAO AUTHDAO = new SQLAuthDAO();

    public AuthData createAuth(String username) {
        if (username == null) {
            throw new NullPointerException("User must have a username string");
        }
        String token = generateToken();
        AuthData newAuthData = new AuthData(token, username);
        AUTHDAO.addAuth(newAuthData);
        return newAuthData;
    }

    public boolean isAuthTokenUnavailable(String authToken) {
        return AUTHDAO.findAuthDataByAuthToken(authToken) == null;
    }

    public AuthData findAuthDataByAuthToken(String authToken) {
        return AUTHDAO.findAuthDataByAuthToken(authToken);
    }

    public AuthDAO getAuthDatabase() {
        return AUTHDAO;
    }

    public LogoutResult logout(LogoutRequest logoutRequest) {
        LogoutResult result;
        AuthData authData = AUTHDAO.findAuthDataByAuthToken(logoutRequest.authToken());
        if (authData == null) {
            result = new LogoutResult("Error: unauthorized");
        } else {
            AUTHDAO.deleteAuth(authData);
            result = new LogoutResult("");
        }
        return result;
    }

    public void clearAuthDataBase() {
        AUTHDAO.clear();
    }

    private static String generateToken() {
        return UUID.randomUUID().toString();
    }
}
