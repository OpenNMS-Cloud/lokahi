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
