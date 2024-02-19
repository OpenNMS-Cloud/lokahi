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
package org.opennms.horizon.minion.flows.parser.ipfix;

import com.google.common.collect.ImmutableMap;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.jline.utils.Log;
import org.opennms.horizon.minion.flows.parser.Protocol;
import org.opennms.horizon.minion.flows.parser.ie.InformationElementDatabase;
import org.opennms.horizon.minion.flows.parser.ie.Semantics;
import org.opennms.horizon.minion.flows.parser.ie.values.BooleanValue;
import org.opennms.horizon.minion.flows.parser.ie.values.DateTimeValue;
import org.opennms.horizon.minion.flows.parser.ie.values.FloatValue;
import org.opennms.horizon.minion.flows.parser.ie.values.IPv4AddressValue;
import org.opennms.horizon.minion.flows.parser.ie.values.IPv6AddressValue;
import org.opennms.horizon.minion.flows.parser.ie.values.ListValue;
import org.opennms.horizon.minion.flows.parser.ie.values.MacAddressValue;
import org.opennms.horizon.minion.flows.parser.ie.values.OctetArrayValue;
import org.opennms.horizon.minion.flows.parser.ie.values.SignedValue;
import org.opennms.horizon.minion.flows.parser.ie.values.StringValue;
import org.opennms.horizon.minion.flows.parser.ie.values.UnsignedValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class InformationElementProvider implements InformationElementDatabase.Provider {
    private static final String COLUMN_ID = "ElementID";
    private static final String COLUMN_NAME = "Name";
    private static final String COLUMN_TYPE = "Abstract Data Type";
    private static final String COLUMN_SEMANTICS = "Data Type Semantics";

    private static final CSVFormat CSV_FORMAT =
            CSVFormat.newFormat(',').withQuote('"').withEscape('\\').withFirstRecordAsHeader();
    private static final Logger LOG = LoggerFactory.getLogger(InformationElementProvider.class);

    private static final Map<String, Semantics> SEMANTICS_LOOKUP = ImmutableMap.<String, Semantics>builder()
            .put("default", Semantics.DEFAULT)
            .put("quantity", Semantics.QUANTITY)
            .put("totalCounter", Semantics.TOTAL_COUNTER)
            .put("deltaCounter", Semantics.DELTA_COUNTER)
            .put("identifier", Semantics.IDENTIFIER)
            .put("flags", Semantics.FLAGS)
            .put("list", Semantics.LIST)
            .put("snmpCounter", Semantics.SNMP_COUNTER)
            .put("snmpGauge", Semantics.SNMP_GAUGE)
            .build();

    private static final Map<String, InformationElementDatabase.ValueParserFactory> TYPE_LOOKUP =
            ImmutableMap.<String, InformationElementDatabase.ValueParserFactory>builder()
                    .put("octetArray", OctetArrayValue::parser)
                    .put("unsigned8", UnsignedValue::parserWith8Bit)
                    .put("unsigned16", UnsignedValue::parserWith16Bit)
                    .put("unsigned32", UnsignedValue::parserWith32Bit)
                    .put("unsigned64", UnsignedValue::parserWith64Bit)
                    .put("signed8", SignedValue::parserWith8Bit)
                    .put("signed16", SignedValue::parserWith16Bit)
                    .put("signed32", SignedValue::parserWith32Bit)
                    .put("signed64", SignedValue::parserWith64Bit)
                    .put("float32", FloatValue::parserWith32Bit)
                    .put("float64", FloatValue::parserWith64Bit)
                    .put("boolean", BooleanValue::parser)
                    .put("macAddress", MacAddressValue::parser)
                    .put("string", StringValue::parser)
                    .put("dateTimeSeconds", DateTimeValue::parserWithSeconds)
                    .put("dateTimeMilliseconds", DateTimeValue::parserWithMilliseconds)
                    .put("dateTimeMicroseconds", DateTimeValue::parserWithMicroseconds)
                    .put("dateTimeNanoseconds", DateTimeValue::parserWithNanoseconds)
                    .put("ipv4Address", IPv4AddressValue::parser)
                    .put("ipv6Address", IPv6AddressValue::parser)
                    .put("basicList", ListValue::parserWithBasicList)
                    .put("subTemplateList", ListValue::parserWithSubTemplateList)
                    .put("subTemplateMultiList", ListValue::parserWithSubTemplateMultiList)
                    .build();

    @Override
    public void load(final InformationElementDatabase.Adder adder) {
        try (final Reader r = new InputStreamReader(
                Objects.requireNonNull(this.getClass().getResourceAsStream("/ipfix-information-elements.csv")))) {
            for (final CSVRecord record : CSV_FORMAT.parse(r)) {
                final int id;
                try {
                    id = Integer.parseInt(record.get(COLUMN_ID));
                } catch (final NumberFormatException e) {
                    continue;
                }

                final String name = record.get(COLUMN_NAME);
                final InformationElementDatabase.ValueParserFactory valueParserFactory =
                        TYPE_LOOKUP.get(record.get(COLUMN_TYPE));

                if (valueParserFactory == null) {
                    LOG.info("ValueParserFactory is null.. ");
                    continue;
                }

                final Optional<Semantics> semantics =
                        Optional.ofNullable(SEMANTICS_LOOKUP.get(record.get(COLUMN_SEMANTICS)));

                adder.add(Protocol.IPFIX, id, valueParserFactory, name, semantics);
            }
        } catch (final IOException e) {
            Log.error("Exception while loading InformationElementDatabase. ", e.getMessage());
        }
    }
}
