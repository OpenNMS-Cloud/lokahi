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
package org.opennms.horizon.minion.ipc.twin.whiteboard.core.internal;

import static org.opennms.horizon.minion.ipc.twin.api.TwinListener.MESSAGE_LISTENER_TOPIC;

import com.savoirtech.eos.pattern.whiteboard.AbstractWhiteboard;
import com.savoirtech.eos.util.ServiceProperties;
import java.io.Closeable;
import java.io.IOException;
import java.util.function.Consumer;
import org.opennms.horizon.minion.ipc.twin.api.TwinListener;
import org.opennms.horizon.minion.ipc.twin.api.TwinSubscriber;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TwinListenerWhiteboard extends AbstractWhiteboard<TwinListener, Closeable> {

    private final Logger logger = LoggerFactory.getLogger(TwinListenerWhiteboard.class);
    private final TwinSubscriber twinSubscriber;

    public TwinListenerWhiteboard(BundleContext bundleContext, TwinSubscriber twinSubscriber) {
        super(bundleContext, TwinListener.class);
        this.twinSubscriber = twinSubscriber;
    }

    @Override
    protected Closeable addService(TwinListener service, ServiceProperties props) {
        Class<?> payload = service.getType();
        String subscriberKey = props.getProperty(MESSAGE_LISTENER_TOPIC);

        if (payload == null) {
            logger.warn("Subscriber {} does not specify proper payload type, ignoring", service);
            return null;
        }
        if (subscriberKey == null) {
            logger.warn(
                    "Subscriber {} for payload {} does not specify proper subscriber key, ignoring",
                    service,
                    payload.getName());
            return null;
        }

        return twinSubscriber.subscribe(subscriberKey, payload, new Consumer() {
            @Override
            public void accept(Object message) {
                if (payload.isInstance(message)) {
                    service.accept(message);
                } else {
                    logger.warn("Message {} is not a valid object required by subscriber {}.", message, subscriberKey);
                }
            }

            @Override
            public String toString() {
                return "closeable consumer for " + service;
            }
        });
    }

    @Override
    protected void removeService(TwinListener service, Closeable tracked) {
        try {
            tracked.close();
        } catch (IOException e) {
            logger.warn("Error while closing up grpc subscription for {}", service, e);
        }
    }
}
