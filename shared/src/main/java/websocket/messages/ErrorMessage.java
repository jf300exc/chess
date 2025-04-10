package websocket.messages;

import java.util.Objects;

public class ErrorMessage extends ServerMessage {
    String errorMessage;

    public ErrorMessage(ServerMessageType type, String errorMessage) {
        super(type);
        this.errorMessage = errorMessage;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }
        ErrorMessage that = (ErrorMessage) o;
        return Objects.equals(errorMessage, that.errorMessage);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), errorMessage);
    }
}
