package org.opennms.horizon.minion.azure;

import org.opennms.horizon.minion.plugin.api.Scanner;
import org.opennms.horizon.minion.plugin.api.ScannerManager;
import org.opennms.horizon.shared.azure.http.AzureHttpClient;

public class AzureScannerManager implements ScannerManager {

    @Override
    public Scanner create() {
        AzureHttpClient client = new AzureHttpClient();
        return new AzureScanner(client);
    }
}
