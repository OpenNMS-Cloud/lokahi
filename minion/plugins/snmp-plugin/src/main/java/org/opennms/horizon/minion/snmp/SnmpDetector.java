package org.opennms.horizon.minion.snmp;

import com.google.protobuf.Any;
import com.google.protobuf.Descriptors;
import org.opennms.horizon.minion.plugin.api.ServiceDetector;
import org.opennms.horizon.minion.plugin.api.ServiceDetectorResponse;
import org.opennms.horizon.minion.plugin.api.ServiceDetectorResponseImpl;
import org.opennms.horizon.shared.snmp.SnmpAgentConfig;
import org.opennms.horizon.shared.snmp.SnmpConfiguration;
import org.opennms.horizon.shared.snmp.SnmpHelper;
import org.opennms.horizon.shared.snmp.SnmpObjId;
import org.opennms.snmp.contract.SnmpDetectorRequest;
import org.opennms.taskset.contract.MonitorType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class SnmpDetector implements ServiceDetector {
    private static final String DEFAULT_OBJECT_IDENTIFIER = ".1.3.6.1.2.1.1.2.0";
    private static final Logger log = LoggerFactory.getLogger(SnmpDetector.class);
    private static final String SNMP_DETECTION_TIMED_OUT = "SNMP Detection Timed Out";
    private final SnmpHelper snmpHelper;
    private final Descriptors.FieldDescriptor retriesFieldDescriptor;
    private final Descriptors.FieldDescriptor timeoutFieldDescriptor;

    public SnmpDetector(SnmpHelper snmpHelper) {
        this.snmpHelper = snmpHelper;

        Descriptors.Descriptor snmpDetectorRequestDescriptor = SnmpDetectorRequest.getDefaultInstance().getDescriptorForType();

        retriesFieldDescriptor = snmpDetectorRequestDescriptor.findFieldByNumber(SnmpDetectorRequest.RETRIES_FIELD_NUMBER);
        timeoutFieldDescriptor = snmpDetectorRequestDescriptor.findFieldByNumber(SnmpDetectorRequest.TIMEOUT_FIELD_NUMBER);
    }

    @Override
    public CompletableFuture<ServiceDetectorResponse> detect(Any config) {

        String hostAddress = null;

        try {
            if (!config.is(SnmpDetectorRequest.class)) {
                throw new IllegalArgumentException("config must be an SnmpRequest; type-url=" + config.getTypeUrl());
            }

            SnmpDetectorRequest snmpDetectorRequest = config.unpack(SnmpDetectorRequest.class);
            SnmpDetectorRequest effectiveSnmpDetectorRequest = populateDefaultsAsNeeded(snmpDetectorRequest);
            hostAddress = effectiveSnmpDetectorRequest.getHost();

            SnmpAgentConfig agentConfig = getAgentConfig(effectiveSnmpDetectorRequest);

            SnmpObjId snmpObjectId = SnmpObjId.get(DEFAULT_OBJECT_IDENTIFIER);

            return snmpHelper.getAsync(agentConfig, new SnmpObjId[]{snmpObjectId})
                .handle((snmpValues, throwable) -> getResponse(effectiveSnmpDetectorRequest, throwable))
                .completeOnTimeout(getErrorResponse(hostAddress, SNMP_DETECTION_TIMED_OUT),
                    agentConfig.getTimeout(), TimeUnit.MILLISECONDS);

        } catch (IllegalArgumentException e) {
            log.debug("Invalid SNMP Criteria during detection of interface {}", hostAddress, e);
            return CompletableFuture.completedFuture(getErrorResponse(hostAddress, e.getMessage()));
        } catch (Exception e) {
            log.debug("Unexpected exception during SNMP detection of interface {}", hostAddress, e);
            return CompletableFuture.completedFuture(getErrorResponse(hostAddress, e.getMessage()));
        }
    }

    private SnmpDetectorRequest populateDefaultsAsNeeded(SnmpDetectorRequest request) {
        SnmpDetectorRequest.Builder requestBuilder = SnmpDetectorRequest.newBuilder(request);

        if (!request.hasField(retriesFieldDescriptor)) {
            requestBuilder.setRetries(SnmpConfiguration.DEFAULT_RETRIES);
        }

        if (!request.hasField(timeoutFieldDescriptor)) {
            requestBuilder.setTimeout(SnmpConfiguration.DEFAULT_TIMEOUT);
        }

        return requestBuilder.build();
    }

    public SnmpAgentConfig getAgentConfig(SnmpDetectorRequest request) throws UnknownHostException {
        SnmpConfiguration configuration = new SnmpConfiguration();
        configuration.setTimeout(request.getTimeout());
        configuration.setRetries(request.getRetries());

        InetAddress host = InetAddress.getByName(request.getHost());

        return new SnmpAgentConfig(host, configuration);
    }

    private ServiceDetectorResponse getResponse(SnmpDetectorRequest request, Throwable throwable) {
        if (throwable != null) {
            return getErrorResponse(request.getHost(), throwable.getMessage());
        }
        return getDetectedResponse(request.getHost());
    }

    private ServiceDetectorResponse getDetectedResponse(String host) {
        return ServiceDetectorResponseImpl.builder()
            .monitorType(MonitorType.SNMP)
            .ipAddress(host)
            .serviceDetected(true)
            .build();
    }

    private ServiceDetectorResponse getErrorResponse(String host, String reason) {
        return ServiceDetectorResponseImpl.builder()
            .monitorType(MonitorType.SNMP)
            .ipAddress(host)
            .serviceDetected(false)
            .reason(reason)
            .build();
    }
}
