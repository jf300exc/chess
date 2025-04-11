package adapters;

import com.google.gson.*;
import websocket.messages.LoadGameMessage;

import java.lang.reflect.Type;

public class LoadGameMessageAdapter  implements JsonSerializer<LoadGameMessage>, JsonDeserializer<LoadGameMessage> {

    @Override
    public LoadGameMessage deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
        return null;
    }

    @Override
    public JsonElement serialize(LoadGameMessage loadGameMessage, Type type, JsonSerializationContext jsonSerializationContext) {
        JsonObject obj = new JsonObject();
        obj.addProperty("serverMessageType", loadGameMessage.getServerMessageType().name());
        obj.add("game", jsonSerializationContext.serialize(loadGameMessage.getGame()));
        return null;
    }
}
