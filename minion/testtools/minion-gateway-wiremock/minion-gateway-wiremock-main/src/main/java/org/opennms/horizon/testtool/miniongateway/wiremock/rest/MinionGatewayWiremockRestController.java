/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2022-2023 The OpenNMS Group, Inc.
 * OpenNMS(R) is Copyright (C) 1999-2023 The OpenNMS Group, Inc.
 *
 * OpenNMS(R) is a registered trademark of The OpenNMS Group, Inc.
 *
 * OpenNMS(R) is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
 *
 * OpenNMS(R) is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with OpenNMS(R).  If not, see:
 *      http://www.gnu.org/licenses/
 *
 * For more information contact:
 *     OpenNMS(R) Licensing <license@opennms.org>
 *     http://www.opennms.org/
 *     http://www.opennms.com/
 *******************************************************************************/

package org.opennms.horizon.testtool.miniongateway.wiremock.rest;

import org.opennms.cloud.grpc.minion.Identity;
import org.opennms.cloud.grpc.minion.SinkMessage;
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
        path = "/twin-publish/{topic}/{location}",
        consumes = MimeTypeUtils.APPLICATION_JSON_VALUE
    )
    public String publishTwinUpdate(@RequestBody String bodyText, @PathVariable("topic") String topic, @PathVariable("location") String location) {
        log.info("HAVE twin update: topic={}; location={}", topic, location);

        mockTwinHandler.publish(location, topic, bodyText.getBytes(StandardCharsets.UTF_8));

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
        result.put("location", identity.getLocation());

        return result;
    }
}
