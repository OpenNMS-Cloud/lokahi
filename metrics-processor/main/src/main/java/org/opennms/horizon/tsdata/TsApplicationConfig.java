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
package org.opennms.horizon.tsdata;

import com.codahale.metrics.MetricRegistry;
import org.opennms.horizon.timeseries.cortex.CortexTSS;
import org.opennms.horizon.timeseries.cortex.CortexTSSConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TsApplicationConfig {
    @Value("${cortex.write.url}")
    private String cortexWriteURL;

    @Value("${cortex.maxconcurrenthttpconnections:100}")
    private int maxConcurrentHttpConnections;

    @Value("${cortex.cortexwritetimeoutinms:1000}")
    private long cortexWriteTimeoutInMs;

    @Value("${cortex.readtimeoutinms:1000}")
    private long readTimeoutInMs;

    @Value("${cortex.bulkheadmaxwaitdurationinms:9223372036854775807}")
    private long bulkheadMaxWaitDurationInMs;

    @Value("${cortex.organizationid}")
    private String organizationId;

    @Bean
    public CortexTSSConfig cortexTSSConfig() {
        return new CortexTSSConfig(
                cortexWriteURL,
                maxConcurrentHttpConnections,
                cortexWriteTimeoutInMs,
                readTimeoutInMs,
                bulkheadMaxWaitDurationInMs,
                organizationId);
    }

    @Bean
    public CortexTSS createCortex(CortexTSSConfig cortexTSSConfig, MetricRegistry metricRegistry) {
        return new CortexTSS(cortexTSSConfig, metricRegistry);
    }
}
