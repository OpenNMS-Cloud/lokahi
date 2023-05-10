package org.opennms.horizon.shared.azure.http.dto.networkinterface;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class Sku {
    @SerializedName("name")
    @Expose
    private String name;
    @SerializedName("tier")
    @Expose
    private String tier;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getTier() {
        return tier;
    }

    public void setTier(String tier) {
        this.tier = tier;
    }

}
