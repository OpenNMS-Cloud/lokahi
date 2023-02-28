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
}
