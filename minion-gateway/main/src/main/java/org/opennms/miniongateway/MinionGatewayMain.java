package org.opennms.miniongateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ImportResource;

@SpringBootConfiguration
@SpringBootApplication
@ImportResource("classpath:/ignite-cache-config.xml")
public class MinionGatewayMain {

    public static void main(String[] args) {
        SpringApplication.run(MinionGatewayMain.class, args);
    }
}
