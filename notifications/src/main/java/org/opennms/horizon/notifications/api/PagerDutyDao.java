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
package org.opennms.horizon.notifications.api;

import java.util.List;
import org.opennms.horizon.notifications.dto.PagerDutyConfigDTO;
import org.opennms.horizon.notifications.exceptions.NotificationConfigUninitializedException;
import org.opennms.horizon.notifications.mapper.PagerDutyConfigMapper;
import org.opennms.horizon.notifications.model.PagerDutyConfig;
import org.opennms.horizon.notifications.repository.PagerDutyConfigRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class PagerDutyDao {
    private static final Logger LOG = LoggerFactory.getLogger(PagerDutyDao.class);

    @Autowired
    private PagerDutyConfigRepository pagerDutyConfigRepository;

    @Autowired
    private PagerDutyConfigMapper pagerDutyConfigMapper;

    public PagerDutyConfigDTO getConfig(String tenantId) throws NotificationConfigUninitializedException {
        List<PagerDutyConfig> configList = pagerDutyConfigRepository.findByTenantId(tenantId);

        if (configList.size() != 1) {
            throw new NotificationConfigUninitializedException(
                    "PagerDuty config not initialized. Row count=" + configList.size());
        }

        return pagerDutyConfigMapper.modelToDTO(configList.get(0));
    }

    public void saveConfig(PagerDutyConfigDTO configDTO) {
        List<PagerDutyConfig> configList = pagerDutyConfigRepository.findByTenantId(configDTO.getTenantId());

        if (configList.isEmpty()) {
            PagerDutyConfig config = pagerDutyConfigMapper.dtoToModel(configDTO);
            pagerDutyConfigRepository.save(config);
        } else {
            PagerDutyConfig config = configList.get(0);
            config.setIntegrationKey(configDTO.getIntegrationKey());
            pagerDutyConfigRepository.save(config);
        }
    }
}
