package org.opennms.horizon.minion.grpc.queue;

class LinkedHydraTest extends HydraTest {

    @Override
    protected Hydra<Integer> spawn() {
        return new LinkedHydra<>();
    }
}
