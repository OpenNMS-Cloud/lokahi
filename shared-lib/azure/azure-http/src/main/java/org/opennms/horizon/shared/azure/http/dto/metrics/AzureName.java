package org.opennms.horizon.shared.azure.http.dto.metrics;

import com.google.gson.annotations.SerializedName;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@ToString
@Getter
@Setter
public class AzureName {
    @SerializedName("value")
    private String value;
    @SerializedName("localizedValue")
    private String localizedValue;
}
