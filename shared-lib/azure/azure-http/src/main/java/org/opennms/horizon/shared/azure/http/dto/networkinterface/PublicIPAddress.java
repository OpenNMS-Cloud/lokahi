package org.opennms.horizon.shared.azure.http.dto.networkinterface;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PublicIPAddress {
    @SerializedName("name")
    @Expose
    private String name;
    @SerializedName("id")
    @Expose
    private String id;
    @SerializedName("properties")
    @Expose
    private Properties__2 properties;
    @SerializedName("type")
    @Expose
    private String type;
    @SerializedName("sku")
    @Expose
    private Sku sku;
}
