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
package org.opennms.miniongateway;

import com.codahale.metrics.MetricRegistry;
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.OpenTelemetry;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ImportResource;

@SpringBootConfiguration
@SpringBootApplication
@ImportResource("classpath:/ignite-cache-config.xml")
public class MinionGatewayMain {

    public static void main(String[] args) {
        SpringApplication.run(MinionGatewayMain.class, args);
    }

    @Bean
    public MetricRegistry metricRegistry() {
        return new MetricRegistry();
    }

    @Bean
    public OpenTelemetry openTelemetry() {
        return GlobalOpenTelemetry.get(); // this gets the SDK that was configured by the Java agent
    }
}
