package org.opennms.horizon.it.gqlmodels.querywrappers;

public class CreateNodeResponseData {
    private long createTime;
    private long id;
    private long monitoringLocationId;
    private String nodeLabel;

    public long getCreateTime() {
        return createTime;
    }

    public void setCreateTime(long createTime) {
        this.createTime = createTime;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getMonitoringLocationId() {
        return monitoringLocationId;
    }

    public void setMonitoringLocationId(long monitoringLocationId) {
        this.monitoringLocationId = monitoringLocationId;
    }

    public String getNodeLabel() {
        return nodeLabel;
    }

    public void setNodeLabel(String nodeLabel) {
        this.nodeLabel = nodeLabel;
    }
}
