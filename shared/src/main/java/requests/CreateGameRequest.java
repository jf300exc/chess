package requests;

import model.StockfishOptions;

public record CreateGameRequest(String authToken, String gameName, StockfishOptions stockfish) {
    public CreateGameRequest(String authToken, String gameName) {
        this(authToken, gameName, null);
    }
}
