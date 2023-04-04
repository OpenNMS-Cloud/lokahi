package org.opennms.horizon.it.gqlmodels.querywrappers;

public class SnmpInterfaceResponse {
    private long id;
    private String tenantId;
    private long nodeId;
    private String ipAddress;
    private int ifIndex;
    private String ifDescr;
    private int ifType;
    private String ifName;
    private long ifSpeed;
    private int ifAdminStatus;
    private int ifOperatorStatus;
    private String ifAlias;
    private String physicalAddr;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public long getNodeId() {
        return nodeId;
    }

    public void setNodeId(long nodeId) {
        this.nodeId = nodeId;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public int getIfIndex() {
        return ifIndex;
    }

    public void setIfIndex(int ifIndex) {
        this.ifIndex = ifIndex;
    }

    public String getIfDescr() {
        return ifDescr;
    }

    public void setIfDescr(String ifDescr) {
        this.ifDescr = ifDescr;
    }

    public int getIfType() {
        return ifType;
    }

    public void setIfType(int ifType) {
        this.ifType = ifType;
    }

    public String getIfName() {
        return ifName;
    }

    public void setIfName(String ifName) {
        this.ifName = ifName;
    }

    public long getIfSpeed() {
        return ifSpeed;
    }

    public void setIfSpeed(long ifSpeed) {
        this.ifSpeed = ifSpeed;
    }

    public int getIfAdminStatus() {
        return ifAdminStatus;
    }

    public void setIfAdminStatus(int ifAdminStatus) {
        this.ifAdminStatus = ifAdminStatus;
    }

    public int getIfOperatorStatus() {
        return ifOperatorStatus;
    }

    public void setIfOperatorStatus(int ifOperatorStatus) {
        this.ifOperatorStatus = ifOperatorStatus;
    }

    public String getIfAlias() {
        return ifAlias;
    }

    public void setIfAlias(String ifAlias) {
        this.ifAlias = ifAlias;
    }

    public String getPhysicalAddr() {
        return physicalAddr;
    }

    public void setPhysicalAddr(String physicalAddr) {
        this.physicalAddr = physicalAddr;
    }
}
