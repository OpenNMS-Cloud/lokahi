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
package org.opennms.miniongateway.detector.server;

import java.net.InetAddress;
import java.util.concurrent.CompletableFuture;
import org.opennms.miniongateway.detector.api.LocalDetectorAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// TODO: implement the real logic and rename once this is no longer a stub
public class LocalDetectorAdapterStubImpl implements LocalDetectorAdapter {

    private static final Logger DEFAULT_LOGGER = LoggerFactory.getLogger(LocalDetectorAdapterStubImpl.class);

    private Logger log = DEFAULT_LOGGER;

    @Override
    public CompletableFuture<Boolean> detect(
            String location,
            String systemId,
            String serviceName,
            String detectorName,
            InetAddress inetAddress,
            Integer nodeId) {

        log.warn("STUBBED DETECTOR - NEED TO ROUTE TO MINION VIA GRPC");

        return CompletableFuture.supplyAsync(() -> {
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
            }
            log.info("################## CompletableFuture test! returning true");
            return true;
        });
    }
}
