/*
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2022 The OpenNMS Group, Inc.
 * OpenNMS(R) is Copyright (C) 1999-2022 The OpenNMS Group, Inc.
 *
 * OpenNMS(R) is a registered trademark of The OpenNMS Group, Inc.
 *
 * OpenNMS(R) is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
 *
 * OpenNMS(R) is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with OpenNMS(R).  If not, see:
 *      http://www.gnu.org/licenses/
 *
 * For more information contact:
 *     OpenNMS(R) Licensing <license@opennms.org>
 *     http://www.opennms.org/
 *     http://www.opennms.com/
 */

package org.opennms.core.ipc.grpc.server.manager.adapter;

import io.grpc.stub.StreamObserver;
import org.opennms.core.ipc.grpc.common.RpcResponseProto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Consumer;

/**
 * Stream Observer that handles inbound RPC calls initiated by the Minion.
 */
public class InboundRpcAdapter implements StreamObserver<RpcResponseProto> {

    private static final Logger DEFAULT_LOGGER = LoggerFactory.getLogger(InboundRpcAdapter.class);

    private Logger log = DEFAULT_LOGGER;

    private final Consumer<RpcResponseProto> onMessage;
    private final Consumer<Throwable> onError;
    private final Runnable onCompleted;

    public InboundRpcAdapter(Consumer<RpcResponseProto> onMessage, Consumer<Throwable> onError, Runnable onCompleted) {
        this.onMessage = onMessage;
        this.onError = onError;
        this.onCompleted = onCompleted;
    }

    @Override
    public void onNext(RpcResponseProto rpcResponseProto) {
        onMessage.accept(rpcResponseProto);
    }

    @Override
    public void onError(Throwable thrown) {
        onError(thrown);
    }

    @Override
    public void onCompleted() {
        onCompleted.run();
    }
}
