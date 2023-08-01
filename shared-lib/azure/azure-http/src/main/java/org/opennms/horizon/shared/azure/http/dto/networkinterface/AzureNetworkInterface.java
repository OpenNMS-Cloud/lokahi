package org.opennms.horizon.shared.azure.http.dto.networkinterface;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AzureNetworkInterface extends AzureBaseModel {
    @SerializedName("properties")
    @Expose
    private NetworkInterfaceProps properties;

    @SerializedName("location")
    @Expose
    private String location;
}
