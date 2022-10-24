package org.opennms.horizon.minion.taskset.worker.ignite;

import org.apache.ignite.Ignite;
import org.apache.ignite.Ignition;
import org.apache.ignite.internal.IgnitionEx;
import org.apache.ignite.internal.processors.resource.GridSpringResourceContextImpl;
import org.opennms.horizon.minion.taskset.worker.ignite.resource.ApplicationContextAdapter;
import org.opennms.horizon.minion.taskset.worker.ignite.resource.BlueprintBeanRegistry;
import org.osgi.framework.BundleContext;
import org.osgi.service.blueprint.container.BlueprintContainer;

public class IgnitionFactory {
    public static Ignite create(WorkerIgniteConfiguration workerIgniteConfiguration, BundleContext bundleContext, BlueprintContainer container) throws Exception {
        return IgnitionEx.start(workerIgniteConfiguration.prepareIgniteConfiguration(),
            new GridSpringResourceContextImpl(new ApplicationContextAdapter(new BlueprintBeanRegistry(container)))
        );
    }

}
