package org.opennms.horizon.datachoices.job;

import lombok.RequiredArgsConstructor;
import org.opennms.horizon.datachoices.service.DataChoicesService;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.scheduling.quartz.QuartzJobBean;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DataChoicesJob extends QuartzJobBean {
    private final DataChoicesService service;

    @Override
    protected void executeInternal(JobExecutionContext context) throws JobExecutionException {
        try {
            service.execute();
        } catch (Exception e) {
            throw new JobExecutionException("Failed to execute data choices", e);
        }
    }
}
