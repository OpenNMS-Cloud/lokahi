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
package listener.syslog;

import java.util.Optional;


public interface SinkModule<S extends Message, T extends Message> {

    String HEARTBEAT_MODULE_ID = "Heartbeat";

    /**
     * Globally unique identifier.
     *
     * Used in the JMS queue name in the Camel implementation.
     */
    String getId();

    /**
     * The number of threads used to consume from the broker.
     */
    int getNumConsumerThreads();

    /**
     * Marshals the aggregated message to a byte array.
     */
    byte[] marshal(T message);

    /**
     * Unmarshals the aggregated message from a byte array.
     */
    T unmarshal(byte[]  message);


    /**
     * Marshals single message to a byte array.
     */
    byte[]  marshalSingleMessage(S message);

    /**
     * Unmarshals single message from a byte array.
     */
    S unmarshalSingleMessage(byte[]  message);


    /**
     * Thr routing key will be used to ensure all messages of the same group is handled by the same consumer.
     *
     * @param message the message to generate the routing key from
     * @return the routing key or, {@code Optional.empty()} if no routing is required
     */
    default Optional<String> getRoutingKey(T message) {
        return Optional.empty();
    }
}