package org.opennms.horizon.shared.azure.http.dto.publicipaddresses;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import lombok.Getter;
import lombok.Setter;
import org.opennms.horizon.shared.azure.http.dto.networkinterface.IpConfiguration;

import java.util.List;

@Getter
@Setter
public class PublicIpAddressProps {
    @SerializedName("provisioningState")
    @Expose
    private String provisioningState;
    @SerializedName("resourceGuid")
    @Expose
    private String resourceGuid;
    @SerializedName("ipAddress")
    @Expose
    private String ipAddress;
    @SerializedName("publicIPAddressVersion")
    @Expose
    private String publicIPAddressVersion;
    @SerializedName("publicIPAllocationMethod")
    @Expose
    private String publicIPAllocationMethod;
    @SerializedName("idleTimeoutInMinutes")
    @Expose
    private Integer idleTimeoutInMinutes;
    @SerializedName("ipTags")
    @Expose
    private List<Object> ipTags;
    @SerializedName("ipConfiguration")
    @Expose
    private IpConfiguration ipConfiguration;
}
