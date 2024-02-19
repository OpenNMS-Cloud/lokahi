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

import com.google.protobuf.Empty;
import io.grpc.stub.StreamObserver;
import java.util.LinkedList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestEmptyMessageHandler implements StreamObserver<Empty> {

    private static final Logger DEFAULT_LOGGER = LoggerFactory.getLogger(TestEmptyMessageHandler.class);

    private Logger LOG = DEFAULT_LOGGER;

    private static final Object lock = new Object();

    private List<Empty> receivedList = new LinkedList<>();

    // ========================================
    // StreamObserver Interface
    // ----------------------------------------

    @Override
    public void onNext(Empty value) {
        LOG.error("HAVE UNEXPECTED EMPTY MESSAGE");

        synchronized (lock) {
            receivedList.add(value);
        }
    }

    @Override
    public void onError(Throwable t) {}

    @Override
    public void onCompleted() {}

    // ========================================
    // Test Data Access
    // ----------------------------------------

    public Empty[] getReceivedMessagesSnapshot() {
        synchronized (lock) {
            return receivedList.toArray(new Empty[0]);
        }
    }
}
