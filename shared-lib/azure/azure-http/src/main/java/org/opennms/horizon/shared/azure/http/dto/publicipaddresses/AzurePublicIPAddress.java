package org.opennms.horizon.shared.azure.http.dto.publicipaddresses;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AzurePublicIPAddress {
    @SerializedName("id")
    @Expose
    private String id;
    @SerializedName("name")
    @Expose
    private String name;
    @SerializedName("properties")
    @Expose
    private PublicIpAddressProps properties;
}
