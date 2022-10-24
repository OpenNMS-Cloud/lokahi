package org.opennms.miniongateway.ignitedetector;

import javax.annotation.PostConstruct;
import org.apache.ignite.Ignite;
import org.opennms.horizon.shared.ipc.grpc.server.manager.RpcRequestDispatcher;
import org.opennms.miniongateway.detector.api.LocalDetectorAdapter;
import org.opennms.miniongateway.detector.server.IgniteRpcRequestDispatcher;
import org.opennms.miniongateway.detector.server.LocalDetectorAdapterStubImpl;
import org.opennms.miniongateway.grpc.server.tasks.EchoRoutingTask;
import org.opennms.miniongateway.ignite.LocalIgniteRpcRequestDispatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class IgniteDetectorConfig {
    private final Logger logger = LoggerFactory.getLogger(IgniteDetectorConfig.class);
    @Autowired
    private Ignite ignite;

    @Bean("localDetectorAdapter")
    public LocalDetectorAdapter localDetectorAdapter() {
        return new LocalDetectorAdapterStubImpl();
    }

    @Bean("igniteRpcRequestDispatcher")
    public IgniteRpcRequestDispatcher requestDispatcher(RpcRequestDispatcher requestDispatcher) {
        return new LocalIgniteRpcRequestDispatcher(requestDispatcher);
    }

    @PostConstruct
    void deployTask() {
        ignite.compute().localDeployTask(EchoRoutingTask.class, getClass().getClassLoader());
        logger.info("Deployed routing task");
    }
}
