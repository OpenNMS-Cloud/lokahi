package org.opennms.horizon.minion.azure.http.dto.resources;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AzureTags {
    @SerializedName("ProjectName")
    @Expose
    private String projectName;
    @SerializedName("AssetTag")
    @Expose
    private String assetTag;
}
