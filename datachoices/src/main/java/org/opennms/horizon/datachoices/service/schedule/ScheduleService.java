package org.opennms.horizon.datachoices.service.schedule;

import lombok.extern.slf4j.Slf4j;
import org.opennms.horizon.datachoices.job.DataChoicesJob;
import org.opennms.horizon.datachoices.service.dto.CreateJobParams;
import org.opennms.horizon.datachoices.service.dto.CreateSimpleParams;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SimpleTrigger;
import org.quartz.Trigger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Date;

@Slf4j
@Component
public class ScheduleService implements ApplicationRunner {
    private static final String JOB_NAME = "DataChoices";
    private static final String JOB_GROUP = "DataChoices_Group";
    private static final String TRIGGER_NAME = "DataChoices_Trigger";

    @Autowired
    private ScheduleFactory scheduleFactory;

    @Autowired
    private Scheduler scheduler;

    @Value("${datachoices.interval-ms}")
    private long intervalMs;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        JobDetail jobDetail = createJobDetail();
        Trigger trigger = createTrigger();

        JobKey jobKey = JobKey.jobKey(JOB_NAME, JOB_GROUP);

        if (!scheduler.checkExists(jobKey)) {
            scheduler.scheduleJob(jobDetail, trigger);
            log.info("Datachoices job scheduled");
        } else {
            log.info("Datachoices job already exists");
        }
    }

    private JobDetail createJobDetail() {
        CreateJobParams params = new CreateJobParams();

        params.setJobClass(DataChoicesJob.class);
        params.setDurable(true);
        params.setJobName(JOB_NAME);
        params.setJobGroup(JOB_GROUP);
        params.setParams(Collections.emptyMap());

        return scheduleFactory.createJob(params);
    }

    private Trigger createTrigger() {
        CreateSimpleParams params = new CreateSimpleParams();

        params.setTriggerName(TRIGGER_NAME);
        params.setRepeatTime(intervalMs);
        params.setStartTime(new Date());
        params.setMisFireInstruction(SimpleTrigger.MISFIRE_INSTRUCTION_FIRE_NOW);

        return scheduleFactory.createSimpleTrigger(params);
    }
}
