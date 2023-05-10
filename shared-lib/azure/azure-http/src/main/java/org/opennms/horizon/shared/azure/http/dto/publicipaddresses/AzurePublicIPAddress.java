package org.opennms.horizon.shared.azure.http.dto.publicipaddresses;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class AzurePublicIPAddress {
    @SerializedName("name")
    @Expose
    private String name;
    @SerializedName("id")
    @Expose
    private String id;
    @SerializedName("etag")
    @Expose
    private String etag;
    @SerializedName("location")
    @Expose
    private String location;
    @SerializedName("properties")
    @Expose
    private PublicIpAddressProps properties;
    @SerializedName("type")
    @Expose
    private String type;
    @SerializedName("sku")
    @Expose
    private Sku sku;
    @SerializedName("zones")
    @Expose
    private List<String> zones;
    @SerializedName("tags")
    @Expose
    private Tags tags;
}
