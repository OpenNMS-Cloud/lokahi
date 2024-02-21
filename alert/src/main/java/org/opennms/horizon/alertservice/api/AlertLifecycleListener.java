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
package org.opennms.horizon.alertservice.api;

import org.opennms.horizon.alerts.proto.Alert;

/**
 * Used to be notified of updates/changes made to the set of alerts.
 *
 * Implementation should register the listeners with the {@link AlertService} to receive callbacks.
 *
 * Listeners are invoked serially and the implementors should avoid blocking when possible.
 */
public interface AlertLifecycleListener {
    /**
     * Called when an alert has been created or updated.
     *
     * @param alert a newly created or updated alert
     */
    void handleNewOrUpdatedAlert(Alert alert);

    /**
     * Called when an alert has been deleted.
     *
     * @param alert the deleted alert
     */
    void handleDeletedAlert(Alert alert);
}
