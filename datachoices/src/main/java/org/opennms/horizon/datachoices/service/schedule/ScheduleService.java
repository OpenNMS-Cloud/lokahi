/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2022 The OpenNMS Group, Inc.
 * OpenNMS(R) is Copyright (C) 1999-2022 The OpenNMS Group, Inc.
 *
 * OpenNMS(R) is a registered trademark of The OpenNMS Group, Inc.
 *
 * OpenNMS(R) is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
 *
 * OpenNMS(R) is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with OpenNMS(R).  If not, see:
 *      http://www.gnu.org/licenses/
 *
 * For more information contact:
 *     OpenNMS(R) Licensing <license@opennms.org>
 *     http://www.opennms.org/
 *     http://www.opennms.com/
 *******************************************************************************/

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
