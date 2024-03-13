package org.opennms.horizon.minion.syslog.listener;/*
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




import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

public class SyslogSinkModule extends AbstractXmlSinkModule<SyslogConnection, SyslogMessageLogDTO> {

    public static final String MODULE_ID = "Syslog";

    private final SyslogdConfig config;


    public SyslogSinkModule(SyslogdConfig config) {

        this.config = Objects.requireNonNull(config);

    }

    @Override
    public String getId() {
        return MODULE_ID;
    }

    @Override
    public int getNumConsumerThreads() {
        return config.getNumThreads();
    }

    @Override
    public byte[] marshal(SyslogMessageLogDTO message) {
        return new byte[0];
    }

    @Override
    public SyslogMessageLogDTO unmarshal(byte[] message) {
        return null;
    }

    @Override
    public byte[] marshalSingleMessage(SyslogConnection message) {
        return new byte[0];
    }

    @Override
    public AggregationPolicy<SyslogConnection, SyslogMessageLogDTO, SyslogMessageLogDTO> getAggregationPolicy() {
        final String systemId = "test";
        final String systemLocation = "test";
        return new AggregationPolicy<SyslogConnection, SyslogMessageLogDTO, SyslogMessageLogDTO>() {
            @Override
            public int getCompletionSize() {
                return config.getBatchSize();
            }

            @Override
            public int getCompletionIntervalMs() {
                return config.getBatchIntervalMs();
            }

            @Override
            public Object key(SyslogConnection syslogConnection) {
                return syslogConnection.getSource();
            }

            @Override
            public SyslogMessageLogDTO aggregate(SyslogMessageLogDTO accumulator, SyslogConnection connection) {

                return accumulator;
            }

            @Override
            public SyslogMessageLogDTO build(SyslogMessageLogDTO accumulator) {
                return accumulator;
            }
        };
    }

    @Override
    public AsyncPolicy getAsyncPolicy() {
        return new AsyncPolicy() {
            @Override
            public int getQueueSize() {
                return config.getQueueSize();
            }

            @Override
            public int getNumThreads() {
                return config.getNumThreads();
            }

            @Override
            public boolean isBlockWhenFull() {
                return true;
            }
        };
    }

    @Override
    public SyslogConnection unmarshalSingleMessage(byte[] bytes) {
        SyslogMessageLogDTO syslogMessageLogDTO = unmarshal(bytes);
        SyslogMessageDTO syslogMessageDTO = new SyslogMessageDTO();
        InetSocketAddress inetSocketAddress = new InetSocketAddress(syslogMessageLogDTO.getSourceAddress(), Integer.parseInt("10514"));
        return new SyslogConnection(inetSocketAddress, ByteBuffer.wrap("test".getBytes(StandardCharsets.UTF_8)));
    }

    /**
     * Used for testing.
     */
    public SyslogMessageLogDTO toMessageLog(SyslogConnection... connections) {
        final String systemId = "test";
        final String systemLocation = "test";
        if (connections.length < 1) {
            throw new IllegalArgumentException("One or more connection are required.");
        }

        return new SyslogMessageLogDTO();
    }

    @Override
    public int hashCode() {
        return Objects.hash(MODULE_ID);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        return true;
    }
}
