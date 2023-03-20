package org.opennms.horizon.inventory.tenantmetrics;

import io.micrometer.prometheus.PrometheusMeterRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

@Component
public class TenantMetricsNodeCountCollectorInitializer {
    @Autowired
    private PrometheusMeterRegistry prometheusMeterRegistry;

    @Autowired
    private TenantMetricsNodeCountCollector tenantMetricsNodeCountCollector;

    @PostConstruct
    public void init() {
        prometheusMeterRegistry.getPrometheusRegistry().register(tenantMetricsNodeCountCollector);
    }
}
