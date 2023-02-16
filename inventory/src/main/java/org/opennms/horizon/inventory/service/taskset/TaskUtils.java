package org.opennms.horizon.inventory.service.taskset;

public interface TaskUtils {

    String DEFAULT_SCHEDULE = "60000";

    int ICMP_DEFAULT_TIMEOUT_MS = 800;
    int ICMP_DEFAULT_RETRIES = 2;
    int ICMP_DEFAULT_DSCP = 0;
    int ICMP_DEFAULT_PACKET_SIZE = 64;
    boolean ICMP_DEFAULT_ALLOW_FRAGMENTATION = true;

    int SNMP_DEFAULT_TIMEOUT_MS = 18000;
    int SNMP_DEFAULT_RETRIES = 2;

    int AZURE_DEFAULT_TIMEOUT_MS = 18000;
    int AZURE_DEFAULT_RETRIES = 2;
    String AZURE_MONITOR_SCHEDULE = DEFAULT_SCHEDULE;
    String AZURE_COLLECTOR_SCHEDULE = DEFAULT_SCHEDULE;

    String IP_LABEL = "ip=";
    String AZURE_LABEL = "azure=";
    String NODE_SCAN ="nodeScan=node_id/";
    String NODE_ID = "nodeId:";
    String REQUISTION_NAME = "requisition:";

    static String identityForIpTask(long nodeId, String ipAddress, String name) {
        return NODE_ID  + nodeId + "/" + IP_LABEL + ipAddress + "/" + name;
    }

    static String identityForAzureTask(String name, String id) {
        return AZURE_LABEL + name + "-" + id;
    }

    static String identityForNodeScan(long nodeId) {
        return NODE_SCAN + nodeId;
    }

    static String identityForConfig(String configName, String location) {
        return configName + "@" + location;
    }

    static String identityForDiscoveryTask(String ipAddress, String location, String requisitionName) {
        return REQUISTION_NAME + requisitionName + "/"+ IP_LABEL + ipAddress + "/" + location;
    }
}
