package dataaccess;

import java.lang.reflect.Type;
import java.util.EnumMap;
import java.util.Map;

import com.google.gson.*;

import chess.ChessBoard.CastleType;
import chess.ChessBoard.CastlePieceTypes;
import chess.ChessGame.TeamColor;

public class CastleRequirementsAdapter implements JsonSerializer<Map<TeamColor, Map<CastlePieceTypes, Map<CastleType, Boolean>>>>,
        JsonDeserializer<Map<TeamColor, Map<CastlePieceTypes, Map<CastleType, Boolean>>>>  {

    @Override
    public JsonElement serialize(Map<TeamColor, Map<CastlePieceTypes, Map<CastleType, Boolean>>>
                                             teamColorMapMap, Type type, JsonSerializationContext jsonSerializationContext) {
        JsonObject jsonObject = new JsonObject();

        for (Map.Entry<TeamColor, Map<CastlePieceTypes, Map<CastleType, Boolean>>> teamEntry : teamColorMapMap.entrySet()) {
            JsonObject teamJson = new JsonObject();

            for (Map.Entry<CastlePieceTypes, Map<CastleType, Boolean>> pieceEntry : teamEntry.getValue().entrySet()) {
                JsonObject castleTypeJson = new JsonObject();

                for (Map.Entry<CastleType, Boolean> castleEntry : pieceEntry.getValue().entrySet()) {
                    castleTypeJson.addProperty(castleEntry.getKey().name(), castleEntry.getValue());
                }
                teamJson.add(pieceEntry.getKey().name(), castleTypeJson);
            }
            jsonObject.add(teamEntry.getKey().name(), teamJson);
        }

        return jsonObject;
    }

    @Override
    public Map<TeamColor, Map<CastlePieceTypes, Map<CastleType, Boolean>>>
            deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext jsonDeserializationContext)
            throws JsonParseException {
        JsonObject jsonObject = jsonElement.getAsJsonObject();
        Map<TeamColor, Map<CastlePieceTypes, Map<CastleType, Boolean>>> castleRequirementsMap = new EnumMap<>(TeamColor.class);

        for (Map.Entry<String, JsonElement> teamEntry : jsonObject.entrySet()) {
            TeamColor teamColor = TeamColor.valueOf(teamEntry.getKey());
            JsonObject teamJson = teamEntry.getValue().getAsJsonObject();

            Map<CastlePieceTypes, Map<CastleType, Boolean>> pieceTypeMap = new EnumMap<>(CastlePieceTypes.class);

            for (Map.Entry<String, JsonElement> pieceEntry : teamJson.entrySet()) {
                CastlePieceTypes pieceTypes = CastlePieceTypes.valueOf(pieceEntry.getKey());
                JsonObject castleTypeMap = pieceEntry.getValue().getAsJsonObject();

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
