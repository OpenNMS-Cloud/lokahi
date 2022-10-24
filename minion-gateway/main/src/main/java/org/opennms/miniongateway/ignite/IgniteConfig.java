package org.opennms.miniongateway.ignite;

import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteCheckedException;
import org.apache.ignite.IgniteSpring;
import org.apache.ignite.Ignition;
import org.apache.ignite.configuration.ClientConnectorConfiguration;
import org.apache.ignite.configuration.DataStorageConfiguration;
import org.apache.ignite.configuration.IgniteConfiguration;
import org.apache.ignite.configuration.ThinClientConfiguration;
import org.apache.ignite.kubernetes.configuration.KubernetesConnectionConfiguration;
import org.apache.ignite.spi.discovery.tcp.TcpDiscoverySpi;
import org.apache.ignite.spi.discovery.tcp.ipfinder.kubernetes.TcpDiscoveryKubernetesIpFinder;
import org.apache.ignite.spi.discovery.tcp.ipfinder.multicast.TcpDiscoveryMulticastIpFinder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class IgniteConfig {

    @Value("${ignite.use-kubernetes:false}")
    private boolean useKubernetes;

    @Value("${ignite.kubernetes-service-name:unknown}")
    private String kubernetesServiceName;

    @Autowired
    private ApplicationContext applicationContext;

//========================================
// Beans
//----------------------------------------

    @Bean
    public Ignite ignite() {
        IgniteConfiguration igniteConfiguration = this.prepareIgniteConfiguration();

        try {
            return IgniteSpring.start(igniteConfiguration, applicationContext);
        } catch (IgniteCheckedException icExc) {
            throw new RuntimeException("failed to start Ignite", icExc);
        }
    }

//========================================
// Internals
//----------------------------------------

    private IgniteConfiguration prepareIgniteConfiguration() {
        org.apache.ignite.configuration.IgniteConfiguration igniteConfiguration = new org.apache.ignite.configuration.IgniteConfiguration();

        // enable compute calls from thin client
        ThinClientConfiguration thinClientConfiguration = new ThinClientConfiguration();
        thinClientConfiguration.setMaxActiveComputeTasksPerConnection(100);

        igniteConfiguration.setClientMode(false);
        igniteConfiguration.setMetricsLogFrequency(0);  // DISABLE IGNITE METRICS
        igniteConfiguration.setClientConnectorConfiguration(new ClientConnectorConfiguration().setThinClientConfiguration(thinClientConfiguration)); // enable client connector

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

        // Using port 47401 to separate this cluster from the minion one
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
