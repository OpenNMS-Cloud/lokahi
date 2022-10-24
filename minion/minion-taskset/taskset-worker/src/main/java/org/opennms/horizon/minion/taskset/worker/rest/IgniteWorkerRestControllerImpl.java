package org.opennms.horizon.minion.taskset.worker.rest;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import javax.ws.rs.core.Response;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.ignite.Ignite;
import org.apache.ignite.services.ServiceDescriptor;

@AllArgsConstructor
@Slf4j
public class IgniteWorkerRestControllerImpl implements IgniteWorkerRestController {

    //TODO: should this be injected through ignite annotation?
    private Ignite ignite;

    @Override
    public Response reportServiceDeploymentMetrics(boolean verbose) {
        Map<String, Object> result = calculateServiceDeploymentMetrics(verbose);

        return Response.ok(result).build();
    }

//========================================
// Internals
//----------------------------------------

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
}
