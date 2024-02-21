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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.junit.Test;

public class AzureInstanceViewTest {

    @Test
    public void testIsReady() {
        AzureInstanceView instanceView = getInstanceView(true, true);
        assertTrue(instanceView.isReady());
    }

    @Test
    public void testNotReady() {
        AzureInstanceView instanceView = getInstanceView(true, false);
        assertFalse(instanceView.isReady());
    }

    @Test
    public void testNullVmAgent() {
        AzureInstanceView instanceView = new AzureInstanceView();
        assertFalse(instanceView.isReady());
    }

    @Test
    public void testIsUp() {
        AzureInstanceView instanceView = getInstanceView(true, true);
        assertTrue(instanceView.isUp());
    }

    @Test
    public void testIsDown() {
        AzureInstanceView instanceView = getInstanceView(false, false);
        assertFalse(instanceView.isUp());
    }

    private AzureInstanceView getInstanceView(boolean status, boolean ready) {
        AzureInstanceView instanceView = new AzureInstanceView();

        List<AzureStatus> statuses = new ArrayList<>();
        AzureStatus status1 = new AzureStatus();
        status1.setCode("Some Other Status");
        statuses.add(status1);

        if (status) {
            AzureStatus status2 = new AzureStatus();
            status2.setCode("PowerState/running");
            statuses.add(status2);
        }
        List<AzureStatus> readyStatuses = new ArrayList<>();
        AzureStatus readyStatus = new AzureStatus();
        readyStatuses.add(readyStatus);
        VmAgent vmAgent = new VmAgent();
        vmAgent.setStatuses(readyStatuses);
        instanceView.setVmAgent(vmAgent);
        if (ready) {
            readyStatus.setCode("ProvisioningState/succeeded");
        } else {
            readyStatus.setCode("ProvisioningState/failed");
        }

        instanceView.setStatuses(statuses);
        return instanceView;
    }
}
