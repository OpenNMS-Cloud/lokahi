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

import com.google.common.base.Verify;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.opennms.horizon.shared.snmp.SnmpObjId;
import org.opennms.horizon.shared.snmp.SnmpRowResult;
import org.opennms.horizon.shared.snmp.TableTracker;
import org.opennms.horizon.snmp.api.SnmpResponseMetric;
import org.opennms.horizon.snmp.api.SnmpResultMetric;
import org.opennms.snmp.contract.SnmpCollectorPart;
import org.springframework.expression.Expression;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

/**
 * The SnmpIfCollector class is responsible for performing the actual SNMP data
 * collection for a node over a specified network interface. The SnmpIfCollector
 * implements the SnmpHandler class in order to receive notifications when an
 * SNMP reply is received or error occurs.
 * <p>
 * The SnmpIfCollector is provided a list of MIB objects to collect and an
 * interface over which to collect the data. Data collection can be via SNMPv1
 * GetNext requests or SNMPv2 GetBulk requests depending upon the parms used to
 * construct the collector.
 */
public class SnmpTableCollector extends TableTracker {
    private static final SpelExpressionParser PARSER = new SpelExpressionParser();
    private static final Set<String> NUMERIC_TYPES = Set.of("counter", "integer", "gauge", "counter32", "gauge32");

    private final String instance;
    private final List<SnmpCollectorPart.Element> elements;

    private final Expression filterExpression;
    private final SnmpResponseMetric.Builder builder;

    private SnmpTableCollector(
            final String instance,
            final List<SnmpCollectorPart.Element> elements,
            final Expression filterExpression,
            final SnmpResponseMetric.Builder builder) {
        super(elements.stream().map(element -> SnmpObjId.get(element.getOid())).toArray(SnmpObjId[]::new));

        this.instance = Objects.requireNonNull(instance);
        this.elements = Objects.requireNonNull(elements);

        this.filterExpression = filterExpression;

        this.builder = Objects.requireNonNull(builder);
    }

    @Override
    public void rowCompleted(final SnmpRowResult row) {
        Verify.verify(row.getColumnCount() == this.elements.size());

        if (this.filterExpression != null) {
            final var context = new StandardEvaluationContext();

            for (int i = 0; i < row.getColumnCount(); i++) {
                final var element = this.elements.get(i);
                context.setVariable(
                        element.getAlias(),
                        row.getValue(SnmpObjId.get(element.getOid())).toDisplayString());
            }

            context.setVariable("instance", this.instance);

            try {
                final var shouldPersist = this.filterExpression.getValue(context, Boolean.class);
                if (shouldPersist == null || !shouldPersist) {
                    return;
                }
            } catch (final Exception e) {
                return;
            }
        }

        // Find all row entries of type string and build a label collection which is used to create result values for
        // all non-string values
        final var labels = this.elements.stream()
                .filter(element -> Objects.equals(element.getType(), "string"))
                .collect(Collectors.toUnmodifiableMap(
                        SnmpCollectorPart.Element::getAlias,
                        element -> row.getValue(SnmpObjId.get(element.getOid())).toDisplayString()));

        this.elements.stream()
                .filter(element -> NUMERIC_TYPES.contains(element.getType()))
                .forEach(element -> {
                    final var value = row.getResult(SnmpObjId.get(element.getOid()));
                    if (value.isEmpty()) {
                        return;
                    }

                    this.builder.addResults(SnmpResultMetric.newBuilder()
                            .setBase(value.get().getBase().toString())
                            .setInstance(value.get().getInstance().toString())
                            .setAlias(element.getAlias())
                            .setValue(SnmpCollectionSet.mapValue(value.get().getValue()))
                            .putAllLabels(labels));
                });
    }

    public static SnmpTableCollector forTable(
            final SnmpCollectorPart.Table table, final SnmpResponseMetric.Builder builder) {
        final var filterExpression =
                table.hasPersistFilterExpr() ? PARSER.parseExpression(table.getPersistFilterExpr()) : null;

        return new SnmpTableCollector(table.getInstance(), table.getElementList(), filterExpression, builder);
    }
}
