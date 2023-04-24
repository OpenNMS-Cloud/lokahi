/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2022-2023 The OpenNMS Group, Inc.
 * OpenNMS(R) is Copyright (C) 1999-2023 The OpenNMS Group, Inc.
 *
 * OpenNMS(R) is a registered trademark of The OpenNMS Group, Inc.
 *
 * OpenNMS(R) is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
 *
 * OpenNMS(R) is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with OpenNMS(R).  If not, see:
 *      http://www.gnu.org/licenses/
 *
 * For more information contact:
 *     OpenNMS(R) Licensing <license@opennms.org>
 *     http://www.opennms.org/
 *     http://www.opennms.com/
 *******************************************************************************/

package org.opennms.horizon.minion.taskset.worker.ignite;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.apache.ignite.Ignite;
import org.apache.ignite.cache.CacheMode;
import org.apache.ignite.cache.CacheRebalanceMode;
import org.apache.ignite.cache.CacheWriteSynchronizationMode;
import org.apache.ignite.cache.PartitionLossPolicy;
import org.apache.ignite.configuration.CacheConfiguration;
import org.apache.ignite.configuration.DataStorageConfiguration;
import org.apache.ignite.configuration.IgniteConfiguration;
import org.apache.ignite.internal.processors.resource.GridResourceProcessor;
import org.apache.ignite.kubernetes.configuration.KubernetesConnectionConfiguration;
import org.apache.ignite.spi.discovery.tcp.TcpDiscoverySpi;
import org.apache.ignite.spi.discovery.tcp.ipfinder.kubernetes.TcpDiscoveryKubernetesIpFinder;
import org.apache.ignite.spi.discovery.tcp.ipfinder.multicast.TcpDiscoveryMulticastIpFinder;
import org.apache.ignite.spi.metric.MetricExporterSpi;
import org.opennms.horizon.minion.taskset.worker.ignite.classloader.CompoundClassLoader;
import org.opennms.horizon.minion.taskset.worker.impl.TaskSetLifecycleManagerImpl;

import java.util.Arrays;
import java.util.Optional;

@Data
@AllArgsConstructor
public class WorkerIgniteConfiguration {

    private final MetricExporterSpi metricExporterSpi;
    private final boolean useKubernetes;
    private final String kubernetesServiceName;
    private final String kubernetesNamespace;

    public IgniteConfiguration prepareIgniteConfiguration() {
        IgniteConfiguration igniteConfiguration = new IgniteConfiguration();

        igniteConfiguration.setClientMode(false);
        igniteConfiguration.setMetricsLogFrequency(0);  // DISABLE IGNITE METRICS

        Optional.ofNullable(System.getenv("MINION_ID"))
            .or(() -> Optional.ofNullable(System.getenv("HOSTNAME")))
            .ifPresent(igniteConfiguration::setConsistentId);

        if (useKubernetes) {
            configureClusterNodeDiscoveryKubernetes(igniteConfiguration);
        } else {
            configureClusterNodeDiscovery(igniteConfiguration);
        }

        configureDataStorage(igniteConfiguration);
        configureCache(igniteConfiguration, "workflows");
        configureCache(igniteConfiguration, TaskSetLifecycleManagerImpl.TASK_SERVICE_CACHE_NAME);

        configureClassLoader(igniteConfiguration);
        igniteConfiguration.setMetricExporterSpi(metricExporterSpi);

        return igniteConfiguration;
    }

//========================================
// Internals
//----------------------------------------

    private void configureClassLoader(IgniteConfiguration igniteConfiguration) {
        // Required for OSGI, otherwise ignite has trouble finding our application classes for (un)marshalling.  Need
        //  a composite class loader to make sure ignite can still find its own internals as well.

        CompoundClassLoader compoundClassLoader =
                new CompoundClassLoader(this,
                        Arrays.asList(this.getClass().getClassLoader(), Ignite.class.getClassLoader(), GridResourceProcessor.class.getClassLoader()));

        igniteConfiguration.setClassLoader(compoundClassLoader);
    }

    private void configureClusterNodeDiscovery(IgniteConfiguration igniteConfiguration) {
        TcpDiscoverySpi tcpDiscoverySpi = new TcpDiscoverySpi();

        // Using defaults for now (multicast group 228.1.2.4, port 47400)
        TcpDiscoveryMulticastIpFinder ipFinder = new TcpDiscoveryMulticastIpFinder();
        tcpDiscoverySpi.setIpFinder(ipFinder);

        igniteConfiguration.setDiscoverySpi(tcpDiscoverySpi);
    }

    private void configureClusterNodeDiscoveryKubernetes(IgniteConfiguration igniteConfiguration) {
        TcpDiscoverySpi tcpDiscoverySpi = new TcpDiscoverySpi();

        KubernetesConnectionConfiguration connectionConfiguration = new KubernetesConnectionConfiguration();
        connectionConfiguration.setServiceName(kubernetesServiceName);

        if ((kubernetesNamespace != null) && (! kubernetesNamespace.isEmpty())) {
            connectionConfiguration.setNamespace(kubernetesNamespace);
        }

        TcpDiscoveryKubernetesIpFinder ipFinder = new TcpDiscoveryKubernetesIpFinder(connectionConfiguration);
        tcpDiscoverySpi.setIpFinder(ipFinder);

        igniteConfiguration.setDiscoverySpi(tcpDiscoverySpi);
    }

    private void configureDataStorage(IgniteConfiguration igniteConfiguration) {
        DataStorageConfiguration dataStorageConfiguration = new DataStorageConfiguration();
        dataStorageConfiguration.getDefaultDataRegionConfiguration().setPersistenceEnabled(false);

        igniteConfiguration.setDataStorageConfiguration(dataStorageConfiguration);
    }

    private void configureCache(IgniteConfiguration igniteConfiguration, String cacheName) {
        CacheConfiguration<?,?> cacheConfiguration = new CacheConfiguration<>(cacheName);

        cacheConfiguration.setCacheMode(CacheMode.PARTITIONED);
        cacheConfiguration.setBackups(2);
        cacheConfiguration.setRebalanceMode(CacheRebalanceMode.SYNC);
        cacheConfiguration.setWriteSynchronizationMode(CacheWriteSynchronizationMode.FULL_SYNC);
        cacheConfiguration.setPartitionLossPolicy(PartitionLossPolicy.READ_ONLY_SAFE);

        igniteConfiguration.setCacheConfiguration(cacheConfiguration);
    }
}
