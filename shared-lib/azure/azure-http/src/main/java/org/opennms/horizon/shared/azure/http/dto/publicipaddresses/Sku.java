package org.opennms.horizon.shared.azure.http.dto.publicipaddresses;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Sku {
    @SerializedName("name")
    @Expose
    private String name;
    @SerializedName("tier")
    @Expose
    private String tier;
}
