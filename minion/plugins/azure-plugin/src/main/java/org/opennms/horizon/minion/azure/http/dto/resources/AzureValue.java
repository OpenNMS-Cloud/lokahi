package org.opennms.horizon.minion.azure.http.dto.resources;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AzureValue {
    @SerializedName("id")
    @Expose
    private String id;
    @SerializedName("name")
    @Expose
    private String name;
    @SerializedName("type")
    @Expose
    private String type;
    @SerializedName("sku")
    @Expose
    private AzureSku sku;
    @SerializedName("managedBy")
    @Expose
    private String managedBy;
    @SerializedName("location")
    @Expose
    private String location;
    @SerializedName("tags")
    @Expose
    private AzureTags tags;
    @SerializedName("systemData")
    @Expose
    private AzureSystemData systemData;
}
