package org.opennms.horizon.shared.azure.http.dto.networkinterface;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.util.List;

public class DnsSettings {
    @SerializedName("dnsServers")
    @Expose
    private List<Object> dnsServers;
    @SerializedName("appliedDnsServers")
    @Expose
    private List<Object> appliedDnsServers;
    @SerializedName("internalDomainNameSuffix")
    @Expose
    private String internalDomainNameSuffix;

    public List<Object> getDnsServers() {
        return dnsServers;
    }

    public void setDnsServers(List<Object> dnsServers) {
        this.dnsServers = dnsServers;
    }

    public List<Object> getAppliedDnsServers() {
        return appliedDnsServers;
    }

    public void setAppliedDnsServers(List<Object> appliedDnsServers) {
        this.appliedDnsServers = appliedDnsServers;
    }

    public String getInternalDomainNameSuffix() {
        return internalDomainNameSuffix;
    }

    public void setInternalDomainNameSuffix(String internalDomainNameSuffix) {
        this.internalDomainNameSuffix = internalDomainNameSuffix;
    }
}
