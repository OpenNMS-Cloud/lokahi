/*
 * Licensed to The OpenNMS Group, Inc (TOG) under one or more
 * contributor license agreements.  See the LICENSE.md file
 * distributed with this work for additional information
 * regarding copyright ownership.
 *
 * TOG licenses this file to You under the GNU Affero General
 * Public License Version 3 (the "License") or (at your option)
 * any later version.  You may not use this file except in
 * compliance with the License.  You may obtain a copy of the
 * License at:
 *
 *      https://www.gnu.org/licenses/agpl-3.0.txt
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied.  See the License for the specific
 * language governing permissions and limitations under the
 * License.
 */
package org.opennms.horizon.minion.taskset.worker.impl;

import com.google.common.collect.Sets;
import java.util.AbstractMap.SimpleEntry;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.Getter;
import lombok.Setter;
import org.apache.ignite.Ignite;
import org.apache.ignite.services.Service;
import org.apache.ignite.services.ServiceConfiguration;
import org.apache.ignite.services.ServiceDescriptor;
import org.opennms.horizon.minion.taskset.worker.TaskSetLifecycleManager;
import org.opennms.taskset.contract.TaskDefinition;
import org.opennms.taskset.contract.TaskSet;
import org.opennms.taskset.contract.TaskType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.DigestUtils;

public class TaskSetLifecycleManagerImpl implements TaskSetLifecycleManager {

    public static final String SERVICE_NAME_PREFIX = "task:";
    public static final String TASK_SERVICE_CACHE_NAME = "minion.task-service";

    private static final Logger DEFAULT_LOGGER = LoggerFactory.getLogger(TaskSetLifecycleManagerImpl.class);

    private Logger log = DEFAULT_LOGGER;

    @Getter
    @Setter
    private Ignite ignite;

    /**
     * Reference to the last TaskSet that was successfully deployed
     */
    private TaskSet deployedTaskSet;

    // ========================================
    // Processing
    // ----------------------------------------

    @Override
    public int deploy(TaskSet taskSet) {
        log.info("About to deploy task set {}", taskSet);
        // Take the snapshot of currently running services.
        Collection<ServiceDescriptor> serviceDescriptorList = ignite.services().serviceDescriptors();

        // Prepare singleton services that run on every node in the cluster.
        // WARNING: very large numbers of these node singletons will impact startup performance notably due to the
        //  slowness with starting Ignite services one at a time.  Unfortunately, the "node singleton" cannot be started
        //  via deployAllAsync().
        // Below code calculates list of node singletons with their names without actual deployment. Launch of services
        // is deferred and executed after termination of earlier services. While this will add delay in activation of
        // these
        // it ensures fewer conflicts with port allocation and similar. Because node singleton services are bound to
        // listeners
        // whose can be partially re-configured old and new configuration might still rely on same port number.
        Map<String, TaskExecutorIgniteService> nodeSingletons = prepareNodeSingletonServices(taskSet);

        // Prepare the services that run on only 1 node across the cluster
        List<ServiceConfiguration> serviceConfigurationList = prepareOnePerClusterServiceConfigurations(taskSet);

        // Deploy
        ignite.services().deployAllAsync(serviceConfigurationList).listen(in -> {
            log.info("Deployed all task definitions from task set {}", taskSet);
        });

        // Find the set of services that are no longer needed
        Collection<String> canceledServices =
                calculateServicesToUndeploy(serviceConfigurationList, nodeSingletons.keySet(), serviceDescriptorList);
        ignite.services().cancelAllAsync(canceledServices).listen(in -> {
            // deploy node singletons only if earlier services are terminated
            log.info("Deploying {} node singletons", nodeSingletons.size());
            for (Entry<String, TaskExecutorIgniteService> entry : nodeSingletons.entrySet()) {
                ignite.services().deployNodeSingletonAsync(entry.getKey(), entry.getValue());
            }
        });

        // Log the update summary
        log.info(
                "Completed task set update: deploy-count={}; cancel-count={}",
                serviceConfigurationList.size() + nodeSingletons.size(),
                canceledServices.size());

        // Save a reference to the TaskSet
        deployedTaskSet = taskSet;

        return canceledServices.size();
    }

