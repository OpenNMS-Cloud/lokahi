package org.opennms.horizon.shared.azure.http.dto.resourcegroup;

import com.google.gson.annotations.SerializedName;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AzureProperties {
    @SerializedName("provisioningState")
    private String provisioningState;
}
