package org.opennms.horizon.shared.azure.http.dto.resources;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AzureSystemData {
    @SerializedName("createdBy")
    @Expose
    private String createdBy;
    @SerializedName("createdByType")
    @Expose
    private String createdByType;
    @SerializedName("createdAt")
    @Expose
    private String createdAt;
    @SerializedName("lastModifiedBy")
    @Expose
    private String lastModifiedBy;
    @SerializedName("lastModifiedByType")
    @Expose
    private String lastModifiedByType;
    @SerializedName("lastModifiedAt")
    @Expose
    private String lastModifiedAt;
}
