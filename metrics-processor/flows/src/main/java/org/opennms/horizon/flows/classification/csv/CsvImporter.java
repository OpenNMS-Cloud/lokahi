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
package org.opennms.horizon.flows.classification.csv;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.opennms.horizon.flows.classification.persistence.api.Rule;

public class CsvImporter {

    private CsvImporter() {}

    public static List<Rule> parseCSV(final InputStream inputStream, final boolean hasHeader) throws IOException {
        Objects.requireNonNull(inputStream);
        final List<Rule> result = new ArrayList<>();

        CSVFormat csvFormat = CSVFormat.RFC4180.withDelimiter(';');
        if (hasHeader) csvFormat = csvFormat.withHeader();
        final CSVParser parser = csvFormat.parse(new InputStreamReader(inputStream));
        for (CSVRecord record : parser.getRecords()) {
            // Read Values
            final String name = record.get(0);
            final String protocol = record.get(1);
            final String srcAddress = record.get(2);
            final String srcPort = record.get(3);
            final String dstAddress = record.get(4);
            final String dstPort = record.get(5);
            final String exportFilter = record.get(6);
            final String omnidirectional = record.get(7);

            // Set values
            final Rule rule = new Rule();
            rule.setName("".equals(name) ? null : name);
            rule.setDstPort("".equals(dstPort) ? null : dstPort);
            rule.setDstAddress("".equals(dstAddress) ? null : dstAddress);
            rule.setSrcPort("".equals(srcPort) ? null : srcPort);
            rule.setSrcAddress("".equals(srcAddress) ? null : srcAddress);
            rule.setProtocol("".equals(protocol) ? null : protocol);
            rule.setExporterFilter("".equals(exportFilter) ? null : exportFilter);
            rule.setOmnidirectional(Boolean.parseBoolean(omnidirectional));

            result.add(rule);
        }

        return result;
    }
}
