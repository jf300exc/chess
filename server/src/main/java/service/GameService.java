package service;

import chess.ChessGame;
import dataaccess.*;
import engine.StockfishEngine;
import model.*;
import requests.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.function.IntSupplier;

public class GameService {
    private static final Object CREATE_LOCK = new Object();
    private final GameDAO games;
    private final AuthDAO auth;
    private final UserDAO users;
    private final IntSupplier nextID;
    private final EngineFactory engines;

    @FunctionalInterface
    public interface EngineFactory { StockfishEngine open() throws IOException; }

    public GameService() {
        this(new SQLGameDAO(), new SQLAuthDAO(), new SQLUserDAO(), GameIDCounter::getNewGameID, StockfishEngine::new);
    }

    public GameService(GameDAO games, AuthDAO auth, UserDAO users, IntSupplier nextID, EngineFactory engines) {
        this.games = games; this.auth = auth; this.users = users; this.nextID = nextID; this.engines = engines;
    }

    public ListGamesResult listGames(ListGamesRequest request) {
        if (auth.findAuthDataByAuthToken(request.authToken()) == null) { return new ListGamesResult(null, "Error: unauthorized"); }
        var list = new ArrayList<GameEntry>();
        games.findGameData().forEach(game -> list.add(new GameEntry(game)));
        return new ListGamesResult(list, "");
    }

    public PlayerProfile profile(String token) {
        AuthData account = auth.findAuthDataByAuthToken(token);
        UserData user = account == null ? null : users.findUserDataByUsername(account.username());
        return user == null ? null : new PlayerProfile(user.username(), user.elo());
    }

    public CreateGameResult createGame(CreateGameRequest request) {
        AuthData account = auth.findAuthDataByAuthToken(request.authToken());
        if (account == null) { return new CreateGameResult(null, "Error: unauthorized"); }
        if (request.gameName() == null || request.gameName().isBlank() || request.gameName().length() > 255) {
            return new CreateGameResult(null, "Error: bad request");
        }
        StockfishPlayer stockfish = null;
        if (request.stockfish() != null) {
            try { stockfish = preparePlayer(request.stockfish(), account.username()); }
            catch (IllegalArgumentException e) { return new CreateGameResult(null, "Error: bad request"); }
            catch (IOException e) { return new CreateGameResult(null, "Error: Stockfish unavailable. Install Stockfish or set STOCKFISH_PATH."); }
        }
        synchronized (CREATE_LOCK) {
            int id = nextID.getAsInt();
            String white = null, black = null;
            if (stockfish != null) {
                white = stockfish.color() == ChessGame.TeamColor.WHITE ? StockfishPlayer.USERNAME : account.username();
                black = stockfish.color() == ChessGame.TeamColor.BLACK ? StockfishPlayer.USERNAME : account.username();
            }
            games.addGameData(new GameData(id, white, black, request.gameName(), new ChessGame(), stockfish));
            return new CreateGameResult(Integer.toString(id), "");
        }
    }

    /** A seated human can fill the opposite empty seat before the first move. */
    public JoinGameResult addStockfish(AddStockfishRequest request) {
        AuthData account = auth.findAuthDataByAuthToken(request.authToken());
        if (account == null) { return new JoinGameResult("Error: unauthorized"); }
        try {
            if (request.stockfish() == null) { return new JoinGameResult("Error: bad request"); }
            request.stockfish().validate();
            int id = Integer.parseInt(request.gameID());
            synchronized (GameLocks.forGame(id)) {
                GameData game = games.findGameDataByID(request.gameID());
                if (game == null) { return new JoinGameResult("Error: game does not exist"); }
                boolean aiWhite = request.stockfish().color() == ChessGame.TeamColor.WHITE;
                String human = aiWhite ? game.blackUsername() : game.whiteUsername();
                String seat = aiWhite ? game.whiteUsername() : game.blackUsername();
                if (!account.username().equals(human)) { return new JoinGameResult("Error: unauthorized"); }
                if (seat != null) { return new JoinGameResult("Error: already taken"); }
                if (game.stockfish() != null || !game.game().equals(new ChessGame())) { return new JoinGameResult("Error: bad request"); }
                StockfishPlayer player = preparePlayer(request.stockfish(), human);
                games.saveGame(new GameData(id, aiWhite ? StockfishPlayer.USERNAME : human,
                        aiWhite ? human : StockfishPlayer.USERNAME, game.gameName(), game.game(), player));
                return new JoinGameResult("");
            }
        } catch (IllegalArgumentException e) { return new JoinGameResult("Error: bad request"); }
        catch (IOException e) { return new JoinGameResult("Error: Stockfish unavailable. Install Stockfish or set STOCKFISH_PATH."); }
    }

    private StockfishPlayer preparePlayer(StockfishOptions options, String username) throws IOException {
        options.validate();
        if (StockfishPlayer.USERNAME.equalsIgnoreCase(username)) {
            throw new IllegalArgumentException("The Stockfish name is reserved for the engine");
        }
        UserData user = users.findUserDataByUsername(username);
        if (user == null) { throw new IllegalArgumentException("Player account missing"); }
        try (StockfishEngine engine = engines.open()) { return engine.player(options, user.elo()); }
    }

    public JoinGameResult joinGame(JoinGameRequest request) {
        AuthData account = auth.findAuthDataByAuthToken(request.authToken());
        if (account == null) { return new JoinGameResult("Error: unauthorized"); }
        if (!"WHITE".equals(request.playerColor()) && !"BLACK".equals(request.playerColor())) { return new JoinGameResult("Error: bad request"); }
        try {
            int id = Integer.parseInt(request.gameID());
            synchronized (GameLocks.forGame(id)) {
                GameData game = games.findGameDataByID(request.gameID());
                if (game == null) { return new JoinGameResult("Error: game does not exist"); }
                String seat = request.playerColor().equals("WHITE") ? game.whiteUsername() : game.blackUsername();
                if (seat != null) { return new JoinGameResult("Error: already taken"); }
                // AI matches keep their original human so results cannot be credited to a replacement account.
                if (game.stockfish() != null || game.game().isGameOver()) { return new JoinGameResult("Error: bad request"); }
                games.saveGame(GameData.updateGameDataUsers(request.playerColor(), account.username(), game));
                return new JoinGameResult("");
            }
        } catch (NumberFormatException e) { return new JoinGameResult("Error: bad request"); }
    }

    public void clearGameDataBase() { games.clear(); }
}
