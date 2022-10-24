package org.opennms.miniongateway.router;

import org.apache.ignite.Ignite;
import org.opennms.horizon.shared.ipc.grpc.server.manager.MinionManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MinionLookupServiceConfig {
    @Autowired
    MinionManager minionManager;

    @Bean(MinionLookupServiceImpl.IGNITE_SERVICE_NAME)
    public MinionLookupService minionLookupService(@Autowired Ignite ignite) {
        MinionLookupService minionLookupService = new MinionLookupServiceImpl(ignite);
        minionManager.addMinionListener(minionLookupService);
        return minionLookupService;
    }
}
