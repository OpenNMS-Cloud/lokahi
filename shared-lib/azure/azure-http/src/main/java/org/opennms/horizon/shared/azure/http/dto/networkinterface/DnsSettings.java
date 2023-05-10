package org.opennms.horizon.shared.azure.http.dto.networkinterface;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class DnsSettings {
    @SerializedName("dnsServers")
    @Expose
    private List<Object> dnsServers = new ArrayList<>();
    @SerializedName("appliedDnsServers")
    @Expose
    private List<Object> appliedDnsServers = new ArrayList<>();
    @SerializedName("internalDomainNameSuffix")
    @Expose
    private String internalDomainNameSuffix;
}
