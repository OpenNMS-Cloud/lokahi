package org.opennms.horizon.core.monitor.taskset;

public class MonitorTaskSetIdentityUtil {

    public static final String IP_LABEL = "ip=";
    public static final String PORT_LABEL = "port=";

    public String identityForIpTask(String ipAddress, String name) {
        return IP_LABEL + ipAddress + "/" + name;
    }

    public String identityForIpPortTask(String ipAddress, int port, String name) {
        return IP_LABEL + ipAddress + ";" + PORT_LABEL + port + "/" + name;
    }
}
