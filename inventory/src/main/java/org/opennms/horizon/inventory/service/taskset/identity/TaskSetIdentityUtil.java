package org.opennms.horizon.inventory.service.taskset.identity;

import org.springframework.stereotype.Component;

@Component
public class TaskSetIdentityUtil {
    private static final String IP_LABEL = "ip=";

    public String identityForIpTask(String ipAddress, String name) {
        return IP_LABEL + ipAddress + "/" + name;
    }
}
