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
package org.opennms.horizon.alertservice.service;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import org.opennms.horizon.alertservice.api.AlertLifecycleListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Helper class used to track and interact with {@link AlertLifecycleListener}s.
 */
@Service
public class AlertListenerRegistry {
    private static final Logger LOG = LoggerFactory.getLogger(AlertListenerRegistry.class);

    private final List<AlertLifecycleListener> listeners = new CopyOnWriteArrayList<>();

    public void addListener(AlertLifecycleListener listener) {
        listeners.add(listener);
    }

    public void removeListener(AlertLifecycleListener listener) {
        listeners.remove(listener);
    }

    public void forEachListener(Consumer<AlertLifecycleListener> callback) {
        for (AlertLifecycleListener listener : listeners) {
            try {
                callback.accept(listener);
            } catch (Exception e) {
                LOG.error("Error occurred while invoking listener: {}. Skipping.", listener, e);
            }
        }
    }
}
