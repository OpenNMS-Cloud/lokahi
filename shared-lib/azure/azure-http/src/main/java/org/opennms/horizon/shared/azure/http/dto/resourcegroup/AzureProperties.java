package org.opennms.horizon.shared.azure.http.dto.resourcegroup;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AzureProperties {
    @SerializedName("provisioningState")
    @Expose
    private String provisioningState;
}
