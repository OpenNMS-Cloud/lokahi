package org.opennms.horizon.inventory.testtool.miniongateway.wiremock.api;

import org.opennms.taskset.service.contract.UpdateTasksRequest;

import java.util.List;

/**
 * API for accessing information from, configuring test interactions, or trigger test interactions into the Minion Gateway.
 */
public interface ServiceApiForMinionGatewayWiremock {
    List<UpdateTasksRequest> getReceivedTaskSetUpdates();
}
