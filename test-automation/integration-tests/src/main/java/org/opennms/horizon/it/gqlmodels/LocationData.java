package org.opennms.horizon.it.gqlmodels;

public class LocationData {
    private Long id;
    private String location;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    @Override
    public String toString() {
        return "LocationData{" +
            "id='" + id + '\'' +
            ", location=" + location +
            '}';
    }
}
