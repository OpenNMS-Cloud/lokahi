package org.opennms.horizon.minion.taskset.worker.ignite;

import org.apache.ignite.Ignite;
import org.apache.ignite.internal.IgnitionEx;
import org.apache.ignite.ioc.internal.processors.resource.GridInjectResourceContextImpl;
import org.apache.ignite.osgi.blueprint.ioc.BlueprintRegistry;
import org.osgi.framework.BundleContext;
import org.osgi.service.blueprint.container.BlueprintContainer;

public class IgnitionFactory {
    public static Ignite create(WorkerIgniteConfiguration workerIgniteConfiguration, BundleContext bundleContext, BlueprintContainer container) throws Exception {
        return IgnitionEx.start(workerIgniteConfiguration.prepareIgniteConfiguration(),
            new GridInjectResourceContextImpl(new BlueprintRegistry(container))
        );
    }

}
