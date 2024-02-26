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
package org.opennms.horizon.minion.ipc.twin.common;

import java.util.HashMap;
import java.util.Map;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
public class TwinUpdate {

    private final TwinRequest twinRequest;
    private byte[] object;
    private int version;
    private boolean isPatch;
    private String sessionId;

    @EqualsAndHashCode.Exclude
    private Map<String, String> tracingInfo = new HashMap<>();

    public TwinUpdate() {
        this.twinRequest = new TwinRequest();
    }

    public TwinUpdate(String key, byte[] object) {
        this.twinRequest = new TwinRequest(key);
        this.object = object;
    }

    public TwinUpdate(String key) {
        this.twinRequest = new TwinRequest(key);
    }

    public void setKey(String key) {
        twinRequest.setKey(key);
    }

    public String getKey() {
        return twinRequest.getKey();
    }

    public Map<String, String> getTracingInfo() {
        return tracingInfo;
    }

    public void addTracingInfo(String key, String value) {
        this.tracingInfo.put(key, value);
    }
}
