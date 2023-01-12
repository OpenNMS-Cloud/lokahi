package org.opennms.horizon.minion.plugin.api;

import com.google.protobuf.Message;

public interface ScanResultsResponse {
    Message getResults();

    String getReason();
}
