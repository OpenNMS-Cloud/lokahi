package org.opennms.horizon.minion.azure.http.dto.instanceview;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AzureStatus {
    @SerializedName("code")
    @Expose
    private String code;
    @SerializedName("level")
    @Expose
    private String level;
    @SerializedName("displayStatus")
    @Expose
    private String displayStatus;
    @SerializedName("time")
    @Expose
    private String time;
}
