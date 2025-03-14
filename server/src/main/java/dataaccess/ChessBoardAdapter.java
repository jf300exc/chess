package dataaccess;

import chess.ChessBoard;
import chess.ChessBoard.*;
import chess.ChessGame.*;
import chess.ChessPiece;
import chess.ChessPosition;
import com.google.gson.*;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.Map;

public class ChessBoardAdapter implements JsonDeserializer<ChessBoard>, JsonSerializer<ChessBoard> {
    @Override
    public JsonElement serialize(ChessBoard chessBoard, Type type, JsonSerializationContext jsonSerializationContext) {
        JsonObject obj = new JsonObject();

        JsonArray boardArray = new JsonArray();
        for (Map.Entry<ChessPosition, ChessPiece> entry : chessBoard.getBoardMap().entrySet()) {
            JsonObject boardEntry = new JsonObject();
            boardEntry.add("position", jsonSerializationContext.serialize(entry.getKey()));
            boardEntry.add("piece", jsonSerializationContext.serialize(entry.getValue()));
            boardArray.add(boardEntry);
        }
        obj.add("board", boardArray);

        obj.add("castleRequirements", jsonSerializationContext.serialize(chessBoard.getCastleRequirements(), new TypeToken<Map<TeamColor, Map<CastlePieceTypes, Map<CastleType, Boolean>>>>() {}.getType()));
        obj.add("whiteKingPos", jsonSerializationContext.serialize(chessBoard.getKingPos(TeamColor.WHITE)));
        obj.add("blackKingPos", jsonSerializationContext.serialize(chessBoard.getKingPos(TeamColor.BLACK)));
        obj.add("enPassantWhite", jsonSerializationContext.serialize(chessBoard.getEnPassant(TeamColor.WHITE)));
        obj.add("enPassantBlack", jsonSerializationContext.serialize(chessBoard.getEnPassant(TeamColor.BLACK)));

        return obj;
    }

    @Override
    public ChessBoard deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
        JsonObject obj = jsonElement.getAsJsonObject();
        ChessBoard chessBoard = new ChessBoard();

        JsonArray boardArray = obj.getAsJsonArray("board");
        for (JsonElement boardElement : boardArray) {
            JsonObject boardEntry = boardElement.getAsJsonObject();
            ChessPosition position = jsonDeserializationContext.deserialize(boardEntry.get("position"), ChessPosition.class);
            ChessPiece piece = jsonDeserializationContext.deserialize(boardEntry.get("piece"), ChessPiece.class);
            chessBoard.getBoardMap().put(position, piece);
        }

        Map<TeamColor, Map<CastlePieceTypes, Map<CastleType, Boolean>>> castleReqs =
                jsonDeserializationContext.deserialize(obj.get("castleRequirements"), new TypeToken<Map<TeamColor, Map<CastlePieceTypes, Map<CastleType, Boolean>>>>() {}.getType());
        chessBoard.setCastleRequirements(castleReqs);

        chessBoard.setKingPos(jsonDeserializationContext.deserialize(obj.get("whiteKingPos"), ChessPosition.class), TeamColor.WHITE);
        chessBoard.setKingPos(jsonDeserializationContext.deserialize(obj.get("blackKingPos"), ChessPosition.class), TeamColor.BLACK);
        chessBoard.setEnPassant(jsonDeserializationContext.deserialize(obj.get("enPassantWhite"), ChessPosition.class), TeamColor.BLACK);
        chessBoard.setEnPassant(jsonDeserializationContext.deserialize(obj.get("enPassantBlack"), ChessPosition.class), TeamColor.WHITE);

        return chessBoard;
    }
}
