package org.opennms.horizon.shared.azure.http.dto.networkinterface;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class IpConfigurationProps {
    @SerializedName("privateIPAddress")
    @Expose
    private String privateIPAddress;
    @SerializedName("publicIPAddress")
    @Expose
    private PublicIPAddress publicIPAddress;
}
