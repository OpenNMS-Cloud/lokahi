package org.opennms.horizon.server.service.gateway;

public class PlatformGateway extends GatewayBase {
    public static final String URL_PATH_EVENTS = "/events";
    public static final String URL_PATH_NOTIFICATIONS_CONFIG = "/notifications/config";
    public static final String URL_PATH_ALARMS = "/alarms";
    public static final String URL_PATH_DEVICES = "/devices";
    public static final String URL_PATH_LOCATIONS = "/locations";
    public static final String URL_PATH_ALARMS_LIST = URL_PATH_ALARMS + "/list";
    public static final String URL_PATH_ALARMS_ACK = URL_PATH_ALARMS + "/%d/ack";
    public static final String URL_PATH_ALARMS_CLEAR = URL_PATH_ALARMS + "/%d/clear";
    public static final String URL_PATH_MINIONS = "/minions";
    public static final String URL_PATH_MINIONS_ID = "/minions/%s";
    public static final String URL_PATH_DATA_CHOICES = "/datachoices";
    public static final String QUERY_PARAM_TOGGLE = "toggle";

    public PlatformGateway(String baseUrl) {
        super(baseUrl);
    }
}
