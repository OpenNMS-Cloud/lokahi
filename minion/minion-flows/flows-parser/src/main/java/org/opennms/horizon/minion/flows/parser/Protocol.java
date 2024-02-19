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
package org.opennms.horizon.minion.flows.parser;

import java.util.Objects;
import org.opennms.horizon.minion.flows.parser.proto.Header;

public enum Protocol {
    NETFLOW5(Header.VERSION, "Netflow v5"),
    NETFLOW9(org.opennms.horizon.minion.flows.parser.netflow9.proto.Header.VERSION, "Netflow v9"),
    IPFIX(org.opennms.horizon.minion.flows.parser.ipfix.proto.Header.VERSION, "IPFix");

    public final int version;
    public final String description;

    Protocol(final int version, final String description) {
        this.version = version;
        this.description = Objects.requireNonNull(description);
    }
}
