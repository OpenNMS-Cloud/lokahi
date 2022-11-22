package org.opennms.miniongatewaygrpcproxy;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootConfiguration
@SpringBootApplication
public class MinionGatewayGrpcProxyMain {

    public static void main(String[] args) {
        SpringApplication.run(MinionGatewayGrpcProxyMain.class, args);
    }
}
