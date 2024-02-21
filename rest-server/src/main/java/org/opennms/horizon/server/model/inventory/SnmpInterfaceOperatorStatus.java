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
package org.opennms.horizon.server.model.inventory;

import java.util.HashMap;
import java.util.Map;

public enum SnmpInterfaceOperatorStatus {
    UP(1),
    DOWN(2),
    TESTING(3),
    UNKNOWN(4),
    DORMANT(5),
    NOT_PRESENT(6),
    LOWER_LAYER_DOWN(7);

    public final int value;
    private static final Map<Integer, SnmpInterfaceOperatorStatus> MAP = new HashMap<>();

    SnmpInterfaceOperatorStatus(int value) {
        this.value = value;
    }

    static {
        for (SnmpInterfaceOperatorStatus status : SnmpInterfaceOperatorStatus.values()) {
            MAP.put(status.getValue(), status);
        }
    }

    public static SnmpInterfaceOperatorStatus valueOf(int value) {
        return MAP.get(value);
    }

    public int getValue() {
        return value;
    }
}
