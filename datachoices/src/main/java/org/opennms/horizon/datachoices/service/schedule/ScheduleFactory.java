package org.opennms.horizon.datachoices.service.schedule;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.opennms.horizon.datachoices.service.dto.CreateJobParams;
import org.opennms.horizon.datachoices.service.dto.CreateSimpleParams;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.SimpleTrigger;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.quartz.JobDetailFactoryBean;
import org.springframework.scheduling.quartz.SimpleTriggerFactoryBean;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ScheduleFactory {
    private final ApplicationContext context;

    public JobDetail createJob(CreateJobParams createJobParams) {
        JobDetailFactoryBean factoryBean = new JobDetailFactoryBean();
        factoryBean.setJobClass(createJobParams.getJobClass());
        factoryBean.setDurability(createJobParams.isDurable());
        factoryBean.setApplicationContext(context);
        factoryBean.setName(createJobParams.getJobName());
        factoryBean.setGroup(createJobParams.getJobGroup());

        JobDataMap jobDataMap = new JobDataMap();
        String classKey = createJobParams.getJobName() + createJobParams.getJobGroup();
        jobDataMap.put(classKey, createJobParams.getJobClass().getName());
        jobDataMap.putAll(createJobParams.getParams());

        factoryBean.setJobDataMap(jobDataMap);
        factoryBean.afterPropertiesSet();

        return factoryBean.getObject();
    }

    public SimpleTrigger createSimpleTrigger(CreateSimpleParams createSimpleParams) {
        SimpleTriggerFactoryBean factoryBean = new SimpleTriggerFactoryBean();
        factoryBean.setName(createSimpleParams.getTriggerName());
        factoryBean.setStartTime(createSimpleParams.getStartTime());
        factoryBean.setRepeatInterval(createSimpleParams.getRepeatTime());
        factoryBean.setRepeatCount(SimpleTrigger.REPEAT_INDEFINITELY);
        factoryBean.setMisfireInstruction(createSimpleParams.getMisFireInstruction());
        factoryBean.afterPropertiesSet();
        return factoryBean.getObject();
    }
}
