package org.opennms.horizon.minion.plugin.api;

import lombok.Builder;
import lombok.Data;
import org.opennms.azure.contract.AzureScanItem;

import java.util.List;

@Data
@Builder
public class AzureScannerResponseImpl implements AzureScannerResponse {
    private List<AzureScanItem> results;
    private String reason;
}
