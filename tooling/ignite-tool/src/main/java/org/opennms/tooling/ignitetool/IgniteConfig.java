package org.opennms.tooling.ignitetool;

import org.apache.ignite.Ignite;
import org.apache.ignite.Ignition;
import org.apache.ignite.configuration.DataStorageConfiguration;
import org.apache.ignite.configuration.IgniteConfiguration;
import org.apache.ignite.kubernetes.configuration.KubernetesConnectionConfiguration;
import org.apache.ignite.spi.discovery.tcp.TcpDiscoverySpi;
import org.apache.ignite.spi.discovery.tcp.ipfinder.kubernetes.TcpDiscoveryKubernetesIpFinder;
import org.apache.ignite.spi.discovery.tcp.ipfinder.multicast.TcpDiscoveryMulticastIpFinder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class IgniteConfig {

    @Value("${ignite.use-kubernetes:false}")
    private boolean useKubernetes;

    @Value("${ignite.kubernetes-service-name:unknown}")
    private String kubernetesServiceName;

//========================================
// Beans
//----------------------------------------

    @Bean
    public Ignite ignite() {
        IgniteConfiguration igniteConfiguration = this.prepareIgniteConfiguration();

        return Ignition.start(igniteConfiguration);
    }

//========================================
// Internals
//----------------------------------------

    private IgniteConfiguration prepareIgniteConfiguration() {
        org.apache.ignite.configuration.IgniteConfiguration igniteConfiguration = new org.apache.ignite.configuration.IgniteConfiguration();

        igniteConfiguration.setClientMode(false);
        igniteConfiguration.setMetricsLogFrequency(0);  // DISABLE IGNITE METRICS

        if (useKubernetes) {
            configureClusterNodeDiscoveryKubernetes(igniteConfiguration);
        } else {
            configureClusterNodeDiscovery(igniteConfiguration);
        }

        configureDataStorage(igniteConfiguration);

        return igniteConfiguration;
    }

    private void configureClusterNodeDiscovery(org.apache.ignite.configuration.IgniteConfiguration igniteConfiguration) {
        TcpDiscoverySpi tcpDiscoverySpi = new TcpDiscoverySpi();

        // Using defaults for now (multicast group 228.1.2.4, port 47400)
        TcpDiscoveryMulticastIpFinder ipFinder = new TcpDiscoveryMulticastIpFinder().setMulticastPort(47401);
        tcpDiscoverySpi.setIpFinder(ipFinder);

        igniteConfiguration.setDiscoverySpi(tcpDiscoverySpi);
    }

    private void configureClusterNodeDiscoveryKubernetes(org.apache.ignite.configuration.IgniteConfiguration igniteConfiguration) {
        TcpDiscoverySpi tcpDiscoverySpi = new TcpDiscoverySpi();

        KubernetesConnectionConfiguration connectionConfiguration = new KubernetesConnectionConfiguration();
        connectionConfiguration.setServiceName(kubernetesServiceName);

        TcpDiscoveryKubernetesIpFinder ipFinder = new TcpDiscoveryKubernetesIpFinder(connectionConfiguration);
        tcpDiscoverySpi.setIpFinder(ipFinder);

        igniteConfiguration.setDiscoverySpi(tcpDiscoverySpi);
    }

    private void configureDataStorage(org.apache.ignite.configuration.IgniteConfiguration igniteConfiguration) {
        DataStorageConfiguration dataStorageConfiguration = new DataStorageConfiguration();
        dataStorageConfiguration.getDefaultDataRegionConfiguration().setPersistenceEnabled(false);

        igniteConfiguration.setDataStorageConfiguration(dataStorageConfiguration);
    }
}
