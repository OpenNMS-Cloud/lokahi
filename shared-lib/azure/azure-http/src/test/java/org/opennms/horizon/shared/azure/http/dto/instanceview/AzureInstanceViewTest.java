package org.opennms.horizon.shared.azure.http.dto.instanceview;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class AzureInstanceViewTest {

    @Test
    public void testIsUp() {
        AzureInstanceView instanceView = getInstanceView(true);
        assertTrue(instanceView.isUp());
    }

    @Test
    public void testIsDown() {
        AzureInstanceView instanceView = getInstanceView(false);
        assertFalse(instanceView.isUp());
    }

    private AzureInstanceView getInstanceView(boolean status) {
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

        instanceView.setStatuses(statuses);
        return instanceView;
    }

}
