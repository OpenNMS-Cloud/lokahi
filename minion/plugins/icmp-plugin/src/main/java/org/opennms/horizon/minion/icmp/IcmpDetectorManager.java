package org.opennms.horizon.minion.icmp;

import lombok.RequiredArgsConstructor;
import org.opennms.horizon.minion.plugin.api.ServiceDetector;
import org.opennms.horizon.minion.plugin.api.ServiceDetectorManager;
import org.opennms.horizon.shared.icmp.PingerFactory;

@RequiredArgsConstructor
public class IcmpDetectorManager implements ServiceDetectorManager {
    private final PingerFactory pingerFactory;

    @Override
    public ServiceDetector create() {
        return new IcmpDetector(pingerFactory);
    }
}
