package service;

import Requests.LoginRequest;
import Requests.LoginResult;
import Requests.RegisterRequest;
import Requests.RegisterResult;
import dataaccess.MemoryUserDAO;
import dataaccess.UserDAO;
import model.AuthData;
import model.UserData;

public class UserService {
    private static final UserDAO userdao = new MemoryUserDAO();
    private static final AuthService authService = new AuthService();

    public RegisterResult register(RegisterRequest registerRequest) {
        UserData newUserData = new UserData(registerRequest.username(),
                registerRequest.password(), registerRequest.email());
        RegisterResult result;
        if (userdao.findUserDataByUsername(newUserData.username()) != null) {
            result = new RegisterResult("", "", "Error: already taken");
        } else {
            userdao.addUser(newUserData);
            AuthData newAuthData = authService.createAuth(newUserData.username());
            result = new RegisterResult(newUserData.username(), newAuthData.authToken(), "");
        }
        return result;
    }

    public LoginResult login(LoginRequest loginRequest) {
        LoginResult result;
        UserData userData = userdao.findUserDataByUsername(loginRequest.username());
        if (userData == null) {
            result = new LoginResult("", "", "Error: unauthorized");
        } else if (!userData.password().equals(loginRequest.password())) {
            result = new LoginResult("", "", "Error: unauthorized");
        } else {
            AuthData newAuthData = authService.createAuth(loginRequest.username());
            result = new LoginResult(newAuthData.username(), newAuthData.authToken(), "");
        }
        return result;
    }

    public UserDAO getUserDataBase() {
        return userdao;
    }

    public void clearUserDataBase() {
        userdao.clear();
    }
}
