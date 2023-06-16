package org.opennms.horizon.it.gqlmodels;

import java.util.List;
import java.util.Map;

public class ErrorData {
    private String message;
    private List<ErrorLocationData> locations;
    private List<String> path;
    private Map extensions;

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public List<ErrorLocationData> getLocations() {
        return locations;
    }

    public void setLocations(List<ErrorLocationData> locations) {
        this.locations = locations;
    }

    public List<String> getPath() {
        return path;
    }

    public void setPath(List<String> path) {
        this.path = path;
    }

    public Map getExtensions() {
        return extensions;
    }

    public void setExtensions(Map extensions) {
        this.extensions = extensions;
    }

    @Override
    public String toString() {
        return "ErrorData{" +
            "message='" + message + '\'' +
            ", locations=" + locations +
            ", path=" + path +
            ", extensions=" + extensions +
            '}';
    }
}
