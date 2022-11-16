package org.opennms.horizon.inventory.service.taskset.identity;

import org.springframework.stereotype.Component;

@Component
public class TaskSetIdentityUtil {
    private static final String IP_LABEL = "ip=";
    private static final String PORT_LABEL = "port=";

    public String identityForIpTask(String ipAddress, String name) {
        return IP_LABEL + ipAddress + "/" + name;
    }

    public String identityForIpPortTask(String ipAddress, int port, String name) {
        return IP_LABEL + ipAddress + ";" + PORT_LABEL + port + "/" + name;
    }
}
