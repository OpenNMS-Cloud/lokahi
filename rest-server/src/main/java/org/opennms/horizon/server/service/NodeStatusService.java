package org.opennms.horizon.server.service;

import lombok.RequiredArgsConstructor;
import org.opennms.horizon.inventory.dto.IpInterfaceDTO;
import org.opennms.horizon.inventory.dto.NodeDTO;
import org.opennms.horizon.server.model.TSResult;
import org.opennms.horizon.server.model.TimeRangeUnit;
import org.opennms.horizon.server.model.TimeSeriesQueryResult;
import org.opennms.horizon.server.service.grpc.InventoryClient;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static java.util.Objects.isNull;
import static org.springframework.util.CollectionUtils.isEmpty;

@Service
@RequiredArgsConstructor
public class NodeStatusService {
    private static final String RESPONSE_TIME_METRIC = "response_time_msec";
    private static final int TIME_RANGE_IN_MINUTES = 1;
    private static final String NODE_ID_KEY = "node_id";
    private static final String MONITOR_KEY = "monitor";
    private static final String INSTANCE_KEY = "instance";
    private static final long TIMEOUT_IN_SECONDS = 30L;
    private final InventoryClient client;
    private final PrometheusTSDBServiceImpl prometheusTSDBService;

    public boolean getNodeStatus(long id, String monitorType, String accessToken) {
        NodeDTO node = client.getNodeById(id, accessToken);

        if (node.getIpInterfacesCount() > 0) {
            IpInterfaceDTO ipInterface = node.getIpInterfaces(0);
            String ipAddress = ipInterface.getIpAddress();

            TimeSeriesQueryResult result = getStatusMetric(id, ipAddress, monitorType);
            if (isNull(result)) {
                return false;
            }
            List<TSResult> tsResults = result.getData().getResult();

            if (isEmpty(tsResults)) {
                return false;
            }

            TSResult tsResult = tsResults.get(0);
            List<List<Double>> values = tsResult.getValues();

            return !isEmpty(values);
        }
        return false;
    }

    private TimeSeriesQueryResult getStatusMetric(long id, String ipAddress, String monitorType) {
        Map<String, String> labels = new HashMap<>();
        labels.put(NODE_ID_KEY, String.valueOf(id));
        labels.put(MONITOR_KEY, monitorType);
        labels.put(INSTANCE_KEY, ipAddress);

        Mono<TimeSeriesQueryResult> result = prometheusTSDBService
            .getMetric(RESPONSE_TIME_METRIC, labels, TIME_RANGE_IN_MINUTES, TimeRangeUnit.MINUTE);
        try {
            return result.toFuture().get(TIMEOUT_IN_SECONDS, TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Failed to get metrics for node", e);
        }
    }
}
