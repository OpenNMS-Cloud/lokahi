package org.opennms.horizon.inventory.testtool.miniongateway.wiremock;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication()
@SpringBootConfiguration
public class MinionGatewayWiremockMain {
    public static void main(String[] args) {
        SpringApplication.run(MinionGatewayWiremockMain.class, args);
    }
}
