package org.opennms.horizon.testtool.miniongateway.wiremock.rest;

import org.opennms.cloud.grpc.minion.Identity;
import org.opennms.horizon.testtool.miniongateway.wiremock.api.MockGrpcServiceApi;
import org.opennms.horizon.testtool.miniongateway.wiremock.api.MockTwinHandler;
import org.opennms.horizon.testtool.miniongateway.wiremock.api.SinkMessageDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.TreeMap;

@RestController
@RequestMapping(path = "/api")
public class MinionGatewayWiremockRestController {

    private static final Logger DEFAULT_LOGGER = LoggerFactory.getLogger(MinionGatewayWiremockRestController.class);

    private Logger log = DEFAULT_LOGGER;

    @Autowired
    private MockTwinHandler mockTwinHandler;

    @Autowired
    private MockGrpcServiceApi mockGrpcServiceApi;

    @PostMapping(
        path = "/twin-publish/{topic}",
        consumes = MimeTypeUtils.APPLICATION_JSON_VALUE
    )
    public String publishTwinUpdate(@RequestBody String bodyText, @PathVariable("topic") String topic) {
        log.info("HAVE twin update: topic={};", topic);

        mockTwinHandler.publish(topic, bodyText.getBytes(StandardCharsets.UTF_8));

        return bodyText;
    }

    @GetMapping(
        path="/minions",
        produces = MimeTypeUtils.APPLICATION_JSON_VALUE
    )
    public Object getConnectedMinionList() {
        var minions = mockGrpcServiceApi.getConnectedMinions();
        return minions.stream().map(this::minionIdentityToMap);
    }

    /** Returns the received flows. */
    @GetMapping(
        path="/sinkMessages",
        produces = MimeTypeUtils.APPLICATION_JSON_VALUE
    )
    public Object getSinkMessages() {
        return mockGrpcServiceApi
            .getReceivedSinkMessages()
            .stream()
            .map(SinkMessageDto::from);
    }

//========================================
// Internals
//----------------------------------------

    // PROTOBUF-to-JSON workaround
    private Map<String, String> minionIdentityToMap(Identity identity) {
        Map<String, String> result = new TreeMap<>();

        result.put("systemId", identity.getSystemId());

        return result;
    }
}
