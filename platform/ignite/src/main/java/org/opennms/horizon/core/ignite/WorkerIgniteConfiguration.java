package org.opennms.horizon.core.ignite;

import org.apache.ignite.Ignite;
import org.apache.ignite.Ignition;
import org.apache.ignite.cache.CacheMode;
import org.apache.ignite.cache.CacheRebalanceMode;
import org.apache.ignite.cache.CacheWriteSynchronizationMode;
import org.apache.ignite.cache.PartitionLossPolicy;
import org.apache.ignite.configuration.CacheConfiguration;
import org.apache.ignite.configuration.DataStorageConfiguration;
import org.apache.ignite.configuration.IgniteConfiguration;
import org.apache.ignite.kubernetes.configuration.KubernetesConnectionConfiguration;
import org.apache.ignite.spi.discovery.tcp.TcpDiscoverySpi;
import org.apache.ignite.spi.discovery.tcp.ipfinder.kubernetes.TcpDiscoveryKubernetesIpFinder;
import org.apache.ignite.spi.discovery.tcp.ipfinder.multicast.TcpDiscoveryMulticastIpFinder;
import org.opennms.horizon.shared.lang.CompoundClassLoader;

import java.util.Arrays;

public class WorkerIgniteConfiguration {

    private boolean useKubernetes;
    private String kubernetesServiceName;

//========================================
// Getters and Setters
//----------------------------------------

    public boolean isUseKubernetes() {
        return useKubernetes;
    }

    public void setUseKubernetes(boolean useKubernetes) {
        this.useKubernetes = useKubernetes;
    }

    public String getKubernetesServiceName() {
        return kubernetesServiceName;
    }

    public void setKubernetesServiceName(String kubernetesServiceName) {
        this.kubernetesServiceName = kubernetesServiceName;
    }

//========================================
//
//----------------------------------------

    public IgniteConfiguration prepareIgniteConfiguration() {
        IgniteConfiguration igniteConfiguration = new IgniteConfiguration();

        igniteConfiguration.setClientMode(false);
        igniteConfiguration.setMetricsLogFrequency(0);  // DISABLE IGNITE METRICS

        if (useKubernetes) {
            configureClusterNodeDiscoveryKubernetes(igniteConfiguration);
        } else {
            configureClusterNodeDiscovery(igniteConfiguration);
        }

        configureDataStorage(igniteConfiguration);

        configureClassLoader(igniteConfiguration);

        return igniteConfiguration;
    }

    public Ignite startIgnite(IgniteConfiguration igniteConfiguration) {
        return Ignition.start(igniteConfiguration);
    }

//========================================
// Internals
//----------------------------------------

    private void configureClassLoader(IgniteConfiguration igniteConfiguration) {
        // Required for OSGI, otherwise ignite has trouble finding our application classes for (un)marshalling.  Need
        //  a composite class loader to make sure ignite can still find its own internals as well.

        CompoundClassLoader compoundClassLoader =
                new CompoundClassLoader(
                        Arrays.asList(this.getClass().getClassLoader(), Ignite.class.getClassLoader()));

        igniteConfiguration.setClassLoader(compoundClassLoader);
    }

    private void configureClusterNodeDiscovery(IgniteConfiguration igniteConfiguration) {
        TcpDiscoverySpi tcpDiscoverySpi = new TcpDiscoverySpi();

        // Using port 47401 to separate this cluster from the minion one
        TcpDiscoveryMulticastIpFinder ipFinder = new TcpDiscoveryMulticastIpFinder().setMulticastPort(47401);
        tcpDiscoverySpi.setIpFinder(ipFinder);

        igniteConfiguration.setDiscoverySpi(tcpDiscoverySpi);
    }

    private void configureClusterNodeDiscoveryKubernetes(IgniteConfiguration igniteConfiguration) {
        TcpDiscoverySpi tcpDiscoverySpi = new TcpDiscoverySpi();

        KubernetesConnectionConfiguration connectionConfiguration = new KubernetesConnectionConfiguration();
        connectionConfiguration.setServiceName(kubernetesServiceName);

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
