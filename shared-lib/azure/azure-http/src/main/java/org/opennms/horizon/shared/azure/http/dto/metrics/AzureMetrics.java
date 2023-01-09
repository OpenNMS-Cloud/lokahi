package org.opennms.horizon.shared.azure.http.dto.metrics;

import com.google.gson.annotations.SerializedName;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@ToString
@Getter
@Setter
public class AzureMetrics {
    @SerializedName("cost")
    private Integer cost;
    @SerializedName("timespan")
    private String timespan;
    @SerializedName("interval")
    private String interval;
    @SerializedName("value")
    private List<AzureValue> value = new ArrayList<>();
    @SerializedName("namespace")
    private String namespace;
    @SerializedName("resourceregion")
    private String resourceregion;

    public void collect(Map<String, Double> collectedData) {
        for (AzureValue metric : value) {
            metric.collect(collectedData);
        }
    }
}
