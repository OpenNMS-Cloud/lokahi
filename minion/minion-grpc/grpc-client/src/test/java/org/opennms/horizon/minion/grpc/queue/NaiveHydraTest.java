package org.opennms.horizon.minion.grpc.queue;

class NaiveHydraTest extends HydraTest {

    @Override
    protected Hydra<Integer> spawn() {
        return new NaiveHydra<>();
    }
}
