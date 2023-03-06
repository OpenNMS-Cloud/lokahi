/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2018-2018 The OpenNMS Group, Inc.
 * OpenNMS(R) is Copyright (C) 1999-2018 The OpenNMS Group, Inc.
 *
 * OpenNMS(R) is a registered trademark of The OpenNMS Group, Inc.
 *
 * OpenNMS(R) is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
 *
 * OpenNMS(R) is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with OpenNMS(R).  If not, see:
 *      http://www.gnu.org/licenses/
 *
 * For more information contact:
 *     OpenNMS(R) Licensing <license@opennms.org>
 *     http://www.opennms.org/
 *     http://www.opennms.com/
 *******************************************************************************/

package org.opennms.horizon.flows.classification.csv;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.opennms.horizon.flows.classification.persistence.api.Rule;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class CsvImporter {

    private CsvImporter() {
    }

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
