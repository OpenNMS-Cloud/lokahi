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

import com.codahale.metrics.MetricRegistry;
import java.net.InetAddress;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.opennms.horizon.flows.document.FlowDocument;
import org.opennms.horizon.minion.flows.parser.factory.DnsResolver;
import org.opennms.horizon.minion.flows.parser.transport.MessageBuilder;
import org.opennms.horizon.shared.ipc.rpc.IpcIdentity;
import org.opennms.horizon.shared.ipc.sink.api.AsyncDispatcher;

public class ClockSkewTest {

    private final IpcIdentity identity = new IpcIdentity() {
        @Override
        public String getId() {
            return "myId";
        }
    };

    private final DnsResolver dnsResolver = new DnsResolver() {

        @Override
        public CompletableFuture<Optional<InetAddress>> lookup(final String hostname) {
            return CompletableFuture.completedFuture(Optional.empty());
        }

        @Override
        public CompletableFuture<Optional<String>> reverseLookup(InetAddress inetAddress) {
            return CompletableFuture.completedFuture(Optional.empty());
        }
    };

    private final ParserBase parserBase = new ParserBaseExt(
            Protocol.NETFLOW5,
            "name",
            new AsyncDispatcher<>() {
                @Override
                public void send(FlowDocument message) {}

                @Override
                public void close() {}
            },
            identity,
            dnsResolver,
            new MetricRegistry());

    @Before
    public void reset() {
        emptyClockSkewCache();
    }

    @Test
    public void testClockSkewCorrectlyInsertedInCache() {
        long current = System.currentTimeMillis();

        parserBase.setMaxClockSkew(300);
        parserBase.setClockSkewEventRate(3600);
        parserBase.detectClockSkew(current - 299000, InetAddress.getLoopbackAddress());
        Assert.assertEquals(0, parserBase.getClockSkewEventCache().size());
        emptyClockSkewCache();

        parserBase.detectClockSkew(current - 301000, InetAddress.getLoopbackAddress());
        Assert.assertEquals(1, parserBase.getClockSkewEventCache().size());
        emptyClockSkewCache();

        parserBase.detectClockSkew(current - 301000, InetAddress.getLoopbackAddress());
        Assert.assertEquals(1, parserBase.getClockSkewEventCache().size());
    }

    @Test
    public void testClockSkewEventRate() throws Exception {
        long current = System.currentTimeMillis();

        parserBase.setMaxClockSkew(300);
        parserBase.setClockSkewEventRate(1);
        parserBase.detectClockSkew(current - 299000, InetAddress.getLoopbackAddress());
        Assert.assertEquals(0, parserBase.getClockSkewEventCache().size());

        parserBase.detectClockSkew(current - 301000, InetAddress.getLoopbackAddress());
        Assert.assertEquals(1, parserBase.getClockSkewEventCache().size());

        parserBase.detectClockSkew(current - 301000, InetAddress.getLoopbackAddress());
        Assert.assertEquals(1, parserBase.getClockSkewEventCache().size());

        parserBase.detectClockSkew(current - 301000, InetAddress.getLoopbackAddress());
        Assert.assertEquals(1, parserBase.getClockSkewEventCache().size());

        emptyClockSkewCache();

        parserBase.detectClockSkew(current - 301000, InetAddress.getLoopbackAddress());
        Assert.assertEquals(1, parserBase.getClockSkewEventCache().size());
    }

    private void emptyClockSkewCache() {
        parserBase.getClockSkewEventCache().asMap().clear();
    }

    private static class ParserBaseExt extends ParserBase {

        public ParserBaseExt(
                Protocol protocol,
                String name,
                AsyncDispatcher<FlowDocument> dispatcher,
                IpcIdentity identity,
                DnsResolver dnsResolver,
                MetricRegistry metricRegistry) {
            super(protocol, name, dispatcher, identity, dnsResolver, metricRegistry);
        }

        @Override
        protected MessageBuilder getMessageBuilder() {
            return (values, enrichment) -> FlowDocument.newBuilder();
        }

        @Override
        public Object dumpInternalState() {
            return null;
        }
    }
}
