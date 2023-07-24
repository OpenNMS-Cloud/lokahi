package org.opennms.miniongateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ImportResource;

import com.codahale.metrics.MetricRegistry;

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
}
