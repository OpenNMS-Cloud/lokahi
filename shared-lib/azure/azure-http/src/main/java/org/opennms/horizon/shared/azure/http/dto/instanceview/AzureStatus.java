package org.opennms.horizon.shared.azure.http.dto.instanceview;

import com.google.gson.annotations.SerializedName;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

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

    public Long getElapsedTimeMs() {
        if (time == null) {
            return null;
        }
        ZonedDateTime statusDateTime = ZonedDateTime.parse(time, DateTimeFormatter.ISO_OFFSET_DATE_TIME);
        ZonedDateTime nowDateTime = ZonedDateTime.now(statusDateTime.getZone());

        Instant statusInstant = statusDateTime.toInstant();
        Instant nowInstant = nowDateTime.toInstant();

        return nowInstant.toEpochMilli() - statusInstant.toEpochMilli();
    }
}
