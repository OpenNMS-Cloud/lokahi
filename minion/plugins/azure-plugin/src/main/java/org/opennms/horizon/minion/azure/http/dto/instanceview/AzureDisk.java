package org.opennms.horizon.minion.azure.http.dto.instanceview;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class AzureDisk {
    @SerializedName("name")
    @Expose
    private String name;
    @SerializedName("statuses")
    @Expose
    private List<AzureStatus> statuses = new ArrayList<>();
}
