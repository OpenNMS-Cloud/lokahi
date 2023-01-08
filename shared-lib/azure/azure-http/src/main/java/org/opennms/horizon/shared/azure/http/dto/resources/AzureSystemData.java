package org.opennms.horizon.shared.azure.http.dto.resources;

import com.google.gson.annotations.SerializedName;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AzureSystemData {
    @SerializedName("createdBy")
    private String createdBy;
    @SerializedName("createdByType")
    private String createdByType;
    @SerializedName("createdAt")
    private String createdAt;
    @SerializedName("lastModifiedBy")
    private String lastModifiedBy;
    @SerializedName("lastModifiedByType")
    private String lastModifiedByType;
    @SerializedName("lastModifiedAt")
    private String lastModifiedAt;
}
