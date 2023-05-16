package org.opennms.horizon.metrics;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(properties = {"grpc.flow-ingestor.url=test"})
class TsdataprocessorApplicationTests {

	@Test
	void contextLoads() {
	}

}
