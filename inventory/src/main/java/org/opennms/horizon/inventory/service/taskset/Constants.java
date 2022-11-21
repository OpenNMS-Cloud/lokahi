package org.opennms.horizon.inventory.service.taskset;

public interface Constants {

    String DEFAULT_SCHEDULE = "60000";

    interface Icmp {
        int DEFAULT_TIMEOUT = 800;
        int DEFAULT_RETRIES = 2;
        int DEFAULT_DSCP = 0;
        int DEFAULT_PACKET_SIZE = 64;
        boolean DEFAULT_ALLOW_FRAGMENTATION = true;
    }

    interface Snmp {
        int DEFAULT_TIMEOUT = 18000;
        int DEFAULT_RETRIES = 2;
    }
}
