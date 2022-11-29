package org.opennms.horizon.inventory.component;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.opennms.horizon.inventory.service.taskset.response.DetectorResponseService;
import org.opennms.taskset.contract.DetectorResponse;
import org.opennms.taskset.contract.TaskResult;
import org.opennms.taskset.contract.TaskSetResults;

import java.util.HashMap;
import java.util.Map;

import static org.mockito.Mockito.times;

@RunWith(MockitoJUnitRunner.class)
public class TaskSetResultsConsumerTest {

    private static final String TEST_LOCATION = "Default";
    private static final String TEST_TENANT_ID = "opennms-prime";

    @InjectMocks
    private TaskSetResultsConsumer consumer;

    @Mock
    private DetectorResponseService service;

    @Test
    public void testReceiveMessage() {

        DetectorResponse response = DetectorResponse.newBuilder().build();

        TaskResult taskResult = TaskResult.newBuilder()
            .setLocation(TEST_LOCATION)
            .setDetectorResponse(response)
            .build();

        TaskSetResults results = TaskSetResults.newBuilder()
            .addResults(taskResult).build();

        Map<String, Object> headers = new HashMap<>();
        headers.put(Constants.TENANT_ID_KEY, TEST_TENANT_ID.getBytes());

        consumer.receiveMessage(results.toByteArray(), headers);

        Mockito.verify(service, times(1))
            .accept(TEST_TENANT_ID, TEST_LOCATION, response);
    }
}
