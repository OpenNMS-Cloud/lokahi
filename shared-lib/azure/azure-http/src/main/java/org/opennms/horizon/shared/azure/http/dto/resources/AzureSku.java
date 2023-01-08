package org.opennms.horizon.shared.azure.http.dto.resources;

import com.google.gson.annotations.SerializedName;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AzureSku {
    @SerializedName("name")
    private String name;
    @SerializedName("tier")
    private String tier;
}
