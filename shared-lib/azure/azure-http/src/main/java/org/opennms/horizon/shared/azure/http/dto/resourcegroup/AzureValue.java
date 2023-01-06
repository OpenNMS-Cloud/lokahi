package org.opennms.horizon.shared.azure.http.dto.resourcegroup;

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
    @SerializedName("location")
    @Expose
    private String location;
    @SerializedName("properties")
    @Expose
    private AzureProperties properties;
}
