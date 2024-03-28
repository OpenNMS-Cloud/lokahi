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

import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.opennms.horizon.shared.snmp.AggregateTracker;
import org.opennms.horizon.shared.snmp.Collectable;
import org.opennms.horizon.shared.snmp.SingleInstanceTracker;
import org.opennms.horizon.shared.snmp.SnmpInstId;
import org.opennms.horizon.shared.snmp.SnmpObjId;
import org.opennms.horizon.shared.snmp.SnmpResult;
import org.opennms.horizon.snmp.api.SnmpResponseMetric;
import org.opennms.horizon.snmp.api.SnmpResultMetric;
import org.opennms.snmp.contract.SnmpCollectorPart;

public class SnmpScalarCollector extends AggregateTracker {

    private final Map<SnmpObjId, String> aliases;

    private final SnmpResponseMetric.Builder builder;

    private SnmpScalarCollector(
            final Collection<? extends Collectable> children,
            final Map<SnmpObjId, String> aliases,
            final SnmpResponseMetric.Builder builder) {
        super(children);

        this.aliases = Objects.requireNonNull(aliases);

        this.builder = Objects.requireNonNull(builder);
    }

    @Override
    protected void storeResult(final SnmpResult result) {
        final var alias = this.aliases.get(result.getAbsoluteInstance());

        this.builder.addResults(SnmpResultMetric.newBuilder()
                .setBase(result.getBase().toString())
                .setInstance(result.getInstance().toString())
                .setAlias(alias)
                .setValue(SnmpCollectionSet.mapValue(result.getValue())));
    }

    public static SnmpScalarCollector forScalar(
            final SnmpCollectorPart.Scalar scalar, final SnmpResponseMetric.Builder builder) {
        final var children = scalar.getElementList().stream()
                .map(element -> {
                    final var oid = SnmpObjId.get(element.getOid());

                    final var base = oid.parent();
                    final var instance = new SnmpInstId(oid.getLastSubId());

                    return new SingleInstanceTracker(base, instance);
                })
                .toList();

        final var aliases = scalar.getElementList().stream()
                .collect(Collectors.toMap(
                        element -> new SnmpObjId(element.getOid()), SnmpCollectorPart.Element::getAlias));

        return new SnmpScalarCollector(children, aliases, builder);
    }
}
