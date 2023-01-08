package org.opennms.horizon.shared.azure.http.dto.metrics;

import com.google.gson.annotations.SerializedName;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@ToString
@Getter
@Setter
public class AzureDatum {
    @SerializedName("timeStamp")
    private String timeStamp;
    @SerializedName("total")
    private Double total;
    @SerializedName("average")
    private Double average;


    // Datum return different keys depending on the metric. Either total or average
    public Double getValue() {
        if (total != null) {
            return total;
        }
        if (average != null) {
            return average;
        }
        return null;
    }
}
