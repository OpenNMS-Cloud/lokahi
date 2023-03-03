package org.opennms.horizon.inventory.testtool.miniongateway.wiremock.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.protobuf.Any;
import com.google.protobuf.Descriptors;
import com.google.protobuf.GeneratedMessageV3;
import com.google.protobuf.InvalidProtocolBufferException;
import org.opennms.horizon.inventory.testtool.miniongateway.wiremock.api.ServiceApiForMinionGatewayWiremock;
import org.opennms.icmp.contract.IcmpDetectorRequest;
import org.opennms.icmp.contract.IcmpMonitorRequest;
import org.opennms.node.scan.contract.NodeScanRequest;
import org.opennms.sink.flows.contract.FlowsConfig;
import org.opennms.sink.traps.contract.TrapConfig;
import org.opennms.snmp.contract.SnmpCollectorRequest;
import org.opennms.snmp.contract.SnmpDetectorRequest;
import org.opennms.snmp.contract.SnmpMonitorRequest;
import org.opennms.taskset.service.contract.UpdateTasksRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

@RestController
@RequestMapping(path = "/api")
public class MinionGatewayWiremockRestController {

    public static final String GOOGLE_PROTOBUF_TYPE_PREFIX = "type.googleapis.com/";

    private static final Logger DEFAULT_LOGGER = LoggerFactory.getLogger(MinionGatewayWiremockRestController.class);

    private Logger LOG = DEFAULT_LOGGER;

    @Autowired
    private ServiceApiForMinionGatewayWiremock mockGrpcServiceApi;

    private Map<String, Class<? extends GeneratedMessageV3>> googleTypeUrlMap =
        Map.of(
            GOOGLE_PROTOBUF_TYPE_PREFIX + IcmpMonitorRequest.getDescriptor().getFullName(), IcmpMonitorRequest.class,
            GOOGLE_PROTOBUF_TYPE_PREFIX + SnmpMonitorRequest.getDescriptor().getFullName(), SnmpMonitorRequest.class,

            GOOGLE_PROTOBUF_TYPE_PREFIX + IcmpDetectorRequest.getDescriptor().getFullName(), IcmpDetectorRequest.class,
            GOOGLE_PROTOBUF_TYPE_PREFIX + SnmpDetectorRequest.getDescriptor().getFullName(), SnmpDetectorRequest.class,

            GOOGLE_PROTOBUF_TYPE_PREFIX + SnmpCollectorRequest.getDescriptor().getFullName(), SnmpCollectorRequest.class,

            GOOGLE_PROTOBUF_TYPE_PREFIX + NodeScanRequest.getDescriptor().getFullName(), NodeScanRequest.class,
            GOOGLE_PROTOBUF_TYPE_PREFIX + FlowsConfig.getDescriptor().getFullName(), FlowsConfig.class,
            GOOGLE_PROTOBUF_TYPE_PREFIX + TrapConfig.getDescriptor().getFullName(), TrapConfig.class
        );


    @GetMapping(
        path = "/taskset/updates",
        produces = MimeTypeUtils.APPLICATION_JSON_VALUE
    )
    public Object getTaskSetUpdates() {

        List<UpdateTasksRequest> records = mockGrpcServiceApi.getReceivedTaskSetUpdates();
        List<Map<String, Object>> result = records.stream().map((record) -> convertProtobufToMap(record)).collect(Collectors.toList());

        return result;
    }

//========================================
// Internals
//----------------------------------------

    /**
     * Given a protobuf message, convert to a Map so it can be serialized to JSON easily.
     *
     * NOTE: there may be some cases (field types) not supported here yet.
     *
     * @param messageV3
     * @return
     */
    private Map<String, Object> convertProtobufToMap(com.google.protobuf.GeneratedMessageV3 messageV3) {
        // If the message is an Any, decode the Any intelligently
        if (messageV3 instanceof Any) {
            return convertAnyToMap((Any) messageV3);
        }

        // Not an any, use introspection to decode the fields
        Map<String, Object> result = new TreeMap<>();

        for (Map.Entry<Descriptors.FieldDescriptor, Object> field : messageV3.getAllFields().entrySet()) {
            String fieldName = field.getKey().getName();
            Object value = field.getValue();

            // Map the value if it is a message or List
            Object mappedValue = value;
            if (value instanceof GeneratedMessageV3) {
                mappedValue = convertProtobufToMap((GeneratedMessageV3) value);
            } else if (value instanceof Descriptors.EnumValueDescriptor) {
                Descriptors.EnumValueDescriptor evd = (Descriptors.EnumValueDescriptor) value;
                mappedValue = evd.getName();
            } else if (value instanceof List) {
                List listValue = (List) value;

                mappedValue = listValue.stream().map(this::convertObjectIfProtobuf).collect(Collectors.toList());
            }

            result.put(fieldName, mappedValue);
        }

        return result;
    }

    /**
     * Convert an Any protobuf message type to its fields intelligently, if the "packed" type is known.
     *
     * @param any
     * @return
     */
    private Map<String, Object> convertAnyToMap(Any any) {
        Map<String, Object> result;

        // Get the class for this any
        Class<? extends GeneratedMessageV3> clazz = googleTypeUrlMap.get(any.getTypeUrl());
        if (clazz != null) {
            try {
                result = convertProtobufToMap(any.unpack(clazz));
                result.put("typeUrl", any.getTypeUrl());
            } catch (InvalidProtocolBufferException ipbExc) {
                throw new RuntimeException("failed to unpack Any contents", ipbExc);
            }
        } else {
            result = new HashMap<>();
            result.put("__ANY__", true);
            result.put("typeUrl", any.getTypeUrl());
            result.put("error", "failed to decode - type url unrecognized");
            result.put("raw", any.getValue().toByteArray());
        }

        return result;
    }

    /**
     * Convert the give object to a Map if it is a protobuf message object; otherwise, just keep the original value.
     *
     * @param value
     * @return
     */
    private Object convertObjectIfProtobuf(Object value) {
        if (value instanceof GeneratedMessageV3) {
            return this.convertProtobufToMap((GeneratedMessageV3) value);
        }

        return value;
    }
}
