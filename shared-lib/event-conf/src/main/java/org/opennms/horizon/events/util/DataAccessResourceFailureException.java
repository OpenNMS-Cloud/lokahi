package org.opennms.horizon.events.util;

public class DataAccessResourceFailureException extends DataAccessException {
    public DataAccessResourceFailureException(String message, Throwable t) {
        super(message,t);
    }

    public DataAccessResourceFailureException(String message) {
        super(message);
    }
}
