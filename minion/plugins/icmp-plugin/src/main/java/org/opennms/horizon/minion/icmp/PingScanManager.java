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

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import org.opennms.horizon.minion.plugin.api.Scanner;
import org.opennms.horizon.minion.plugin.api.ScannerManager;
import org.opennms.horizon.shared.icmp.PingerFactory;

public class PingScanManager implements ScannerManager {

    private final ThreadFactory threadFactory =
            new ThreadFactoryBuilder().setNameFormat("ping-sweep-%d").build();

    private final ExecutorService executor = Executors.newCachedThreadPool(threadFactory);
    private final PingerFactory pingerFactory;

    public PingScanManager(PingerFactory pingerFactory) {
        this.pingerFactory = pingerFactory;
    }

    @Override
    public Scanner create() {
        return new PingScan(pingerFactory, executor);
    }
}
