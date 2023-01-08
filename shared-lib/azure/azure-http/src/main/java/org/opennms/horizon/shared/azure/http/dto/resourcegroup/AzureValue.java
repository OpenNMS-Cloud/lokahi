package org.opennms.horizon.shared.azure.http.dto.resourcegroup;

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
    @SerializedName("location")
    private String location;
    @SerializedName("properties")
    private AzureProperties properties;
}
