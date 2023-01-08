package org.opennms.horizon.shared.azure.http.dto.resources;

import com.google.gson.annotations.SerializedName;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AzureTags {
    @SerializedName("ProjectName")
    private String projectName;
    @SerializedName("AssetTag")
    private String assetTag;
}
