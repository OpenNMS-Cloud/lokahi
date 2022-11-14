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

            SnmpAgentConfig agentConfig = getAgentConfig(snmpDetectorRequest);

            SnmpObjId snmpObjectId = SnmpObjId.get(DEFAULT_OBJECT_IDENTIFIER);

            return snmpHelper.getAsync(agentConfig, new SnmpObjId[]{snmpObjectId})
                .handle((snmpValues, throwable) -> {
                    String host = effectiveSnmpDetectorRequest.getHost();
                    if (throwable != null) {
                        return getErrorResponse(host, throwable.getMessage());
                    } else {
                        return getDetectedResponse(host);
                    }
                })
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

    private SnmpDetectorRequest populateDefaultsAsNeeded(SnmpDetectorRequest snmpDetectorRequest) {
        SnmpDetectorRequest.Builder requestBuilder = SnmpDetectorRequest.newBuilder(snmpDetectorRequest);

        if (!snmpDetectorRequest.hasField(retriesFieldDescriptor)) {
            requestBuilder.setRetries(SnmpConfiguration.DEFAULT_RETRIES);
        }

        if (!snmpDetectorRequest.hasField(timeoutFieldDescriptor)) {
            requestBuilder.setTimeout(SnmpConfiguration.DEFAULT_TIMEOUT);
        }

        return requestBuilder.build();
    }

    public SnmpAgentConfig getAgentConfig(SnmpDetectorRequest snmpDetectorRequest) throws UnknownHostException {
        SnmpConfiguration configuration = new SnmpConfiguration();
        configuration.setTimeout(snmpDetectorRequest.getTimeout());
        configuration.setRetries(snmpDetectorRequest.getRetries());

        InetAddress host = InetAddress.getByName(snmpDetectorRequest.getHost());

        return new SnmpAgentConfig(host, configuration);
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
