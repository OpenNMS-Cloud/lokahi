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
package org.opennms.horizon.minion.grpc.client.message;

import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import org.opennms.cloud.grpc.minion.CloudToMinionMessage;
import org.opennms.horizon.minion.grpc.CloudMessageHandler;
import org.opennms.horizon.shared.ipc.rpc.api.minion.CloudMessageReceiver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ThinCloudMessageHandler implements CloudMessageHandler {

    private final Logger logger = LoggerFactory.getLogger(ThinCloudMessageHandler.class);
    private final Set<CloudMessageReceiver> handlers = new CopyOnWriteArraySet<>();

    @Override
    public void handle(CloudToMinionMessage message) {
        if (handlers.isEmpty()) {
            logger.warn("No registered handlers for message {}", message);
            return;
        }

        for (CloudMessageReceiver handler : handlers) {
            if (handler.canHandle(message)) {
                handler.handle(message);
            }
        }
    }

    public void bind(CloudMessageReceiver handler) {
        if (handler != null) {
            handlers.add(handler);
            logger.info("Registered cloud message handler {}", handler);
        }
    }

    public void unbind(CloudMessageReceiver handler) {
        if (handler != null) {
            handlers.remove(handler);
            logger.info("Removing cloud message handler {}", handler);
        }
    }
}
