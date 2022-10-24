package org.opennms.horizon.notifications.exceptions;

public abstract class NotificationException extends Exception {
    public NotificationException() {
    }

    public NotificationException(String message) {
        super(message);
    }

    public NotificationException(String message, Throwable cause) {
        super(message, cause);
    }

    public NotificationException(Throwable cause) {
        super(cause);
    }
}
