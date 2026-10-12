package peerflow;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Notification {
    private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    // Instance variables
    private String message;
    private LocalDateTime timestamp;
    private boolean delivered;

    // Constructor
    public Notification(String message) {
        this.message = message;
        this.timestamp = LocalDateTime.now();
        this.delivered = false;
    }

    // Methods
    public void notifyUser(User recipient) {
        if (recipient == null) {
            System.out.println("  [!] Notification has no recipient; not sent.");
            return;
        }
        this.delivered = true;
        recipient.receive(this);
        System.out.println("  [NOTIFY -> " + recipient.getUserName()
                + " (" + recipient.getUserRole() + ")] " + message);
    }

    public String getMessage() {
        return message;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public boolean isDelivered() {
        return delivered;
    }

    @Override
    public String toString() {
        return "[" + timestamp.format(STAMP) + "] " + message;
    }
}
