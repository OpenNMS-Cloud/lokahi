package org.opennms.horizon.minion.plugin.api;

import java.net.InetAddress;
import java.util.Map;

public interface ServiceDetectorRequest {

    /**
     * @return the address of the host against with the detector should be invoked.
     */
    InetAddress getAddress();

    /**
     * @return additional attributes stored outside of the detector's configuration that
     * may be required when running the detector.
     */
    Map<String, String> getRuntimeAttributes();

}
