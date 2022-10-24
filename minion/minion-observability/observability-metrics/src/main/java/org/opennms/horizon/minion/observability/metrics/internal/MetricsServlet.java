package org.opennms.horizon.minion.observability.metrics.internal;

import com.codahale.metrics.MetricRegistry;
import io.prometheus.client.Collector.MetricFamilySamples;
import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.dropwizard.DropwizardExports;
import io.prometheus.client.exporter.common.TextFormat;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Enumeration;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class MetricsServlet extends HttpServlet {

  private final CollectorRegistry collectorRegistry;

  public MetricsServlet(MetricRegistry metricRegistry) {
    collectorRegistry = new CollectorRegistry();
    collectorRegistry.register(new DropwizardExports(metricRegistry));
  }

  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
    Enumeration<MetricFamilySamples> samples = collectorRegistry.metricFamilySamples();

    try (ServletOutputStream outputStream = resp.getOutputStream()) {
      try (Writer writer = new OutputStreamWriter(outputStream)) {
        TextFormat.write004(writer, samples);
      }

      outputStream.flush();
    }

  }
}
