package requests;

import model.StockfishOptions;

public record AddStockfishRequest(String authToken, String gameID, StockfishOptions stockfish) { }
