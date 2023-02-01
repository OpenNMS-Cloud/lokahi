/*
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2022 The OpenNMS Group, Inc.
 * OpenNMS(R) is Copyright (C) 1999-2022 The OpenNMS Group, Inc.
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
 */

package org.opennms.tooling.ignitetool.rest;

import org.apache.ignite.Ignite;
import org.apache.ignite.cluster.ClusterNode;
import org.apache.ignite.services.ServiceDescriptor;
import org.opennms.taskset.contract.TaskSet;
import org.opennms.tooling.ignitetool.message.IgniteMessageConsumerManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import static org.opennms.taskset.service.api.TaskSetPublisher.TASK_SET_PUBLISH_SERVICE;

@SuppressWarnings("rawtypes")
@RestController
@RequestMapping("/ignite")
public class IgniteToolRestController {

    private static final Logger DEFAULT_LOGGER = LoggerFactory.getLogger(IgniteToolRestController.class);

    private Logger log = DEFAULT_LOGGER;

    @Autowired
    private Ignite ignite;

    @Autowired
    private IgniteMessageConsumerManager igniteMessageConsumerManager;

    @GetMapping("/hello")
    public String hello() {
        return "hello";
    }

    @GetMapping(path = "/service-deployment/metrics", produces = MimeTypeUtils.APPLICATION_JSON_VALUE)
    public Map<String, Object> reportServiceDeploymentMetrics(boolean verbose) {
        Map<String, Object> result = calculateServiceDeploymentMetrics(verbose);

        return result;
    }

    @PostMapping(path = "/message/{topic}")
    public void postMessageToIgniteTopic(@PathVariable("topic") String topic, @RequestBody String content) {
        ignite.message().send(topic, content);
    }

    @PutMapping(path = "/message/{topic}/listener")
    public void createMessageListener(@PathVariable("topic") String topic) {
        igniteMessageConsumerManager.startListenMessages(topic, (msg) -> this.igniteMessageLogger(topic, msg));
    }

    @DeleteMapping(path = "/message/{topic}/listener")
    public void removeMessageListener(@PathVariable("topic") String topic) {
        igniteMessageConsumerManager.stopListenMessages(topic);
    }

    @PostMapping(path = "/task-set/{tenant-id}/{location}", consumes = MimeTypeUtils.APPLICATION_JSON_VALUE)
    public void publishTaskSet(@PathVariable("tenant-id") String tenantId, @PathVariable("location") String location, @RequestBody TaskSet taskSet) {

    }

    @GetMapping(path = "/topology/{version}")
    public Map getTopology(@PathVariable("version") String version) {
        long versionNumber = parseTopologyVersion(version);

        Collection<ClusterNode> topology = ignite.cluster().topology(versionNumber);

        Map result = new TreeMap();
        result.put("topologyVersion", versionNumber);
        result.put("detail", topology);
        result.put("summary", summarizeTopology(topology));

        return result;
    }

//========================================
// Internals
//----------------------------------------

    private long parseTopologyVersion(String versionString) {
        long versionNumber;
        if (
                (versionString == null) ||
                (versionString.isEmpty()) ||
                (versionString.equalsIgnoreCase("latest")) ||
                (versionString.equals("-"))
        ) {
            versionNumber = ignite.cluster().topologyVersion();
        } else {
            versionNumber = Long.parseLong(versionString);
        }

        return versionNumber;
    }

    private String formatElapsedTime(long firstTimestamp, long secondTimestamp) {
        long diffNano = secondTimestamp - firstTimestamp;
        long diffSec = diffNano / 1000000000L;
        long diffRemainingMilli = ( diffNano / 1000000L ) % 1000L;

        return diffSec + "s " + diffRemainingMilli + "ms";
    }

    private Map<String, Object> calculateServiceDeploymentMetrics(boolean includeByService) {
        Map<String, Integer> countsByIgniteNode = new HashMap<>();
        Map<String, Integer> countsByService = new HashMap<>();
        AtomicInteger total = new AtomicInteger(0);

        Collection<ServiceDescriptor> serviceDescriptors = ignite.services().serviceDescriptors();
        serviceDescriptors.forEach(serviceDescriptor -> {
            Map<UUID, Integer> topo = serviceDescriptor.topologySnapshot();
            AtomicInteger subtotal = new AtomicInteger(0);

            for (Map.Entry<UUID, Integer> topoEntry : topo.entrySet()) {
                countsByIgniteNode.compute(String.valueOf(topoEntry.getKey()), (key, curVal) -> {

                    total.addAndGet(topoEntry.getValue());
                    subtotal.addAndGet(topoEntry.getValue());

                    if (curVal != null) {
                        return curVal + topoEntry.getValue();
                    } else {
                        return topoEntry.getValue();
                    }
                });
            }

            countsByService.put(serviceDescriptor.name(), subtotal.get());
        });

        // Sort
        Map<String, Integer> sortedCountsByIgniteNode = new TreeMap<>(countsByIgniteNode);
        Map<String, Integer> sortedServices = new TreeMap<>(countsByService);

        Map<String, Object> top = new TreeMap<>();
        top.put("countsByIgniteNode", sortedCountsByIgniteNode);

        if (includeByService) {
            top.put("countsByService", sortedServices);
        }

        top.put("total", total.get());
        top.put("serviceCount", serviceDescriptors.size());

        return top;
    }

    private void igniteMessageLogger(String topic, Object content) {
        log.info("MESSAGE RECEIVED: topic={}; content={}", topic, content);
    }

    private Map summarizeTopology(Collection<ClusterNode> nodes) {
        Map result = new TreeMap();

        result.put("node-count", nodes.size());

        Map summaryPerNode = new TreeMap();
        nodes.forEach(node -> {
            Map oneNodeSummary = new TreeMap();
            oneNodeSummary.put("addresses", node.addresses());
            oneNodeSummary.put("hostnames", node.hostNames());

            summaryPerNode.put(node.id(), oneNodeSummary);
        });

        result.put("nodes", summaryPerNode);

        return result;
    }
}
