package org.opennms.horizon.shared.azure.http.dto.networkinterface;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
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
    private List<Object> hostedWorkloads  = new ArrayList<>();
    @SerializedName("tapConfigurations")
    @Expose
    private List<Object> tapConfigurations = new ArrayList<>();
    @SerializedName("nicType")
    @Expose
    private String nicType;
    @SerializedName("allowPort25Out")
    @Expose
    private Boolean allowPort25Out;
}
