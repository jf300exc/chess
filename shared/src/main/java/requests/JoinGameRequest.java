package requests;

public record JoinGameRequest(String authToken, String playerColor, String gameID) {
}
