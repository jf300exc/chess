package requests;

public record CreateGameRequest(String authToken, String gameName) {
}
