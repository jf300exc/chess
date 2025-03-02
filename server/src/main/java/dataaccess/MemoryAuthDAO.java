package dataaccess;

import model.AuthData;

public class MemoryAuthDAO implements AuthDAO {
    @Override
    public void createAuth(AuthData authData) {
        throw new RuntimeException("Not implemented");
    }
}
