package org.opennms.horizon.minion.plugin.api;

import com.google.protobuf.Message;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ScanResultsResponseImpl implements ScanResultsResponse {
    private Message results;

    private String reason;
}
