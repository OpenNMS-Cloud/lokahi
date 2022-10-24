package org.opennms.horizon.minion.plugin.api;

import java.util.Map;

public interface ServiceDetectorResults {

    /**
     * @return true if the service was detected, false otherwise
     */
    boolean isServiceDetected();

    /**
     * How long did it take to determine if the service is present or not?
     *
     * @return response time in milliseconds
     */
    double getResponseTimeMs();

    /**
     * TECHDEBT: In Horizon 30 this is used to cary information from the agent
     * that was gathered during the detector invocation, to be used by the provisioning
     * process. In the WS-Man detector case, this is used to cary the Windows Server version
     * which is then stored in an asset field.
     *
     * @return additional attributes from the detector invocation
     */
    Map<String, String> getServiceAttributes();

}
