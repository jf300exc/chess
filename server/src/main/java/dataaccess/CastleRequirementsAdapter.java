package dataaccess;

import java.lang.reflect.Type;
import java.util.EnumMap;
import java.util.Map;

import com.google.gson.*;

import chess.ChessBoard.CastleType;
import chess.ChessBoard.CastlePieceTypes;
import chess.ChessGame.TeamColor;

public class CastleRequirementsAdapter implements JsonSerializer<Map<TeamColor, Map<CastlePieceTypes, Map<CastleType, Boolean>>>>, JsonDeserializer<Map<TeamColor, Map<CastlePieceTypes, Map<CastleType, Boolean>>>>  {

    @Override
    public JsonElement serialize(Map<TeamColor, Map<CastlePieceTypes, Map<CastleType, Boolean>>> teamColorMapMap, Type type, JsonSerializationContext jsonSerializationContext) {
        JsonObject jsonObject = new JsonObject();

        for (Map.Entry<TeamColor, Map<CastlePieceTypes, Map<CastleType, Boolean>>> entry : teamColorMapMap.entrySet()) {
            JsonObject innerJson = new JsonObject();

            for (Map.Entry<CastlePieceTypes, Map<CastleType, Boolean>> innerEntry : entry.getValue().entrySet()) {
                JsonObject castleTypeMap = new JsonObject();

                for (Map.Entry<CastleType, Boolean> castleEntry : innerEntry.getValue().entrySet()) {
                    castleTypeMap.addProperty(castleEntry.getKey().name(), castleEntry.getValue());
                }
                innerJson.add(entry.getKey().name(), castleTypeMap);
            }
            jsonObject.add(entry.getKey().name(), innerJson);
        }

        return jsonObject;
    }

    @Override
    public Map<TeamColor, Map<CastlePieceTypes, Map<CastleType, Boolean>>> deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
        JsonObject jsonObject = jsonElement.getAsJsonObject();
        Map<TeamColor, Map<CastlePieceTypes, Map<CastleType, Boolean>>> castleRequirementsMap = new EnumMap<>(TeamColor.class);

        for (Map.Entry<String, JsonElement> entry : jsonObject.entrySet()) {
            TeamColor teamColor = TeamColor.valueOf(entry.getKey());
            JsonObject innerJson = entry.getValue().getAsJsonObject();

            Map<CastlePieceTypes, Map<CastleType, Boolean>> pieceTypeMap = new EnumMap<>(CastlePieceTypes.class);

            for (Map.Entry<String, JsonElement> pieceTypeEntry : innerJson.entrySet()) {
                CastlePieceTypes pieceTypes = CastlePieceTypes.valueOf(pieceTypeEntry.getKey());
                JsonObject castleTypeMap = pieceTypeEntry.getValue().getAsJsonObject();

                Map<CastleType, Boolean> castleTypeBooleanMap = new EnumMap<>(CastleType.class);

                for (Map.Entry<String, JsonElement> castleEntry : castleTypeMap.entrySet()) {
                    CastleType castleType = CastleType.valueOf(castleEntry.getKey());
                    Boolean value = castleEntry.getValue().getAsBoolean();
                    castleTypeBooleanMap.put(castleType, value);
                }

                pieceTypeMap.put(pieceTypes, castleTypeBooleanMap);
            }

            castleRequirementsMap.put(teamColor, pieceTypeMap);
        }

        return castleRequirementsMap;
    }
}
