package dataaccess;

import java.lang.reflect.Type;
import java.util.Map;

import chess.ChessBoard;
import chess.ChessBoard.CastleType;
import chess.ChessBoard.CastlePieceTypes;
import chess.ChessGame;
import chess.ChessGame.TeamColor;
import com.google.gson.*;

public class CastleRequirementsAdapter implements JsonSerializer<Map<TeamColor, Map<CastlePieceTypes, Map<CastleType, Boolean>>>>, JsonDeserializer<Map<TeamColor, Map<CastlePieceTypes, Map<CastleType, Boolean>>>>  {

    @Override
    public JsonElement serialize(Map<TeamColor, Map<CastlePieceTypes, Map<CastleType, Boolean>>> teamColorMapMap, Type type, JsonSerializationContext jsonSerializationContext) {
        JsonObject jsonObject = new JsonObject();



        return null;
    }

    @Override
    public Map<TeamColor, Map<CastlePieceTypes, Map<CastleType, Boolean>>> deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
        JsonObject jsonObject = jsonElement.getAsJsonObject();
        return Map.of();
    }
}
