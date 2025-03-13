package service;

import dataaccess.SQLUserDAO;
import org.mindrot.jbcrypt.BCrypt;
import requests.LoginRequest;
import requests.LoginResult;
import requests.RegisterRequest;
import requests.RegisterResult;
import dataaccess.UserDAO;
import model.AuthData;
import model.UserData;

public class UserService {
    private static final UserDAO USERDOA = new SQLUserDAO();
    private static final AuthService AUTH_SERVICE = new AuthService();

    public RegisterResult register(RegisterRequest registerRequest) {
        UserData newUserData = new UserData(registerRequest.username(),
                registerRequest.password(), registerRequest.email());
        RegisterResult result;
        if (USERDOA.findUserDataByUsername(newUserData.username()) != null) {
            result = new RegisterResult("", "", "Error: already taken");
        } else {
            USERDOA.addUser(newUserData);
            AuthData newAuthData = AUTH_SERVICE.createAuth(newUserData.username());
            result = new RegisterResult(newUserData.username(), newAuthData.authToken(), "");
        }
        return result;
    }

    public LoginResult login(LoginRequest loginRequest) {
        LoginResult result;
        UserData userData = USERDOA.findUserDataByUsername(loginRequest.username());
        if (userData == null) {
            result = new LoginResult("", "", "Error: unauthorized");
        } else if (!verifyPassword(loginRequest.password(), userData.password())) {
            result = new LoginResult("", "", "Error: unauthorized");
        } else {
            AuthData newAuthData = AUTH_SERVICE.createAuth(loginRequest.username());
            result = new LoginResult(newAuthData.username(), newAuthData.authToken(), "");
        }
        return result;
    }

    public UserDAO getUserDataBase() {
        return USERDOA;
    }

    public void clearUserDataBase() {
        USERDOA.clear();
    }

    private boolean verifyPassword(String password, String hashedPassword) {
        return BCrypt.checkpw(password, hashedPassword);
    }
}
