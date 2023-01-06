package org.opennms.horizon.shared.azure.http;

public class AzureHttpException extends Exception {

    public AzureHttpException(String message) {
        super(message);
    }

    public AzureHttpException(String message, Throwable t) {
        super(message, t);
    }
}
