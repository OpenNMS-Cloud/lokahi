package org.opennms.horizon.shared.azure.http.dto.networkinterface;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.util.List;

public class NetworkInterfaceProps {
    @SerializedName("provisioningState")
    @Expose
    private String provisioningState;
    @SerializedName("resourceGuid")
    @Expose
    private String resourceGuid;
    @SerializedName("ipConfigurations")
    @Expose
    private List<IpConfiguration> ipConfigurations;
    @SerializedName("dnsSettings")
    @Expose
    private DnsSettings dnsSettings;
    @SerializedName("macAddress")
    @Expose
    private String macAddress;
    @SerializedName("enableAcceleratedNetworking")
    @Expose
    private Boolean enableAcceleratedNetworking;
    @SerializedName("vnetEncryptionSupported")
    @Expose
    private Boolean vnetEncryptionSupported;
    @SerializedName("enableIPForwarding")
    @Expose
    private Boolean enableIPForwarding;
    @SerializedName("networkSecurityGroup")
    @Expose
    private NetworkSecurityGroup networkSecurityGroup;
    @SerializedName("primary")
    @Expose
    private Boolean primary;
    @SerializedName("virtualMachine")
    @Expose
    private VirtualMachine virtualMachine;
    @SerializedName("hostedWorkloads")
    @Expose
    private List<Object> hostedWorkloads;
    @SerializedName("tapConfigurations")
    @Expose
    private List<Object> tapConfigurations;
    @SerializedName("nicType")
    @Expose
    private String nicType;
    @SerializedName("allowPort25Out")
    @Expose
    private Boolean allowPort25Out;

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

    public List<IpConfiguration> getIpConfigurations() {
        return ipConfigurations;
    }

    public void setIpConfigurations(List<IpConfiguration> ipConfigurations) {
        this.ipConfigurations = ipConfigurations;
    }

    public DnsSettings getDnsSettings() {
        return dnsSettings;
    }

    public void setDnsSettings(DnsSettings dnsSettings) {
        this.dnsSettings = dnsSettings;
    }

    public String getMacAddress() {
        return macAddress;
    }

    public void setMacAddress(String macAddress) {
        this.macAddress = macAddress;
    }

    public Boolean getEnableAcceleratedNetworking() {
        return enableAcceleratedNetworking;
    }

    public void setEnableAcceleratedNetworking(Boolean enableAcceleratedNetworking) {
        this.enableAcceleratedNetworking = enableAcceleratedNetworking;
    }

    public Boolean getVnetEncryptionSupported() {
        return vnetEncryptionSupported;
    }

    public void setVnetEncryptionSupported(Boolean vnetEncryptionSupported) {
        this.vnetEncryptionSupported = vnetEncryptionSupported;
    }

    public Boolean getEnableIPForwarding() {
        return enableIPForwarding;
    }

    public void setEnableIPForwarding(Boolean enableIPForwarding) {
        this.enableIPForwarding = enableIPForwarding;
    }

    public NetworkSecurityGroup getNetworkSecurityGroup() {
        return networkSecurityGroup;
    }

    public void setNetworkSecurityGroup(NetworkSecurityGroup networkSecurityGroup) {
        this.networkSecurityGroup = networkSecurityGroup;
    }

    public Boolean getPrimary() {
        return primary;
    }

    public void setPrimary(Boolean primary) {
        this.primary = primary;
    }

    public VirtualMachine getVirtualMachine() {
        return virtualMachine;
    }

    public void setVirtualMachine(VirtualMachine virtualMachine) {
        this.virtualMachine = virtualMachine;
    }

    public List<Object> getHostedWorkloads() {
        return hostedWorkloads;
    }

    public void setHostedWorkloads(List<Object> hostedWorkloads) {
        this.hostedWorkloads = hostedWorkloads;
    }

    public List<Object> getTapConfigurations() {
        return tapConfigurations;
    }

    public void setTapConfigurations(List<Object> tapConfigurations) {
        this.tapConfigurations = tapConfigurations;
    }

    public String getNicType() {
        return nicType;
    }

    public void setNicType(String nicType) {
        this.nicType = nicType;
    }

    public Boolean getAllowPort25Out() {
        return allowPort25Out;
    }

    public void setAllowPort25Out(Boolean allowPort25Out) {
        this.allowPort25Out = allowPort25Out;
    }
}
