package org.opennms.horizon.it.gqlmodels.querywrappers;

public class IpInterfaceResponse {
    private Integer id;
    private String tenantId;
    private Integer nodeId;
    private String ipAddress;
    private Boolean snmpPrimary;
    private String hostname;
    private String netmask;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public Integer getNodeId() {
        return nodeId;
    }

    public void setNodeId(Integer nodeId) {
        this.nodeId = nodeId;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public Boolean getSnmpPrimary() {
        return snmpPrimary;
    }

    public void setSnmpPrimary(Boolean snmpPrimary) {
        this.snmpPrimary = snmpPrimary;
    }

    public String getHostname() {
        return hostname;
    }

    public void setHostname(String hostname) {
        this.hostname = hostname;
    }

    public String getNetmask() {
        return netmask;
    }

    public void setNetmask(String netmask) {
        this.netmask = netmask;
    }
}
