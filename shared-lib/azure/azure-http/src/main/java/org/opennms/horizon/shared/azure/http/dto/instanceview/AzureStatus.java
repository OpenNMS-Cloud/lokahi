package org.opennms.horizon.shared.azure.http.dto.instanceview;

import com.google.gson.annotations.SerializedName;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AzureStatus {
    @SerializedName("code")
    private String code;
    @SerializedName("level")
    private String level;
    @SerializedName("displayStatus")
    private String displayStatus;
    @SerializedName("time")
    private String time;
}
