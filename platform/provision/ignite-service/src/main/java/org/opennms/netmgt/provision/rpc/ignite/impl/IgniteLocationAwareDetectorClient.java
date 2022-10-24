package org.opennms.netmgt.provision.rpc.ignite.impl;

import lombok.Getter;
import lombok.Setter;
import org.apache.ignite.client.IgniteClient;
import org.opennms.netmgt.provision.DetectorRequestExecutorBuilder;
import org.opennms.netmgt.provision.LocationAwareDetectorClient;

@Getter
@Setter
public class IgniteLocationAwareDetectorClient implements LocationAwareDetectorClient {

    private IgniteClient igniteClient;

    @Override
    public DetectorRequestExecutorBuilder detect() {
        return null;
//        return new IgniteDetectorRequestExecutorBuilder(igniteClient);
    }
}
