package org.opennms.horizon.shared.azure.http.dto.instanceview;

import com.google.gson.annotations.SerializedName;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class AzureInstanceView {
    @SerializedName("disks")
    private List<AzureDisk> disks = new ArrayList<>();
    @SerializedName("bootDiagnostics")
    private AzureBootDiagnostics bootDiagnostics;
    @SerializedName("hyperVGeneration")
    private String hyperVGeneration;
    @SerializedName("statuses")
    private List<AzureStatus> statuses = new ArrayList<>();
}
