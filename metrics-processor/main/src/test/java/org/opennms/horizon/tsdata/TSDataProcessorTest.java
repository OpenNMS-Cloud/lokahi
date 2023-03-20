package org.opennms.horizon.tsdata;

import com.google.protobuf.Any;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatcher;
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
import org.opennms.taskset.contract.MonitorResponse;
import org.opennms.taskset.contract.MonitorType;
import org.opennms.taskset.contract.SyntheticTransactionMetadata;
import org.opennms.taskset.contract.TaskContext;
import org.opennms.taskset.contract.TaskResult;
import org.opennms.taskset.contract.TaskSetResults;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import prometheus.PrometheusTypes.Label;
import prometheus.PrometheusTypes.TimeSeries.Builder;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
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


    // ensure that nested metadata properties are properly extracted
    @Test
    void testMonitorResultMetadataPropagation() throws Exception {
        List<TaskResult> taskResultList = new ArrayList<>();

        TaskContext taskContext = TaskContext.newBuilder()
            .setNodeId(10)
            .setSyntheticTransaction(SyntheticTransactionMetadata.newBuilder()
                .setSyntheticTransactionId(20)
                .setSyntheticTestId(30).build()
            )
            .build();

        MonitorResponse monitorResponse = MonitorResponse.newBuilder()
            .setResponseTimeMs(1000.0)
            .setMonitorType(MonitorType.ICMP)
            .build();
        taskResultList.add(TaskResult.newBuilder()
            .setMonitorResponse(monitorResponse)
            .setContext(taskContext)
            .build());

        TaskSetResults taskSetResults = TaskSetResults.newBuilder()
            .addAllResults(taskResultList)
            .build();

        Map<String, Object> headers = new HashMap<>();
        headers.put(GrpcConstants.TENANT_ID_KEY, "opennms-prime");

        processor.consume(taskSetResults.toByteArray(), headers);

        verify(cortexTSS, timeout(5000).only()).store(anyString(), and(
            new HasLabelWithValue("node_id", "10"),
            new HasLabelWithValue("synthetic_transaction_id", "20"),
            new HasLabelWithValue("synthetic_test_id", "30")
        ));

    }

    static <T> T and(ArgumentMatcher<T> ... matchers) {
        argThat((ArgumentMatcher<T>) arg -> {
            for (ArgumentMatcher<T> eval : matchers) {
                if (!eval.matches(arg)) {
                    return false;
                }
            }
            return true;
        });
        return null;
    }

    static class HasLabelWithValue implements ArgumentMatcher<Builder> {

        private final String label;
        private final String value;

        public HasLabelWithValue(String label, String value) {
            this.label = label;
            this.value = value;
        }

        @Override
        public boolean matches(Builder argument) {
            List<Label> labels = argument.getLabelsList();
            for (Label metricLabel : labels) {
                if (label.equals(metricLabel.getName())) {
                    return metricLabel.getValue().equals(value);
                }
            }
            return false;
        }
    }
}
