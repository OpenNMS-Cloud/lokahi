package org.opennms.horizon.shared.azure.http.dto.instanceview;

import com.google.gson.annotations.SerializedName;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class AzureDisk {
    @SerializedName("name")
    private String name;
    @SerializedName("statuses")
    private List<AzureStatus> statuses = new ArrayList<>();
}