    @Override
    public TaskSet getDeployedTaskSet() {
        return deployedTaskSet;
    }

    public void close() {
        ignite.close();
    }

    // ========================================
    // Internals
    // ----------------------------------------

    /**
     * Deploy all of the services for workflow definitions that need to run as Node Singletons (i.e. always 1 running
     *  per node).
     *
     * @param workflowDefinitions
     */
    private Map<String, TaskExecutorIgniteService> prepareNodeSingletonServices(TaskSet workflowDefinitions) {
        Map<String, TaskExecutorIgniteService> preparedNodeSingletons = new LinkedHashMap<>();

        int count = 0;
        for (TaskDefinition task : workflowDefinitions.getTaskDefinitionList()) {
            if (task.getType().equals(TaskType.LISTENER)) {
                count++;
                Entry<String, TaskExecutorIgniteService> entry = prepareNodeSingletonService(task);
                preparedNodeSingletons.put(entry.getKey(), entry.getValue());
            }
        }

        return preparedNodeSingletons;
    }

    private Entry<String, TaskExecutorIgniteService> prepareNodeSingletonService(TaskDefinition taskDefinition) {
        String serviceName = formulateServiceNameForTaskSet(taskDefinition);
        TaskExecutorIgniteService workflowExecutorIgniteService = new TaskExecutorIgniteService(taskDefinition);
        return new SimpleEntry<>(serviceName, workflowExecutorIgniteService);
    }

    private List<ServiceConfiguration> prepareOnePerClusterServiceConfigurations(TaskSet taskSet) {
        List<ServiceConfiguration> serviceConfigurationList = taskSet.getTaskDefinitionList().stream()
                .filter(this::isSingletonTask)
                .map((taskDefinition) -> {
                    TaskExecutorIgniteService workflowExecutorIgniteService =
                            new TaskExecutorIgniteService(taskDefinition);
                    ServiceConfiguration serviceConfiguration =
                            prepareServiceConfiguration(taskDefinition, workflowExecutorIgniteService);

                    return serviceConfiguration;
                })
                .collect(Collectors.toList());

        return serviceConfigurationList;
    }

    private boolean isSingletonTask(TaskDefinition taskDefinition) {
        switch (taskDefinition.getType()) {
            case SCANNER:
            case DETECTOR:
            case MONITOR:
            case CONNECTOR:
            case COLLECTOR:
                return true;

            default:
                return false;
        }
    }

    private String formulateServiceNameForTaskSet(TaskDefinition taskDefinition) {
        String checksum =
                DigestUtils.md5DigestAsHex(taskDefinition.getConfiguration().toByteArray());
        return SERVICE_NAME_PREFIX + taskDefinition.getId() + ":" + checksum;
    }

    private ServiceConfiguration prepareServiceConfiguration(TaskDefinition taskDefinition, Service service) {
        ServiceConfiguration serviceConfiguration = new ServiceConfiguration();
        String serviceName = formulateServiceNameForTaskSet(taskDefinition);

        serviceConfiguration.setName(serviceName);
        serviceConfiguration.setService(service);
        serviceConfiguration.setAffinityKey(taskDefinition.getId());
        serviceConfiguration.setCacheName(TASK_SERVICE_CACHE_NAME);
        serviceConfiguration.setTotalCount(1);

        return serviceConfiguration;
    }

    private Collection<String> calculateServicesToUndeploy(
            Collection<ServiceConfiguration> deployed,
            Collection<String> plannedSingletonIds,
            Collection<ServiceDescriptor> serviceDescriptorList) {

        Set<String> deployedNames =
                deployed.stream().map(ServiceConfiguration::getName).collect(Collectors.toSet());
        deployedNames.addAll(plannedSingletonIds);

        Set<String> existingNames = serviceDescriptorList.stream()
                .map(ServiceDescriptor::name)
                .filter(name -> name.startsWith(SERVICE_NAME_PREFIX)) // Only workflow services
                .collect(Collectors.toSet());

        return Sets.difference(existingNames, deployedNames);
    }
}
