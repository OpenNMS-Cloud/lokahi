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

package org.opennms.horizon.shared.ipc.sink.common;

import java.util.Objects;

import org.opennms.horizon.shared.ipc.sink.aggregation.AggregatingMessageDispatcher;
import org.opennms.horizon.shared.ipc.sink.api.AsyncDispatcher;
import org.opennms.horizon.shared.ipc.sink.api.MessageDispatcher;
import org.opennms.horizon.shared.ipc.sink.api.MessageDispatcherFactory;
import org.opennms.horizon.shared.ipc.sink.api.SendQueue;
import org.opennms.horizon.shared.ipc.sink.api.SendQueueFactory;
import org.opennms.horizon.shared.ipc.sink.api.SinkModule;
import org.opennms.horizon.shared.ipc.sink.api.SyncDispatcher;
import org.osgi.framework.ServiceRegistration;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.MetricSet;
import com.codahale.metrics.Timer.Context;
import com.google.protobuf.Message;

import io.opentracing.Scope;
import io.opentracing.Tracer;

/**
 * This class does all the hard work of building and maintaining the state of the message
 * dispatchers so that concrete implementations only need to focus on dispatching the messages.
 * <p>
 * Different types of dispatchers are created based on whether or not the module is using aggregation.
 * <p>
 * Asynchronous dispatchers use a queue and a thread pool to delegate to a suitable synchronous dispatcher.
 *
 * @param <W> type of module specific state or meta-data, use <code>Void</code> if none is used
 * @author jwhite
 */
public abstract class AbstractMessageDispatcherFactory<W> implements MessageDispatcherFactory {

    private ServiceRegistration<MetricSet> metricsServiceRegistration = null;

    protected abstract <S extends Message, T extends Message> void dispatch(SinkModule<S, T> module, W metadata, byte[] message);

    public abstract Tracer getTracer();

    public abstract MetricRegistry getMetrics();

    /**
     * Invokes dispatch within a timer context.
     */
    private <S extends Message, T extends Message> void timedDispatch(DispatcherState<W, S, T> state, byte[] message) {
        try (Context ctx = state.getDispatchTimer().time();
             Scope scope = getTracer().buildSpan(state.getModule().getId()).startActive(true)) {
            dispatch(state.getModule(), state.getMetaData(), message);
        }
    }

    /**
     * Optionally build meta-data or state information for the module which will
     * be passed on all the calls to {@link #dispatch}.
     * <p>
     * This is useful for calculating things like message headers which are
     * re-used on every dispatch.
     *
     * @param module
     * @return
     */
    public <S extends Message, T extends Message> W getModuleMetadata(SinkModule<S, T> module) {
        return null;
    }

    @Override
    public <S extends Message, T extends Message> SyncDispatcher<S> createSyncDispatcher(SinkModule<S, T> module) {
        Objects.requireNonNull(module, "module cannot be null");

        final var state = new DispatcherState<>(AbstractMessageDispatcherFactory.this, module);

        final var dispatcher = createMessageDispatcher(state,
            message -> AbstractMessageDispatcherFactory.this.timedDispatch(state, message));

        return new SyncDispatcher<>() {

            @Override
            public void send(S message) throws InterruptedException {
                dispatcher.dispatch(message);
            }

            @Override
            public void close() throws Exception {
                dispatcher.close();
                state.close();
            }
        };
    }

    @Override
    public <S extends Message, T extends Message> AsyncDispatcher<S> createAsyncDispatcher(SinkModule<S, T> module) {
        Objects.requireNonNull(module, "module cannot be null");
        Objects.requireNonNull(module.getAsyncPolicy(), "module must have an AsyncPolicy");

        final DispatcherState<W, S, T> state = new DispatcherState<>(this, module);

        return new AsyncDispatcherImpl<>(state, this.getSendQueueFactory(), message -> this.timedDispatch(state, message));
    }

    protected abstract SendQueueFactory getSendQueueFactory();

    protected static class DirectDispatcher<S extends Message, T extends Message> extends MessageDispatcher<S, T> {

        private final DispatcherState<?, S, T> state;

        protected DirectDispatcher(final DispatcherState<?, S, T> state,
                                   final Sender sender) {
            super(state, sender);
            this.state = Objects.requireNonNull(state);
        }

        @SuppressWarnings("unchecked")
        @Override
        public void dispatch(final S message) throws InterruptedException {
            // We assume that S == T so we can send single messages directly
            final var marshalled = this.state.getModule().marshalSingleMessage(message);
            this.send(marshalled);
        }

        @Override
        public void close() throws Exception {
            this.state.close();
        }
    }

    public static <S extends Message, T extends Message> MessageDispatcher<S, T> createMessageDispatcher(
        final DispatcherState<?, S, T> state,
        final MessageDispatcher.Sender sender
    ) {
        if (state.getModule().getAggregationPolicy() != null) {
            // Aggregate the message before dispatching them
            return new AggregatingMessageDispatcher<>(state, sender);
        } else {
            // No aggregation strategy is set, dispatch directly to reduce overhead
            return new AbstractMessageDispatcherFactory.DirectDispatcher<>(state, sender);
        }
    }
}
