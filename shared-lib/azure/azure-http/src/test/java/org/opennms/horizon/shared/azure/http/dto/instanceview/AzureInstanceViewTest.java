package org.opennms.horizon.shared.azure.http.dto.instanceview;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

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
