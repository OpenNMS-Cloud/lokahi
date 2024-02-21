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
package org.opennms.horizon.shared.ipc.sink.api;

import com.google.protobuf.Message;
import java.util.Objects;
import org.opennms.horizon.shared.ipc.sink.common.DispatcherState;

public abstract class MessageDispatcher<S extends Message, T extends Message> implements AutoCloseable {

    private final DispatcherState<?, S, T> state;

    private final Sender sender;

    protected MessageDispatcher(final DispatcherState<?, S, T> state, final Sender sender) {
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
