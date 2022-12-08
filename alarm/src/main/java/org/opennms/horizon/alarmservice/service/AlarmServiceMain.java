package org.opennms.horizon.alarmservice.service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootConfiguration
@SpringBootApplication
@ComponentScan(basePackages = "org.opennms.horizon.alarmservice")
@EnableJpaRepositories(basePackages = "org.opennms.horizon.alarmservice.db.repository")
@EntityScan(basePackages = "org.opennms.horizon.alarmservice.db.entity")
public class AlarmServiceMain {

    public static void main(String[] args) {
        SpringApplication.run(AlarmServiceMain.class, args);
    }
}
