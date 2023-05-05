/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2016-2016 The OpenNMS Group, Inc.
 * OpenNMS(R) is Copyright (C) 1999-2016 The OpenNMS Group, Inc.
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
 *******************************************************************************/

package org.opennms.horizon.shared.ipc.sink.api;

import java.util.Objects;
import java.util.function.Consumer;

import org.opennms.horizon.shared.ipc.sink.common.AbstractMessageDispatcherFactory;
import org.opennms.horizon.shared.ipc.sink.common.DispatcherState;

import com.google.protobuf.Message;

public abstract class MessageDispatcher<S extends Message, T extends Message> implements AutoCloseable {

    private final DispatcherState<?, S, T> state;

    private final Sender sender;

    protected MessageDispatcher(final DispatcherState<?, S, T> state,
                                final Sender sender) {
        this.state = Objects.requireNonNull(state);
        this.sender = Objects.requireNonNull(sender);
    }

    public abstract void dispatch(final S message) throws InterruptedException;

    protected void send(final byte[] message) throws InterruptedException {
        this.sender.send(message);
    }

    public SinkModule<S, T> getModule() {
        return this.state.getModule();
    }

    @Override
    public void close() throws Exception {
        this.state.close();
    }

    @FunctionalInterface
    public interface Sender {
        void send(final byte[] message) throws InterruptedException;
    }
}
