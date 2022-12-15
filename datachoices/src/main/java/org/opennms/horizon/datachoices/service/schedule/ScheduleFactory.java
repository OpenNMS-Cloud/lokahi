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
