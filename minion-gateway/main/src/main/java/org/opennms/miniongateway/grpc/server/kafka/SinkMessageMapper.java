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
package org.opennms.miniongateway.grpc.server.kafka;

import com.google.protobuf.Message;

/**
 * Mapper for messages sent by minion via sink channels.
 *
 * @param <I> Minion message.
 * @param <O> Backend message.
 */
public interface SinkMessageMapper<I extends Message, O extends Message> {

    /**
     * Maps given message into backend message.
     *
     * @param tenantId Tenant for which message was generated.
     * @param locationId Location for which message was generated.
     * @param message Message sent by minion.
     * @return Mapped message which should embed both tenantId and locationId.
     */
    O map(String tenantId, String locationId, I message);
}
