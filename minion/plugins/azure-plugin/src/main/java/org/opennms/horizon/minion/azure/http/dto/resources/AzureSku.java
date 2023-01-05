package org.opennms.horizon.minion.azure.http.dto.resources;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AzureSku {
    @SerializedName("name")
    @Expose
    private String name;
    @SerializedName("tier")
    @Expose
    private String tier;
}
