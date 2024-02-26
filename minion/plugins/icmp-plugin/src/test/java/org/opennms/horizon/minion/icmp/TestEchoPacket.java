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
package org.opennms.horizon.minion.icmp;

import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import org.opennms.horizon.shared.icmp.EchoPacket;

@RequiredArgsConstructor
public class TestEchoPacket implements EchoPacket {
    private final boolean echoReply;

    @Override
    public boolean isEchoReply() {
        return this.echoReply;
    }

    @Override
    public int getIdentifier() {
        return 1;
    }

    @Override
    public int getSequenceNumber() {
        return 1;
    }

    @Override
    public long getThreadId() {
        return Thread.currentThread().getId();
    }

    @Override
    public long getReceivedTimeNanos() {
        return 1000;
    }

    @Override
    public long getSentTimeNanos() {
        return 1000;
    }

    @Override
    public double elapsedTime(TimeUnit timeUnit) {
        return 1000;
    }
}
