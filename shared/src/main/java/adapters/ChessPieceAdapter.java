package adapters;

import chess.ChessPiece;
import chess.ChessPiece.PieceType;
import chess.ChessGame.TeamColor;
import com.google.gson.*;

import java.lang.reflect.Type;

public class ChessPieceAdapter implements JsonSerializer<ChessPiece>, JsonDeserializer<ChessPiece> {
    @Override
    public JsonElement serialize(ChessPiece piece, Type type, JsonSerializationContext jsonSerializationContext) {
        JsonObject obj = new JsonObject();
        obj.addProperty("pieceColor", piece.getTeamColor().name());
        obj.addProperty("type", piece.getPieceType().name());
        return obj;
    }

    @Override
    public ChessPiece deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext jsonDeserializationContext)
            throws JsonParseException {
        JsonObject obj = jsonElement.getAsJsonObject();

        TeamColor teamColor = TeamColor.valueOf(obj.get("pieceColor").getAsString());
        PieceType pieceType = PieceType.valueOf(obj.get("type").getAsString());

        return new ChessPiece(teamColor, pieceType);
    }
}
