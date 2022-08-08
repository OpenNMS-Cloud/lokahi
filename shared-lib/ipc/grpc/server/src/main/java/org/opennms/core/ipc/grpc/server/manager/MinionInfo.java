package org.opennms.core.ipc.grpc.server.manager;

import java.util.Objects;

// TODO: move to a more appropriate API package
public class MinionInfo {
    private String id;
    private String location;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MinionInfo that = (MinionInfo) o;
        return Objects.equals(id, that.id) &&
                Objects.equals(location, that.location);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, location);
    }
}
