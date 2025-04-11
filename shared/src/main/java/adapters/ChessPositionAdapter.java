package adapters;

import chess.ChessPosition;
import com.google.gson.*;

import java.lang.reflect.Type;

public class ChessPositionAdapter implements JsonSerializer<ChessPosition>, JsonDeserializer<ChessPosition> {

    @Override
    public JsonElement serialize(ChessPosition position, Type type, JsonSerializationContext jsonSerializationContext) {
        JsonObject obj = new JsonObject();
        obj.addProperty("row", position.getRow());
        obj.addProperty("col", position.getColumn());
        return obj;
    }


    @Override
    public ChessPosition deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext jsonDeserializationContext)
            throws JsonParseException {
        JsonObject obj = jsonElement.getAsJsonObject();
        int row = obj.get("row").getAsInt();
        // "col" not "column"
        int column = obj.get("col").getAsInt();
        return new ChessPosition(row, column);
    }

}
