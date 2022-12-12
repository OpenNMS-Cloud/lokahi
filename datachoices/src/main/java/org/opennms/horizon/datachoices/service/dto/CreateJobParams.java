package org.opennms.horizon.datachoices.service.dto;

import lombok.Data;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.quartz.QuartzJobBean;

import java.util.Map;

@Data
public class CreateJobParams {
    private Class<? extends QuartzJobBean> jobClass;
    private boolean isDurable;
    private String jobName;
    private String jobGroup;
    private Map<String, Object> params;
}
