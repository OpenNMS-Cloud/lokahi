package org.opennms.horizon.minion.grpc.queue;

class NaïveHydraTest extends HydraTest {

    @Override
    protected Hydra<Integer> spawn() {
        return new NaïveHydra<>();
    }
}
