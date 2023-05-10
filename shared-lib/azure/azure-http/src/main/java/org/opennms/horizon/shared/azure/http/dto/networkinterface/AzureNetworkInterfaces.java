package org.opennms.horizon.shared.azure.http.dto.networkinterface;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.util.List;

public class AzureNetworkInterfaces {
    @SerializedName("value")
    @Expose
    private List<AzureNetworkInterface> value;

    public List<AzureNetworkInterface> getValue() {
        return value;
    }

    public void setValue(List<AzureNetworkInterface> value) {
        this.value = value;
    }

}
