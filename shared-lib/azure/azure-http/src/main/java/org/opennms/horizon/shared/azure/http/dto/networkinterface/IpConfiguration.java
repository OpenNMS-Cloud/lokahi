package org.opennms.horizon.shared.azure.http.dto.networkinterface;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class IpConfiguration {
    @SerializedName("name")
    @Expose
    private String name;
    @SerializedName("id")
    @Expose
    private String id;
    @SerializedName("etag")
    @Expose
    private String etag;
    @SerializedName("type")
    @Expose
    private String type;
    @SerializedName("properties")
    @Expose
    private IpConfigurationProps properties;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getEtag() {
        return etag;
    }

    public void setEtag(String etag) {
        this.etag = etag;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public IpConfigurationProps getProperties() {
        return properties;
    }

    public void setProperties(IpConfigurationProps properties) {
        this.properties = properties;
    }
}
