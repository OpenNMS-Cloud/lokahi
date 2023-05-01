package org.opennms.horizon.notifications.exceptions;

public class NotificationAPIRetryableException extends NotificationAPIException {
    public NotificationAPIRetryableException(String message, Throwable cause) {
        super(message, cause);
    }
}
