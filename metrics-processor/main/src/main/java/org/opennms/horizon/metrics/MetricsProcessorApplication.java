package org.opennms.horizon.metrics;

import com.codahale.metrics.MetricRegistry;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.opennms.horizon.flows.grpc.client.InventoryClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication(scanBasePackages = "org.opennms.horizon.*")
public class MetricsProcessorApplication {
	public static void main(String[] args) {
		SpringApplication.run(MetricsProcessorApplication.class, args);
	}

	@Bean
    public MetricRegistry metricRegistry(){
	    return new MetricRegistry();
    }

    @Value("${grpc.url.inventory}")
    private String inventoryGrpcAddress;
    @Value("${grpc.server.deadline:60000}")
    private long deadline;

    @Bean
    public ManagedChannel createInventoryChannel() {
        return ManagedChannelBuilder.forTarget(inventoryGrpcAddress)
            .keepAliveWithoutCalls(true)
            .usePlaintext().build();
    }

    @Bean(destroyMethod = "shutdown", initMethod = "initialStubs")
    public InventoryClient createInventoryClient(ManagedChannel channel) {
        return new InventoryClient(channel, deadline);
    }
}
