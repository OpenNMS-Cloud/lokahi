package org.opennms.horizon.minion.taskset.worker.impl;

import com.google.common.collect.Sets;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
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

public class TaskSetLifecycleManagerImpl implements TaskSetLifecycleManager {

    public static final String SERVICE_NAME_PREFIX = "task:";
    public static final String TASK_SERVICE_CACHE_NAME = "minion.task-service";

    private static final Logger DEFAULT_LOGGER = LoggerFactory.getLogger(TaskSetLifecycleManagerImpl.class);

    private Logger log = DEFAULT_LOGGER;

    @Getter
    @Setter
    private Ignite ignite;

//========================================
// Processing
//----------------------------------------

    @Override
    public int deploy(TaskSet taskSet) {
        log.info("About to deploy task set {}", taskSet);
        // Take the snapshot of currently running services.
        Collection<ServiceDescriptor> serviceDescriptorList = ignite.services().serviceDescriptors();

        // Deploy the services that run on every node in the cluster.
        // WARNING: very large numbers of these node singletons will impact startup performance notably due to the
        //  slowness with starting Ignite services one at a time.  Unfortunately, the "node singleton" cannot be started
        //  via deployAllAsync().
        List<String> singletonIds = deployNodeSingletonServices(taskSet);

        // Prepare the services that run on only 1 node across the cluster
        List<ServiceConfiguration> serviceConfigurationList = prepareOnePerClusterServiceConfigurations(taskSet);

        // Deploy
        ignite.services().deployAllAsync(serviceConfigurationList).listen(in -> {
          log.info("Deployed all task definitions from task set {}", taskSet);
        });

        // Find the set of services that are no longer needed
        Collection<String> canceledServices =
            calculateServicesToUndeploy(serviceConfigurationList, singletonIds, serviceDescriptorList);
        ignite.services().cancelAllAsync(canceledServices);

        // Log the update summary
        log.info("Completed task set update: deploy-count={}; cancel-count={}",
            serviceConfigurationList.size() + singletonIds.size(),
            canceledServices.size());
        return canceledServices.size();
    }

    public void close() {
        ignite.close();
    }

//========================================
// Internals
//----------------------------------------

    /**
     * Deploy all of the services for workflow definitions that need to run as Node Singletons (i.e. always 1 running
     *  per node).
     *
     * @param workflowDefinitions
     */
    private List<String> deployNodeSingletonServices(TaskSet workflowDefinitions) {
        List<String> deployedServices = new LinkedList<>();

        int count = 0;
        for (TaskDefinition task : workflowDefinitions.getTaskDefinitionList()) {
            if (task.getType().equals(TaskType.LISTENER)) {
                count++;
                String serviceName = deployOneNodeSingletonService(task);
                deployedServices.add(serviceName);
            }
        }

        return deployedServices;
    }

    private String deployOneNodeSingletonService(TaskDefinition taskDefinition) {
        String serviceName = formulateServiceNameForTaskSet(taskDefinition);
        TaskExecutorIgniteService workflowExecutorIgniteService = new TaskExecutorIgniteService(taskDefinition);

        ignite.services().deployNodeSingletonAsync(serviceName, workflowExecutorIgniteService);

        return serviceName;
    }

    private List<ServiceConfiguration> prepareOnePerClusterServiceConfigurations(TaskSet taskSet) {
        List<ServiceConfiguration> serviceConfigurationList =
            taskSet.getTaskDefinitionList()
                .stream()
                .filter(this::isSingletonTask)
                .map((taskDefinition) -> {
                        TaskExecutorIgniteService workflowExecutorIgniteService = new TaskExecutorIgniteService(taskDefinition);
                        ServiceConfiguration serviceConfiguration = prepareServiceConfiguration(taskDefinition, workflowExecutorIgniteService);

                        return serviceConfiguration;
                    }
                )
                .collect(Collectors.toList());

        return serviceConfigurationList;
    }

    private boolean isSingletonTask(TaskDefinition taskDefinition) {
        switch (taskDefinition.getType()) {
            case MONITOR:
            case CONNECTOR:
                return true;

            default:
                return false;
        }
    }

    private String formulateServiceNameForTaskSet(TaskDefinition taskDefinition) {
        return SERVICE_NAME_PREFIX + taskDefinition.getId();
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

    private Collection<String>
    calculateServicesToUndeploy(
        List<ServiceConfiguration> deployed,
        List<String> deployedSingletonIds,
        Collection<ServiceDescriptor> serviceDescriptorList) {

        Set<String> deployedNames = deployed.stream().map(ServiceConfiguration::getName).collect(Collectors.toSet());
        deployedNames.addAll(deployedSingletonIds);

        Set<String> existingNames =
            serviceDescriptorList.stream().
                map(ServiceDescriptor::name)
                .filter(name -> name.startsWith(SERVICE_NAME_PREFIX))   // Only workflow services
                .collect(Collectors.toSet());

        return Sets.difference(existingNames, deployedNames);
    }
}
