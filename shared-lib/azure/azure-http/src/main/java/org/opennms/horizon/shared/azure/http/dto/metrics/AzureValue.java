package org.opennms.horizon.shared.azure.http.dto.metrics;

import com.google.gson.annotations.SerializedName;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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

    public void collect(Map<String, Double> collectedData) {
        String metricName = name.getValue();

        if (timeseries.isEmpty()) {
            collectedData.put(metricName, 0d);
        } else {

            AzureTimeseries firstTimeseries = timeseries.get(0);
            List<AzureDatum> data = firstTimeseries.getData();

            //todo: sanity check - double check we actually need to sort (probably not)
            data.sort((o1, o2) -> {
                Instant t1 = Instant.parse(o1.getTimeStamp());
                Instant t2 = Instant.parse(o2.getTimeStamp());
                return t1.compareTo(t2);
            });

            // for now getting last value as it is most recent
            AzureDatum datum = data.get(data.size() - 1);

            Double value = datum.getValue();
            if (value == null) {
                value = 0d;
            }
            collectedData.put(metricName, value);
        }
    }
}
