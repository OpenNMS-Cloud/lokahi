package org.opennms.miniongateway.grpc.twin;

import org.opennms.cloud.grpc.minion.TwinRequestProto;
import org.opennms.cloud.grpc.minion.TwinResponseProto;

/**
 * A high level interface for OpenNMS digital twin concept working on top of RPC mechanism.
 */
public interface TwinProvider {

    TwinResponseProto getTwinResponse(TwinRequestProto twinRequest);

}
