package org.opennms.horizon.datachoices.web.internal;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.opennms.horizon.datachoices.internal.StateManager;
import org.opennms.horizon.datachoices.internal.UsageStatisticsReporter;
import org.opennms.horizon.shared.dto.datachoices.UsageStatisticsReportDTO;

import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@RunWith(MockitoJUnitRunner.class)
public class DataChoiceRestServiceImplTest {

    @InjectMocks
    private DataChoiceRestServiceImpl dataChoiceRestService;

    @Mock
    private StateManager stateManager;

    @Mock
    private UsageStatisticsReporter usageStatisticsReporter;

    @Test
    public void testGetUsageStatistics() {
        UsageStatisticsReportDTO report = new UsageStatisticsReportDTO();
        report.setSystemId(UUID.randomUUID().toString());

        Mockito.when(usageStatisticsReporter.generateReport()).thenReturn(report);

        UsageStatisticsReportDTO returnUsageStats = dataChoiceRestService.getUsageStatistics();
        assertEquals(report.getSystemId(), returnUsageStats.getSystemId());
    }

    @Test
    public void testToggleUsageStatistics() {
        dataChoiceRestService.toggleUsageStatistics(true);
        assertTrue(true);
    }
}
