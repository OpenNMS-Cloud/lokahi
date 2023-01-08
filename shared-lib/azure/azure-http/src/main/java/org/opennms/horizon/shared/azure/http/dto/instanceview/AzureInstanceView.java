package org.opennms.horizon.shared.azure.http.dto.instanceview;

import com.google.gson.annotations.SerializedName;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class AzureInstanceView {
    private static final String POWER_STATE_RUNNING = "PowerState/running";
    private static final String POWER_STATE_DEALLOCATED = "PowerState/deallocated";
    private static final String PROVISIONING_STATE_SUCCEEDED = "ProvisioningState/succeeded";

    @SerializedName("disks")
    private List<AzureDisk> disks = new ArrayList<>();
    @SerializedName("bootDiagnostics")
    private AzureBootDiagnostics bootDiagnostics;
    @SerializedName("hyperVGeneration")
    private String hyperVGeneration;
    @SerializedName("statuses")
    private List<AzureStatus> statuses = new ArrayList<>();

    public boolean isUp() {
        for (AzureStatus status : getStatuses()) {
            String code = status.getCode();
            if (code.equalsIgnoreCase(POWER_STATE_RUNNING)) {
                return true;
            }
        }
        return false;
    }

    public Long getUptimeInMs() {

        // check if its been deallocated as a succeeded is also in the list as a previous element
        // maybe instead loop in reverse for succeeded provisioning state...
        for (AzureStatus status : getStatuses()) {
            String code = status.getCode();
            if (code.equalsIgnoreCase(POWER_STATE_DEALLOCATED)) {
                return null;
            }
        }

        for (AzureStatus status : getStatuses()) {
            String code = status.getCode();
            if (code.equalsIgnoreCase(PROVISIONING_STATE_SUCCEEDED)) {
                return getUptimeInMs(status);
            }
        }
        return null;
    }

    private long getUptimeInMs(AzureStatus status) {
        ZonedDateTime statusDateTime = ZonedDateTime.parse(status.getTime(), DateTimeFormatter.ISO_OFFSET_DATE_TIME);
        ZonedDateTime nowDateTime = ZonedDateTime.now(statusDateTime.getZone());

        Instant statusInstant = statusDateTime.toInstant();
        Instant nowInstant = nowDateTime.toInstant();

        return nowInstant.toEpochMilli() - statusInstant.toEpochMilli();
    }
}
