package org.opennms.horizon.shared.azure.http.dto.resources;

import com.google.gson.annotations.SerializedName;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AzureValue {
    @SerializedName("id")
    private String id;
    @SerializedName("name")
    private String name;
    @SerializedName("type")
    private String type;
    @SerializedName("sku")
    private AzureSku sku;
    @SerializedName("managedBy")
    private String managedBy;
    @SerializedName("location")
    private String location;
    @SerializedName("tags")
    private AzureTags tags;
    @SerializedName("systemData")
    private AzureSystemData systemData;
}
