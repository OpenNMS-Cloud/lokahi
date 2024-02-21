/*
 * Licensed to The OpenNMS Group, Inc (TOG) under one or more
 * contributor license agreements.  See the LICENSE.md file
 * distributed with this work for additional information
 * regarding copyright ownership.
 *
 * TOG licenses this file to You under the GNU Affero General
 * Public License Version 3 (the "License") or (at your option)
 * any later version.  You may not use this file except in
 * compliance with the License.  You may obtain a copy of the
 * License at:
 *
 *      https://www.gnu.org/licenses/agpl-3.0.txt
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied.  See the License for the specific
 * language governing permissions and limitations under the
 * License.
 */
package org.opennms.horizon.shared.ipc.sink.aggregation;

import com.google.protobuf.Message;
import org.opennms.horizon.shared.ipc.sink.api.AggregationPolicy;
import org.opennms.horizon.shared.ipc.sink.api.MessageDispatcher;
import org.opennms.horizon.shared.ipc.sink.api.SinkModule;
import org.opennms.horizon.shared.ipc.sink.common.DispatcherState;

/**
 * A {@link MessageDispatcher} that applies the {@link SinkModule}'s {@link AggregationPolicy}
 * using the {@link Aggregator}.
 *
 * @author jwhite
 */
public class AggregatingMessageDispatcher<S extends Message, T extends Message> extends MessageDispatcher<S, T> {

    private final Aggregator<S, T, ?> aggregator;

    public AggregatingMessageDispatcher(final DispatcherState<?, S, T> state, final Sender sender) {
        super(state, sender);

        this.aggregator =
                new Aggregator<>(state.getModule().getId(), state.getModule().getAggregationPolicy(), this::send);
    }

    @Override
    public void dispatch(final S message) throws InterruptedException {
        this.aggregator.aggregate(message);
    }

    private void send(final T log) throws InterruptedException {
        final var marshalled = this.getModule().marshal(log);
        this.send(marshalled);
    }

    @Override
    public void close() throws Exception {
        super.close();
        this.aggregator.close();
    }
}
