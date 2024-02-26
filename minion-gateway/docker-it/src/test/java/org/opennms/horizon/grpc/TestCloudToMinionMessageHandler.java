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
package org.opennms.horizon.grpc;

import io.grpc.stub.StreamObserver;
import java.util.LinkedList;
import java.util.List;
import org.opennms.cloud.grpc.minion.CloudToMinionMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestCloudToMinionMessageHandler implements StreamObserver<CloudToMinionMessage> {

    private static final Logger DEFAULT_LOGGER = LoggerFactory.getLogger(TestCloudToMinionMessageHandler.class);

    private Logger LOG = DEFAULT_LOGGER;

    private List<CloudToMinionMessage> receivedMessages = new LinkedList<>();

    private final Object lock = new Object();

    // ========================================
    // StreamObserver Interface
    // ----------------------------------------

    @Override
    public void onNext(CloudToMinionMessage value) {
        LOG.info(
                "RECEIVED CLOUD-TO-MINION MESSAGE; type={}",
                value.hasTwinResponse() ? "TWIN-RESPONSE" : value.getAnyVal().getTypeUrl());

        synchronized (lock) {
            receivedMessages.add(value);
        }
    }

    @Override
    public void onError(Throwable t) {}

    @Override
    public void onCompleted() {}

    // ========================================
    // Test Data Access
    // ----------------------------------------

    public CloudToMinionMessage[] getReceivedMessagesSnapshot() {
        synchronized (lock) {
            return receivedMessages.toArray(new CloudToMinionMessage[0]);
        }
    }
}
