package org.opennms.horizon.shared.azure.http.dto.networkinterface;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class IpConfigurationProps {
    @SerializedName("provisioningState")
    @Expose
    private String provisioningState;
    @SerializedName("privateIPAddress")
    @Expose
    private String privateIPAddress;
    @SerializedName("privateIPAllocationMethod")
    @Expose
    private String privateIPAllocationMethod;
    @SerializedName("publicIPAddress")
    @Expose
    private PublicIPAddress publicIPAddress;
    @SerializedName("subnet")
    @Expose
    private Subnet subnet;
    @SerializedName("primary")
    @Expose
    private Boolean primary;
    @SerializedName("privateIPAddressVersion")
    @Expose
    private String privateIPAddressVersion;
}
