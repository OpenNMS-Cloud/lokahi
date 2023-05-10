package org.opennms.horizon.shared.azure.http.dto.publicipaddresses;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.util.List;

public class AzurePublicIpAddresses {
    @SerializedName("value")
    @Expose
    private List<AzurePublicIPAddress> value;

    public List<AzurePublicIPAddress> getValue() {
        return value;
    }

    public void setValue(List<AzurePublicIPAddress> value) {
        this.value = value;
    }
}
