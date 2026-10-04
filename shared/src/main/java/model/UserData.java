package model;

public record UserData(String username, String password, String email, int elo) {
    public static final int BASE_ELO = 1500;

    public UserData(String username, String password, String email) {
        this(username, password, email, BASE_ELO);
    }
}
