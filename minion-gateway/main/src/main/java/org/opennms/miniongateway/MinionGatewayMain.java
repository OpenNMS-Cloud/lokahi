package org.opennms.miniongateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ImportResource;

import com.codahale.metrics.MetricRegistry;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.OpenTelemetry;

@SpringBootConfiguration
@SpringBootApplication
@ImportResource("classpath:/ignite-cache-config.xml")
public class MinionGatewayMain {

    public static void main(String[] args) {
        SpringApplication.run(MinionGatewayMain.class, args);
    }

    @Bean
    public MetricRegistry metricRegistry(){
        return new MetricRegistry();
    }

    @Bean
    public OpenTelemetry openTelemetry() {
        return GlobalOpenTelemetry.get(); // this gets the SDK that was configured by the Java agent
    }
}
