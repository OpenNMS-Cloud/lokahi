package org.opennms.horizon.minion.plugin.api;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

@Data
@Builder
public class ServiceDetectorResponseImpl implements ServiceDetectorResponse {
    private boolean serviceDetected;
    private double responseTimeMs;
    private Map<String, String> serviceAttributes;

    public static ServiceDetectorResponse down() {
        return builder().serviceDetected(false).build();
    }

    public static ServiceDetectorResponse up() {
        return builder().serviceDetected(true).build();
    }
}
