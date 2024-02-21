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
package org.opennms.horizon.shared.azure.http.dto.instanceview;

import com.google.gson.annotations.SerializedName;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AzureInstanceView {
    private static final String POWER_STATE_RUNNING = "PowerState/running";
    private static final String PROVISIONING_STATE_SUCCEEDED = "ProvisioningState/succeeded";

    @SerializedName("vmAgent")
    private VmAgent vmAgent;

    @SerializedName("computerName")
    private String computerName;

    @SerializedName("osName")
    private String osName;

    @SerializedName("osVersion")
    private String osVersion;

    @SerializedName("statuses")
    private List<AzureStatus> statuses = new ArrayList<>();

    public boolean isReady() {
        if (vmAgent != null) {
            for (AzureStatus status : vmAgent.getStatuses()) {
                String code = status.getCode();
                if (code.equalsIgnoreCase(PROVISIONING_STATE_SUCCEEDED)) {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean isUp() {
        for (AzureStatus status : getStatuses()) {
            String code = status.getCode();
            if (code.equalsIgnoreCase(POWER_STATE_RUNNING)) {
                return true;
            }
        }
        return false;
    }
}
