package org.opennms.horizon.it.gqlmodels.querywrappers;

import java.util.List;

public class CreateNodeResponseDetails {
    private Integer id;
    private String tenantId;
    private String nodeLabel;
    private String scanType;
    private String monitoredState;
    private Long createTime;
    private Integer monitoringLocationId;
    private String monitoringLocation;
    private List<IpInterfaceResponse> ipInterfaces;
    private List<SnmpInterfaceResponse> snmpInterfaces;
    private String objectId;
    private String systemName;
    private String systemDescr;
    private String systemLocation;
    private String systemContact;

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

    public String getNodeLabel() {
        return nodeLabel;
    }

    public void setNodeLabel(String nodeLabel) {
        this.nodeLabel = nodeLabel;
    }

    public String getScanType() {
        return scanType;
    }

    public void setScanType(String scanType) {
        this.scanType = scanType;
    }

    public String getMonitoredState() {
        return monitoredState;
    }

    public void setMonitoredState(String monitoredState) {
        this.monitoredState = monitoredState;
    }

    public Long getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Long createTime) {
        this.createTime = createTime;
    }

    public Integer getMonitoringLocationId() {
        return monitoringLocationId;
    }

    public void setMonitoringLocationId(Integer monitoringLocationId) {
        this.monitoringLocationId = monitoringLocationId;
    }

    public String getMonitoringLocation() {
        return monitoringLocation;
    }

    public void setMonitoringLocation(String monitoringLocation) {
        this.monitoringLocation = monitoringLocation;
    }

    public List<IpInterfaceResponse> getIpInterfaces() {
        return ipInterfaces;
    }

    public void setIpInterfaces(List<IpInterfaceResponse> ipInterfaces) {
        this.ipInterfaces = ipInterfaces;
    }

    public List<SnmpInterfaceResponse> getSnmpInterfaces() {
        return snmpInterfaces;
    }

    public void setSnmpInterfaces(List<SnmpInterfaceResponse> snmpInterfaces) {
        this.snmpInterfaces = snmpInterfaces;
    }

    public String getObjectId() {
        return objectId;
    }

    public void setObjectId(String objectId) {
        this.objectId = objectId;
    }

    public String getSystemName() {
        return systemName;
    }

    public void setSystemName(String systemName) {
        this.systemName = systemName;
    }

    public String getSystemDescr() {
        return systemDescr;
    }

    public void setSystemDescr(String systemDescr) {
        this.systemDescr = systemDescr;
    }

    public String getSystemLocation() {
        return systemLocation;
    }

    public void setSystemLocation(String systemLocation) {
        this.systemLocation = systemLocation;
    }

    public String getSystemContact() {
        return systemContact;
    }

    public void setSystemContact(String systemContact) {
        this.systemContact = systemContact;
    }
}
