package org.opennms.horizon.shared.azure.http.dto.networkinterface;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.util.List;

public class Properties__2 {
    @SerializedName("provisioningState")
    @Expose
    private String provisioningState;
    @SerializedName("resourceGuid")
    @Expose
    private String resourceGuid;
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
    private IpConfiguration__1 ipConfiguration;
    @SerializedName("deleteOption")
    @Expose
    private String deleteOption;

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

    public IpConfiguration__1 getIpConfiguration() {
        return ipConfiguration;
    }

    public void setIpConfiguration(IpConfiguration__1 ipConfiguration) {
        this.ipConfiguration = ipConfiguration;
    }

    public String getDeleteOption() {
        return deleteOption;
    }

    public void setDeleteOption(String deleteOption) {
        this.deleteOption = deleteOption;
    }
}
