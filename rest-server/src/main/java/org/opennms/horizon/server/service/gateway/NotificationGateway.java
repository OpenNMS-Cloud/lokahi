package org.opennms.horizon.server.service.gateway;

import org.opennms.horizon.server.service.gateway.GatewayBase;

public class NotificationGateway extends GatewayBase {
    public static final String URL_PATH_NOTIFICATIONS_CONFIG = "/notifications/config";

    public NotificationGateway(String baseUrl) {
        super(baseUrl);
    }
}
