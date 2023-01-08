package org.opennms.horizon.minion.plugin.api;

import org.opennms.azure.contract.AzureScanItem;

import java.util.List;

public interface AzureScannerResponse {

    List<AzureScanItem> getResults();

    String getReason();
}
