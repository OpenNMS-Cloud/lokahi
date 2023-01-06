package org.opennms.horizon.shared.azure.http.dto.instanceview;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class AzureInstanceView {
    @SerializedName("disks")
    @Expose
    private List<AzureDisk> disks = new ArrayList<>();
    @SerializedName("bootDiagnostics")
    @Expose
    private AzureBootDiagnostics bootDiagnostics;
    @SerializedName("hyperVGeneration")
    @Expose
    private String hyperVGeneration;
    @SerializedName("statuses")
    @Expose
    private List<AzureStatus> statuses = new ArrayList<>();
}
