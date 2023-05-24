package org.opennms.horizon.minion.taskset.worker.ignite;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.MetricRegistry;
import org.apache.ignite.lang.IgniteFuture;
import org.apache.ignite.spi.IgniteSpiContext;
import org.apache.ignite.spi.IgniteSpiException;
import org.apache.ignite.spi.metric.BooleanMetric;
import org.apache.ignite.spi.metric.DoubleMetric;
import org.apache.ignite.spi.metric.IntMetric;
import org.apache.ignite.spi.metric.LongMetric;
import org.apache.ignite.spi.metric.Metric;
import org.apache.ignite.spi.metric.MetricExporterSpi;
import org.apache.ignite.spi.metric.ReadOnlyMetricManager;
import org.apache.ignite.spi.metric.ReadOnlyMetricRegistry;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.opennms.horizon.minion.observability.metrics.MetricsProvider;
import org.opennms.horizon.shared.ipc.rpc.IpcIdentity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Predicate;

import static com.codahale.metrics.MetricRegistry.name;

public class IgniteMetricsAdapter implements MetricExporterSpi, MetricsProvider, Runnable {

  private final Logger logger = LoggerFactory.getLogger(IgniteMetricsAdapter.class);
  private final MetricRegistry metrics = new MetricRegistry();
  private final IpcIdentity identity;

  private AtomicBoolean connected = new AtomicBoolean();
  private AtomicBoolean work = new AtomicBoolean();
  private ReadOnlyMetricManager registry;

  public IgniteMetricsAdapter(IpcIdentity identity) {
    this.identity = identity;
  }

  @Override
  public void setMetricRegistry(ReadOnlyMetricManager registry) {
    this.registry = registry;
  }

  @Override
  public void setExportFilter(Predicate<ReadOnlyMetricRegistry> filter) {

  }

  @Override
  public String getName() {
    return "metrics-bridge";
  }

  @Override
  public Map<String, Object> getNodeAttributes() throws IgniteSpiException {
    return Collections.emptyMap();
  }

  @Override
  public void spiStart(@Nullable String igniteInstanceName) throws IgniteSpiException {
    metrics.gauge(connectedMetricName(), () -> new Gauge<Boolean>() {
      @Override
      public Boolean getValue() {
        return connected.get();
      }
    });
  }

  @Override
  public void onContextInitialized(IgniteSpiContext spiCtx) throws IgniteSpiException {

  }

  @Override
  public void onContextDestroyed() {

  }

  @Override
  public void spiStop() throws IgniteSpiException {
    metrics.remove(connectedMetricName());
  }

  @Override
  public void onClientDisconnected(IgniteFuture<?> reconnectFut) {
    connected.set(false);
  }

  @Override
  public void onClientReconnected(boolean clusterRestarted) {
    connected.set(true);
  }

  @Override
  public MetricRegistry getMetrics() {
    return metrics;
  }

  private void refreshMetrics() {
    if (registry == null) {
      return;
    }

    registry.forEach(metricRegistry -> {
      metricRegistry.forEach(metric -> {
        String metricName = name("minion", identity.getId(), "ignite", metric.name());
        if (!metrics.getMetrics().containsKey(metricName)) {
          com.codahale.metrics.Metric bridge = bridge(metric);
          if (bridge != null) {
            logger.debug("Registered metric {}: {}", metric.name(), metric.description());
            metrics.register(metricName, bridge);
          }
        }
      });
    });
  }

  private com.codahale.metrics.Metric bridge(Metric metric) {
    if (metric instanceof BooleanMetric) {
      return new Gauge<>() {
        @Override
        public Boolean getValue() {
          return ((BooleanMetric) metric).value();
        }
      };
    }
    if (metric instanceof IntMetric) {
      return new Gauge<>() {
        @Override
        public Integer getValue() {
          return ((IntMetric) metric).value();
        }
      };
    }
    if (metric instanceof LongMetric) {
      return new Gauge<>() {
        @Override
        public Long getValue() {
          return ((LongMetric) metric).value();
        }
      };
    }
    if (metric instanceof DoubleMetric) {
      return new Gauge<>() {
        @Override
        public Double getValue() {
          return ((DoubleMetric) metric).value();
        }
      };
    }
    if (metric instanceof BooleanMetric) {
      return new Gauge<>() {
        @Override
        public Boolean getValue() {
          return ((BooleanMetric) metric).value();
        }
      };
    }
    return null;
  }

  @NotNull
  private String connectedMetricName() {
    return name("minion", identity.getId(), "connected");
  }

  public void start() throws Exception {
    work.set(true);
    Thread thread = new Thread(this, "ignite-metrics-refresher");
    thread.setDaemon(true);
    thread.start();
  }

  public void stop() {
    work.set(false);
  }

  public void run() {
    while (work.get()) {
      refreshMetrics();
      try {
        Thread.sleep(30_000);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        logger.error("Failed to sleep refreshing metrics", e);
      }
    }
  }
}
