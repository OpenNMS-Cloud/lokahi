/*
 * Licensed to The OpenNMS Group, Inc (TOG) under one or more
 * contributor license agreements.  See the LICENSE.md file
 * distributed with this work for additional information
 * regarding copyright ownership.
 *
 * TOG licenses this file to You under the GNU Affero General
 * Public License Version 3 (the "License") or (at your option)
 * any later version.  You may not use this file except in
 * compliance with the License.  You may obtain a copy of the
 * License at:
 *
 *      https://www.gnu.org/licenses/agpl-3.0.txt
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied.  See the License for the specific
 * language governing permissions and limitations under the
 * License.
 */
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
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MetricsServlet extends HttpServlet {
    private static final Logger log = LoggerFactory.getLogger(MetricsServlet.class);

    private final CollectorRegistry collectorRegistry;

    public MetricsServlet(MetricRegistry metricRegistry) {
        collectorRegistry = new CollectorRegistry();
        collectorRegistry.register(new DropwizardExports(metricRegistry));
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) {
        Enumeration<MetricFamilySamples> samples = collectorRegistry.metricFamilySamples();

        try (ServletOutputStream outputStream = resp.getOutputStream()) {
            try (Writer writer = new OutputStreamWriter(outputStream)) {
                TextFormat.write004(writer, samples);
            }

            flush(outputStream, resp);

        } catch (IOException e) {
            log.error("Failed to write samples to output stream", e);
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    private void flush(ServletOutputStream outputStream, HttpServletResponse resp) {
        try {
            outputStream.flush();
        } catch (IOException e) {
            log.error("Failed to flush samples to output stream", e);
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }
}
