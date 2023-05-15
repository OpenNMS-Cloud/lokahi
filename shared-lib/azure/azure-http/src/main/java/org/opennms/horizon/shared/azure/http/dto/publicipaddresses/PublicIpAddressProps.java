package org.opennms.horizon.shared.azure.http.dto.publicipaddresses;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PublicIpAddressProps {
    @SerializedName("ipAddress")
    @Expose
    private String ipAddress;
}
