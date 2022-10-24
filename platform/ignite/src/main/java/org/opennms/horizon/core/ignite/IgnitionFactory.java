package org.opennms.horizon.core.ignite;

import org.apache.ignite.Ignite;
import org.apache.ignite.Ignition;
import org.apache.ignite.client.IgniteClient;

// TODO: merge with one used by the minion
public class IgnitionFactory {
    public static Ignite create(WorkerIgniteConfiguration workerIgniteConfiguration) {
        return Ignition.start(workerIgniteConfiguration.prepareIgniteConfiguration());
    }
    public static IgniteClient createClient(IgniteClientConfiguration workerIgniteConfiguration) {
        return Ignition.startClient(workerIgniteConfiguration.prepareIgniteClientConfiguration());
    }

}
