package org.opennms.horizon.minion.azure;

import lombok.RequiredArgsConstructor;
import org.opennms.horizon.minion.azure.http.AzureHttpClient;
import org.opennms.horizon.minion.plugin.api.Scanner;
import org.opennms.horizon.minion.plugin.api.ScannerManager;

public class AzureScannerManager implements ScannerManager {

    @Override
    public Scanner create() {
        AzureHttpClient client = new AzureHttpClient();
        return new AzureScanner(client);
    }
}
