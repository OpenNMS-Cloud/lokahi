package org.opennms.horizon.shared.azure.http.dto.networkinterface;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class NetworkInterfaceProps {
    @SerializedName("ipConfigurations")
    @Expose
    private List<IpConfiguration> ipConfigurations = new ArrayList<>();
    @SerializedName("virtualMachine")
    @Expose
    private VirtualMachine virtualMachine;
}
