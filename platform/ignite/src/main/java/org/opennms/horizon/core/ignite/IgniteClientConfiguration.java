package org.opennms.horizon.core.ignite;

import org.apache.ignite.client.ThinClientKubernetesAddressFinder;
import org.apache.ignite.configuration.ClientConfiguration;
import org.apache.ignite.kubernetes.configuration.KubernetesConnectionConfiguration;

public class IgniteClientConfiguration {

    private final boolean useKubernetes;
    private final String kubernetesServiceName;
    private final String addresses;

    public IgniteClientConfiguration(boolean useKubernetes, String kubernetesServiceName, String addresses) {
        this.useKubernetes = useKubernetes;
        this.kubernetesServiceName = kubernetesServiceName;
        this.addresses = addresses;
    }

    public ClientConfiguration prepareIgniteClientConfiguration() {
        ClientConfiguration configuration = new ClientConfiguration();
        if (useKubernetes) {
            KubernetesConnectionConfiguration connectionConfiguration = new KubernetesConnectionConfiguration();
            connectionConfiguration.setServiceName(kubernetesServiceName);
            configuration.setAddressesFinder(new ThinClientKubernetesAddressFinder(connectionConfiguration));
        } else {
            configuration.setAddresses(addresses);
        }
        return configuration;
    }
}
