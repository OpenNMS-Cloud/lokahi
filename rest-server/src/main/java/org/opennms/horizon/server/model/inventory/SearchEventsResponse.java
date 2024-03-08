package org.opennms.horizon.server.model.inventory;

import lombok.AllArgsConstructor;
import lombok.Getter;


@AllArgsConstructor
@Getter
public class SearchEventsResponse {
    private byte[] searchEventsBytes;
    private DownloadFormat downloadFormat;
}

