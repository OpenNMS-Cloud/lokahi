package org.opennms.horizon.inventory.util;

import org.springframework.stereotype.Component;

@Component
public class Clock {
    public long getCurrentTimeMs() {
        return System.currentTimeMillis();
    }
}
