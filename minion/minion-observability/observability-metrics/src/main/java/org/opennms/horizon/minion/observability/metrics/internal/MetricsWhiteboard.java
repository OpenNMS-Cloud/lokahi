package org.opennms.horizon.minion.observability.metrics.internal;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.MetricSet;
import com.savoirtech.eos.pattern.whiteboard.AbstractWhiteboard;
import com.savoirtech.eos.util.ServiceProperties;
import org.osgi.framework.BundleContext;

public class MetricsWhiteboard extends AbstractWhiteboard<MetricSet, MetricSet> {

  private final MetricRegistry metricRegistry;

  public MetricsWhiteboard(BundleContext bundleContext) {
    this(new MetricRegistry(), bundleContext);
  }

  public MetricsWhiteboard(MetricRegistry metricRegistry, BundleContext bundleContext) {
    super(bundleContext, MetricSet.class);
    this.metricRegistry = metricRegistry;
  }

  @Override
  protected MetricSet addService(MetricSet service, ServiceProperties props) {
    metricRegistry.registerAll(service);
    return service;
  }

  @Override
  protected void removeService(MetricSet service, MetricSet tracked) {
    for (String metric : service.getMetrics().keySet()) {
      metricRegistry.remove(metric);
    }
  }
}
