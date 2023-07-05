package org.opennms.horizon.it.gqlmodels;

public class MinionData {
    private long id;
    private String systemId;
    private LocationData location;
    private String label;
    private String status;

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getSystemId() {
        return systemId;
    }

    public void setSystemId(String systemId) {
        this.systemId = systemId;
    }

    public LocationData getLocation() {
        return location;
    }

    public void setLocation(LocationData location) {
        this.location = location;
    }


    @Override
    public String toString() {
        return "MinionData{" +
            "id='" + id + '\'' +
            ", label=" + label +
            ", status=" + status +
            ", systemId=" + systemId +
            ", location=" + location +
            '}';
    }
}
