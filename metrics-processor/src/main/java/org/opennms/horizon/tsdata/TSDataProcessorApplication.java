package org.opennms.horizon.tsdata;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
//@EnableKafkaStreams
public class TSDataProcessorApplication {
	public static void main(String[] args) {
		SpringApplication.run(TSDataProcessorApplication.class, args);
	}
}
