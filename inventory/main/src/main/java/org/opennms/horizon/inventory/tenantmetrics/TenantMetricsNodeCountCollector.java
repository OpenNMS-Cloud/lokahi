package org.opennms.horizon.inventory.tenantmetrics;

import io.prometheus.client.Collector;
import lombok.Setter;
import org.opennms.horizon.inventory.model.TenantCount;
import org.opennms.horizon.inventory.repository.NodeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class TenantMetricsNodeCountCollector extends Collector {

    public static final String NODE_COUNT_METRIC_NAME = "node_count";
    public static final String NODE_COUNT_METRIC_DESCRIPTION = "Count of Nodes";
    public static final String NODE_COUNT_TENANT_LABEL_NAME = "tenant";

    @Autowired
    @Setter     // for testability
    NodeRepository nodeRepository;

    @Override
    public List<MetricFamilySamples> collect() {
        List<TenantCount> tenantCountList = nodeRepository.countNodesByTenant();

        var samplesList =
            tenantCountList.stream()
                .map(this::convertTenantCountToSample)
                .toList();

        MetricFamilySamples metricFamilySamples =
            new MetricFamilySamples(
                NODE_COUNT_METRIC_NAME,
                Type.GAUGE,
                NODE_COUNT_METRIC_DESCRIPTION,
                samplesList
            );

        return List.of(metricFamilySamples);
    }

    private MetricFamilySamples.Sample convertTenantCountToSample(TenantCount tenantCount) {
        return
            new MetricFamilySamples.Sample(
                NODE_COUNT_METRIC_NAME,
                List.of(NODE_COUNT_TENANT_LABEL_NAME),
                List.of(tenantCount.tenantId()),
                tenantCount.count()
            );
    }
}
