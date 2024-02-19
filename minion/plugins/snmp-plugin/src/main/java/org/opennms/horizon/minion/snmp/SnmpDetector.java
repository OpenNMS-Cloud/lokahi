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
package org.opennms.horizon.minion.snmp;

import com.google.protobuf.Any;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import org.opennms.horizon.minion.plugin.api.ServiceDetector;
import org.opennms.horizon.shared.snmp.SnmpAgentConfig;
import org.opennms.horizon.shared.snmp.SnmpHelper;
import org.opennms.horizon.shared.snmp.SnmpObjId;
import org.opennms.inventory.types.ServiceType;
import org.opennms.node.scan.contract.ServiceResult;
import org.opennms.snmp.contract.SnmpDetectorRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SnmpDetector implements ServiceDetector {

    private static final String DEFAULT_OBJECT_IDENTIFIER = ".1.3.6.1.2.1.1.2.0";
    private static final Logger log = LoggerFactory.getLogger(SnmpDetector.class);
    private final SnmpHelper snmpHelper;

    public SnmpDetector(SnmpHelper snmpHelper) {
        this.snmpHelper = snmpHelper;
    }

    @Override
    public CompletableFuture<ServiceResult> detect(String host, Any config) {

        try {
            if (!config.is(SnmpDetectorRequest.class)) {
                throw new IllegalArgumentException("config must be an SnmpRequest; type-url=" + config.getTypeUrl());
            }

            SnmpDetectorRequest snmpDetectorRequest = config.unpack(SnmpDetectorRequest.class);
            // Retrieve agentConfig
            SnmpAgentConfig agentConfig = SnmpConfigUtils.mapAgentConfig(host, snmpDetectorRequest.getAgentConfig());

            SnmpObjId snmpObjectId = SnmpObjId.get(DEFAULT_OBJECT_IDENTIFIER);

            return snmpHelper
                    .getAsync(agentConfig, new SnmpObjId[] {snmpObjectId})
                    .handle((snmpValues, throwable) -> getResponse(host, throwable))
                    .completeOnTimeout(getErrorResult(host), agentConfig.getTimeout(), TimeUnit.MILLISECONDS);

        } catch (IllegalArgumentException e) {
            log.debug("Invalid SNMP Criteria during detection of interface {}", host, e);
            return CompletableFuture.completedFuture(getErrorResult(host));
        } catch (Exception e) {
            log.debug("Unexpected exception during SNMP detection of interface {}", host, e);
            return CompletableFuture.completedFuture(getErrorResult(host));
        }
    }

    private ServiceResult getResponse(String host, Throwable throwable) {
        if (throwable != null) {
            return getErrorResult(host);
        }
        return getDetectedResult(host);
    }

    private ServiceResult getDetectedResult(String host) {
        return ServiceResult.newBuilder()
                .setService(ServiceType.SNMP)
                .setIpAddress(host)
                .setStatus(true)
                .build();
    }

    private ServiceResult getErrorResult(String host) {
        return ServiceResult.newBuilder()
                .setService(ServiceType.SNMP)
                .setIpAddress(host)
                .setStatus(false)
                .build();
    }
}
