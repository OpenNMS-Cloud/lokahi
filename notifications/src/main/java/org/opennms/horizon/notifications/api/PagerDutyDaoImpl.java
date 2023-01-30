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

package org.opennms.horizon.notifications.api;

import org.opennms.horizon.notifications.dto.PagerDutyConfigDTO;
import org.opennms.horizon.notifications.exceptions.NotificationConfigUninitializedException;
import org.opennms.horizon.notifications.mapper.PagerDutyConfigMapper;
import org.opennms.horizon.notifications.model.PagerDutyConfig;
import org.opennms.horizon.notifications.repository.PagerDutyConfigRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class PagerDutyDaoImpl implements PagerDutyDao{
    private static final Logger LOG = LoggerFactory.getLogger(PagerDutyDao.class);

    @Autowired
    private PagerDutyConfigRepository pagerDutyConfigRepository;

    @Autowired
    private PagerDutyConfigMapper pagerDutyConfigMapper;

    @Override
    public PagerDutyConfigDTO getConfig() throws NotificationConfigUninitializedException {
        List<PagerDutyConfig> configList = pagerDutyConfigRepository.findAll();

        if (configList.size() != 1) {
            throw new NotificationConfigUninitializedException("PagerDuty config not initialized. Row count=" + configList.size());
        }

        return pagerDutyConfigMapper.modelToDTO(configList.get(0));
    }

    @Override
    public void saveConfig(PagerDutyConfigDTO configDTO) {
        List<PagerDutyConfig> configList = pagerDutyConfigRepository.findAll();

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
