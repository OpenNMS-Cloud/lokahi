package org.opennms.horizon.shared.azure.http.dto.metrics;

import com.google.gson.annotations.SerializedName;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.ArrayList;
import java.util.List;

@ToString
@Getter
@Setter
public class AzureValue {
    @SerializedName("id")
    private String id;
    @SerializedName("type")
    private String type;
    @SerializedName("name")
    private AzureName name;
    @SerializedName("displayDescription")
    private String displayDescription;
    @SerializedName("unit")
    private String unit;
    @SerializedName("timeseries")
    private List<AzureTimeseries> timeseries = new ArrayList<>();
    @SerializedName("errorCode")
    private String errorCode;
}
