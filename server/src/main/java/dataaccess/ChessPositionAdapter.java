package dataaccess;

import chess.ChessPosition;
import com.google.gson.*;

import java.lang.reflect.Type;

public class ChessPositionAdapter implements JsonSerializer<ChessPosition>, JsonDeserializer<ChessPosition> {

    @Override
    public JsonElement serialize(ChessPosition position, Type type, JsonSerializationContext jsonSerializationContext) {
        JsonObject obj = new JsonObject();
        obj.addProperty("row", position.getRow());
        obj.addProperty("column", position.getColumn());
        return obj;
    }


    @Override
    public ChessPosition deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
        JsonObject obj = jsonElement.getAsJsonObject();
        int row = obj.get("row").getAsInt();
        int column = obj.get("column").getAsInt();
        return new ChessPosition(row, column);
    }

}
