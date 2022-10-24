package org.opennms.miniongateway.grpc.server;

import org.apache.ignite.Ignite;
import org.opennms.horizon.shared.ipc.grpc.server.manager.MinionManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MinionManagerConfig {
    @Bean("minionManager")
    public MinionManager localDetectorAdapter(Ignite ignite) {
        return new MinionManagerImpl();
    }
}
