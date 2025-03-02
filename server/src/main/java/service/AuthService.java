package service;

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

    public static String generateToken() {
        return UUID.randomUUID().toString();
    }
}
