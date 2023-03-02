package org.opennms.horizon.inventory.testtool.miniongateway.wiremock.rest;

import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;
import com.google.protobuf.util.JsonFormat;
import org.opennms.horizon.inventory.testtool.miniongateway.wiremock.api.ProtobufConstants;
import org.opennms.horizon.inventory.testtool.miniongateway.wiremock.api.ServiceApiForMinionGatewayWiremock;
import org.opennms.taskset.service.contract.UpdateTasksRequest;
import org.opennms.taskset.service.contract.UpdateTasksRequestList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping(path = "/api")
public class MinionGatewayWiremockRestController {

    private static final Logger DEFAULT_LOGGER = LoggerFactory.getLogger(MinionGatewayWiremockRestController.class);

    private Logger LOG = DEFAULT_LOGGER;

    @Autowired
    private ServiceApiForMinionGatewayWiremock mockGrpcServiceApi;

    @GetMapping(
        path = "/taskset/updates",
        produces = MimeTypeUtils.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<String> getTaskSetUpdates() throws InvalidProtocolBufferException {

        List<UpdateTasksRequest> records = mockGrpcServiceApi.getReceivedTaskSetUpdates();

        UpdateTasksRequestList updateTasksRequestList =
            UpdateTasksRequestList.newBuilder()
                .addAllUpdateTasksRequest(records)
                .build();

        String jsonText = marshalProtobufToJson(updateTasksRequestList);

        return ResponseEntity.ok(jsonText);
    }

//========================================
// Internals
//----------------------------------------

    private String marshalProtobufToJson(Message message) throws InvalidProtocolBufferException {
        JsonFormat.TypeRegistry typeRegistry =
            JsonFormat.TypeRegistry.newBuilder()
                .add(ProtobufConstants.PROTOBUF_TYPE_LIST)
                .build();

        return JsonFormat.printer().usingTypeRegistry(typeRegistry).print(message);
    }

}
