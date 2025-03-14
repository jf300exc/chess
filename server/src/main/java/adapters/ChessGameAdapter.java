package adapters;

import chess.ChessBoard;
import chess.ChessGame;
import com.google.gson.*;

import java.lang.reflect.Type;

public class ChessGameAdapter implements JsonSerializer<ChessGame>, JsonDeserializer<ChessGame> {
    @Override
    public JsonElement serialize(ChessGame chessGame, Type type, JsonSerializationContext jsonSerializationContext) {
        JsonObject jsonObject = new JsonObject();

        jsonObject.add("gameBoard", jsonSerializationContext.serialize(chessGame.getBoard()));

        jsonObject.add("teamTurn", jsonSerializationContext.serialize(chessGame.getTeamTurn()));
        return jsonObject;
    }

    @Override
    public ChessGame deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext jsonDeserializationContext)
            throws JsonParseException {
        JsonObject jsonObject = jsonElement.getAsJsonObject();

        ChessBoard gameBoard = jsonDeserializationContext.deserialize(jsonObject.get("gameBoard"), ChessBoard.class);

        ChessGame.TeamColor teamTurn = jsonDeserializationContext.deserialize(jsonObject.get("teamTurn"), ChessGame.TeamColor.class);

        ChessGame chessGame = new ChessGame();
        chessGame.setBoard(gameBoard);
        chessGame.setTeamTurn(teamTurn);

        return chessGame;
    }
}
