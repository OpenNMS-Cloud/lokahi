package org.opennms.horizon.shared.azure.http.dto.networkinterface;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class IpConfiguration {
    @SerializedName("id")
    @Expose
    private String id;
    @SerializedName("properties")
    @Expose
    private IpConfigurationProps properties;
}
