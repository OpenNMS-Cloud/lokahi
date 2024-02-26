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
package org.opennms.horizon.minion.flows.parser.netflow9.proto;

import com.google.common.base.MoreObjects;
import io.netty.buffer.ByteBuf;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import org.opennms.horizon.minion.flows.parser.InvalidPacketException;
import org.opennms.horizon.minion.flows.parser.MissingTemplateException;
import org.opennms.horizon.minion.flows.parser.session.Field;
import org.opennms.horizon.minion.flows.parser.session.Session;
import org.opennms.horizon.minion.flows.parser.session.Template;

public final class DataSet extends FlowSet<DataRecord> {
    private final Session.Resolver resolver;

    public final Template template;

    public final List<DataRecord> records;

    public DataSet(
            final Packet packet, final FlowSetHeader header, final Session.Resolver resolver, final ByteBuf buffer)
            throws InvalidPacketException, MissingTemplateException {
        super(packet, header);

        this.resolver = Objects.requireNonNull(resolver);
        this.template = this.resolver.lookupTemplate(this.header.setId);

        final int minimumRecordLength =
                template.stream().mapToInt(Field::length).sum();

        final List<DataRecord> records = new LinkedList();
        while (buffer.isReadable(minimumRecordLength)) {
            records.add(new DataRecord(this, resolver, template, buffer));
        }

        if (records.size() == 0) {
            throw new InvalidPacketException(buffer, "Empty set");
        }

        this.records = Collections.unmodifiableList(records);
    }

    @Override
    public Iterator<DataRecord> iterator() {
        return this.records.iterator();
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("header", header)
                .add("records", records)
                .toString();
    }
}
