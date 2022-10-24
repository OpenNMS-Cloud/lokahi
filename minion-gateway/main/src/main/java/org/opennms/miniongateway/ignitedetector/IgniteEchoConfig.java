package org.opennms.miniongateway.ignitedetector;

import org.opennms.miniongateway.detector.api.LocalEchoAdapter;
import org.opennms.miniongateway.detector.server.LocalEchoAdapterImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class IgniteEchoConfig {
    @Bean("localEchoAdapter")
    public LocalEchoAdapter localEchoAdapter() {
        return new LocalEchoAdapterImpl();
    }
}
