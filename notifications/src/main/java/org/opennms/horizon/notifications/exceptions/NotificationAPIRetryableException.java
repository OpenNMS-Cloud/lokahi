package org.opennms.horizon.notifications.exceptions;

import org.springframework.web.client.ResourceAccessException;

public class NotificationAPIRetryableException extends NotificationAPIException {
    public NotificationAPIRetryableException(String message, Throwable cause) {
        super(message, cause);
    }
}
