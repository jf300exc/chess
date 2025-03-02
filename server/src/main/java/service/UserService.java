package service;

import Requests.RegisterRequest;
import Requests.RegisterResult;
import dataaccess.MemoryUserDAO;
import dataaccess.UserDAO;
import model.AuthData;
import model.UserData;

public class UserService {
    static final UserDAO userdao = new MemoryUserDAO();
    static final AuthService authService = new AuthService();

    public RegisterResult register(RegisterRequest registerRequest) {
        UserData newUserData = new UserData(registerRequest.username(),
                registerRequest.password(), registerRequest.email());
        RegisterResult result;
        if (userdao.getUser(newUserData)) {
            result = new RegisterResult("", "", "Error: already taken");
        } else {
            userdao.addUser(newUserData);
            AuthData newAuthData = authService.createAuth(newUserData.username());
            result = new RegisterResult(newUserData.username(), newAuthData.authToken(), "");
        }
        return result;
    }
}
