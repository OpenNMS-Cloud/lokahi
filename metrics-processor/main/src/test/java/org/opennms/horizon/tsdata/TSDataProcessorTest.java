package org.opennms.horizon.tsdata;

import com.google.protobuf.Any;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opennms.horizon.azure.api.AzureResponseMetric;
import org.opennms.horizon.azure.api.AzureResultMetric;
import org.opennms.horizon.azure.api.AzureValueMetric;
import org.opennms.horizon.azure.api.AzureValueType;
import org.opennms.horizon.shared.constants.GrpcConstants;
import org.opennms.horizon.timeseries.cortex.CortexTSS;
import org.opennms.taskset.contract.CollectorResponse;
import org.opennms.taskset.contract.MonitorType;
import org.opennms.taskset.contract.TaskResult;
import org.opennms.taskset.contract.TaskSetResults;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class TSDataProcessorTest {

    @Mock
    private CortexTSS cortexTSS;

    @InjectMocks
    private TSDataProcessor processor;

    @Test
    void testConsumeAzure() throws Exception {

        List<TaskResult> taskResultList = new ArrayList<>();

        List<AzureResultMetric> azureResultMetrics = new ArrayList<>();
        azureResultMetrics.add(AzureResultMetric.newBuilder()
            .setResourceName("resource-name")
            .setResourceGroup("resource-group")
            .setAlias("netInBytes")
            .setValue(AzureValueMetric.newBuilder()
                .setType(AzureValueType.INT64)
                .setUint64(1234L)
                .build())
            .build());

        CollectorResponse collectorResponse = CollectorResponse.newBuilder()
            .setResult(Any.pack(AzureResponseMetric.newBuilder()
                .addAllResults(azureResultMetrics)
                .build()))
            .setMonitorType(MonitorType.AZURE)
            .build();
        taskResultList.add(TaskResult.newBuilder()
            .setCollectorResponse(collectorResponse)
            .build());

        TaskSetResults taskSetResults = TaskSetResults.newBuilder()
            .addAllResults(taskResultList)
            .build();

        Map<String, Object> headers = new HashMap<>();
        headers.put(GrpcConstants.TENANT_ID_KEY, "opennms-prime");

        processor.consume(taskSetResults.toByteArray(), headers);

        verify(cortexTSS, timeout(5000).only()).store(anyString(), any(prometheus.PrometheusTypes.TimeSeries.Builder.class));

    }
}
