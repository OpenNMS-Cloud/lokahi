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
package org.opennms.miniongateway.ignite;

import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteCheckedException;
import org.apache.ignite.IgniteSpring;
import org.apache.ignite.configuration.ClientConnectorConfiguration;
import org.apache.ignite.configuration.IgniteConfiguration;
import org.apache.ignite.configuration.ThinClientConfiguration;
import org.apache.ignite.kubernetes.configuration.KubernetesConnectionConfiguration;
import org.apache.ignite.spi.discovery.tcp.TcpDiscoverySpi;
import org.apache.ignite.spi.discovery.tcp.ipfinder.kubernetes.TcpDiscoveryKubernetesIpFinder;
import org.apache.ignite.spi.discovery.tcp.ipfinder.multicast.TcpDiscoveryMulticastIpFinder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;

@Configuration
public class IgniteConfig {

    @Value("${ignite.use-kubernetes:true}")
    private boolean useKubernetes;

    @Value("${ignite.kubernetes-namespace:default}")
    private String kubernetesNamespace;

    @Value("${ignite.kubernetes-service-name:opennms-minion-gateway-ignite}")
    private String kubernetesServiceName;

    @Value("${ignite.config:file*:/config/ignite.xml}")
    private String igniteConfig;

    @Autowired
    private ApplicationContext applicationContext;

    private final Logger logger = LoggerFactory.getLogger(IgniteConfig.class);

    // ========================================
    // Beans
    // ----------------------------------------

    @DependsOn("igniteJdbcConnectionStartupGate")
    @Bean
    public Ignite ignite(@Autowired(required = false) IgniteConfiguration cfg) {
        if (cfg == null) {
            cfg = new IgniteConfiguration();
        }
        IgniteConfiguration igniteConfiguration = this.prepareIgniteConfiguration(cfg);

        try {
            return IgniteSpring.start(igniteConfiguration, applicationContext);
        } catch (IgniteCheckedException icExc) {
            throw new RuntimeException("failed to start Ignite", icExc);
        }
    }

    // ========================================
    // Internals
    // ----------------------------------------

    private IgniteConfiguration prepareIgniteConfiguration(IgniteConfiguration igniteConfiguration) {
        igniteConfiguration.setClassLoader(applicationContext.getClassLoader());

        // enable compute calls from thin client
        ThinClientConfiguration thinClientConfiguration = new ThinClientConfiguration();
        thinClientConfiguration.setMaxActiveComputeTasksPerConnection(100);

        igniteConfiguration.setClientMode(false);
        igniteConfiguration.setMetricsLogFrequency(0); // DISABLE IGNITE METRICS
        igniteConfiguration.setClientConnectorConfiguration(new ClientConnectorConfiguration()
                .setThinClientConfiguration(thinClientConfiguration)); // enable client connector

        if (useKubernetes) {
            configureClusterNodeDiscoveryKubernetes(igniteConfiguration);
        } else {
            configureClusterNodeDiscovery(igniteConfiguration);
        }

        return igniteConfiguration;
    }

    private void configureClusterNodeDiscovery(
            org.apache.ignite.configuration.IgniteConfiguration igniteConfiguration) {
        TcpDiscoverySpi tcpDiscoverySpi = new TcpDiscoverySpi();

        // Using port 47401 to separate this cluster from the minion one
        TcpDiscoveryMulticastIpFinder ipFinder = new TcpDiscoveryMulticastIpFinder().setMulticastPort(47401);
        tcpDiscoverySpi.setIpFinder(ipFinder);

        igniteConfiguration.setDiscoverySpi(tcpDiscoverySpi);
    }

    private void configureClusterNodeDiscoveryKubernetes(
            org.apache.ignite.configuration.IgniteConfiguration igniteConfiguration) {
        TcpDiscoverySpi tcpDiscoverySpi = new TcpDiscoverySpi();

        KubernetesConnectionConfiguration connectionConfiguration = new KubernetesConnectionConfiguration();
        connectionConfiguration.setNamespace(kubernetesNamespace);
        connectionConfiguration.setServiceName(kubernetesServiceName);

        TcpDiscoveryKubernetesIpFinder ipFinder = new TcpDiscoveryKubernetesIpFinder(connectionConfiguration);
        tcpDiscoverySpi.setIpFinder(ipFinder);

        igniteConfiguration.setDiscoverySpi(tcpDiscoverySpi);
    }
}
