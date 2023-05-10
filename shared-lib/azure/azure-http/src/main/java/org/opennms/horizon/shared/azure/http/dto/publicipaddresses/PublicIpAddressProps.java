package org.opennms.horizon.shared.azure.http.dto.publicipaddresses;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import org.opennms.horizon.shared.azure.http.dto.networkinterface.IpConfiguration;

import java.util.List;

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

    public String getProvisioningState() {
        return provisioningState;
    }

    public void setProvisioningState(String provisioningState) {
        this.provisioningState = provisioningState;
    }

    public String getResourceGuid() {
        return resourceGuid;
    }

    public void setResourceGuid(String resourceGuid) {
        this.resourceGuid = resourceGuid;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public String getPublicIPAddressVersion() {
        return publicIPAddressVersion;
    }

    public void setPublicIPAddressVersion(String publicIPAddressVersion) {
        this.publicIPAddressVersion = publicIPAddressVersion;
    }

    public String getPublicIPAllocationMethod() {
        return publicIPAllocationMethod;
    }

    public void setPublicIPAllocationMethod(String publicIPAllocationMethod) {
        this.publicIPAllocationMethod = publicIPAllocationMethod;
    }

    public Integer getIdleTimeoutInMinutes() {
        return idleTimeoutInMinutes;
    }

    public void setIdleTimeoutInMinutes(Integer idleTimeoutInMinutes) {
        this.idleTimeoutInMinutes = idleTimeoutInMinutes;
    }

    public List<Object> getIpTags() {
        return ipTags;
    }

    public void setIpTags(List<Object> ipTags) {
        this.ipTags = ipTags;
    }

    public IpConfiguration getIpConfiguration() {
        return ipConfiguration;
    }

    public void setIpConfiguration(IpConfiguration ipConfiguration) {
        this.ipConfiguration = ipConfiguration;
    }
}
