package websocket.messages;

import javax.management.Notification;
import java.util.Objects;

public class NotificationMessage extends ServerMessage {
    private final String notificationMessage;

    public NotificationMessage(ServerMessageType type) {
        super(type);
        this.notificationMessage = null;
    }

    public String getNotificationMessage() {
        return notificationMessage;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }
        NotificationMessage that = (NotificationMessage) o;
        return Objects.equals(notificationMessage, that.notificationMessage);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), notificationMessage);
    }
}
