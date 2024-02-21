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
package org.opennms.horizon.flows.integration;

import org.opennms.horizon.flows.document.TenantLocationSpecificFlowDocumentLog;

/**
 * Persistence interface for flows.
 *
 * After parsing and processing of received flows, the result is passed to all exposed instances of this interface.
 */
public interface FlowRepository {

    /**
     * Persist a batch of flows.
     *
     * @param enrichedFlows the flows which should be persisted
     *
     * @throws FlowException on any error happening during processing.
     */
    void persist(TenantLocationSpecificFlowDocumentLog enrichedFlows) throws FlowException;
}
