package org.opennms.miniongateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootConfiguration
@SpringBootApplication
public class MinionGatewayMain {

    public static void main(String[] args) {
        SpringApplication.run(MinionGatewayMain.class, args);
    }
}
