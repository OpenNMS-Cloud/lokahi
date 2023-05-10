package org.opennms.horizon.shared.azure.http.dto.networkinterface;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

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

    public String getProvisioningState() {
        return provisioningState;
    }

    public void setProvisioningState(String provisioningState) {
        this.provisioningState = provisioningState;
    }

    public String getPrivateIPAddress() {
        return privateIPAddress;
    }

    public void setPrivateIPAddress(String privateIPAddress) {
        this.privateIPAddress = privateIPAddress;
    }

    public String getPrivateIPAllocationMethod() {
        return privateIPAllocationMethod;
    }

    public void setPrivateIPAllocationMethod(String privateIPAllocationMethod) {
        this.privateIPAllocationMethod = privateIPAllocationMethod;
    }

    public PublicIPAddress getPublicIPAddress() {
        return publicIPAddress;
    }

    public void setPublicIPAddress(PublicIPAddress publicIPAddress) {
        this.publicIPAddress = publicIPAddress;
    }

    public Subnet getSubnet() {
        return subnet;
    }

    public void setSubnet(Subnet subnet) {
        this.subnet = subnet;
    }

    public Boolean getPrimary() {
        return primary;
    }

    public void setPrimary(Boolean primary) {
        this.primary = primary;
    }

    public String getPrivateIPAddressVersion() {
        return privateIPAddressVersion;
    }

    public void setPrivateIPAddressVersion(String privateIPAddressVersion) {
        this.privateIPAddressVersion = privateIPAddressVersion;
    }
}
