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

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.opennms.horizon.notifications.dto.PagerDutyConfigDTO;
import org.opennms.horizon.notifications.exceptions.NotificationConfigUninitializedException;
import org.opennms.horizon.notifications.mapper.PagerDutyConfigMapper;
import org.opennms.horizon.notifications.model.PagerDutyConfig;
import org.opennms.horizon.notifications.repository.PagerDutyConfigRepository;

@RunWith(MockitoJUnitRunner.class)
public class PagerDutyDaoTest {
    @InjectMocks
    PagerDutyDao pagerDutyDao;

    @Mock
    PagerDutyConfigRepository pagerDutyConfigRepository;

    @Mock
    PagerDutyConfigMapper pagerDutyConfigMapper;

    @Test
    public void updateConfig() throws Exception {
        Mockito.when(pagerDutyConfigRepository.findByTenantId(any())).thenReturn(Arrays.asList(new PagerDutyConfig()));
        PagerDutyConfigDTO config = getConfigDTO();
        pagerDutyDao.saveConfig(config);

        Mockito.verify(pagerDutyConfigRepository, times(1)).save(any());
    }

    @Test
    public void insertConfig() throws Exception {
        Mockito.when(pagerDutyConfigRepository.findByTenantId(any())).thenReturn(new ArrayList<>());
        PagerDutyConfigDTO config = getConfigDTO();
        pagerDutyDao.saveConfig(config);

        Mockito.verify(pagerDutyConfigRepository, times(1)).save(any());
    }

    @Test
    public void getUninitialisedConfig() {
        try {
            pagerDutyDao.getConfig("any");
        } catch (NotificationConfigUninitializedException e) {
            assertEquals("PagerDuty config not initialized. Row count=0", e.getMessage());
        }
    }

    @Test
    public void getInitialisedConfig() throws Exception {
        List<PagerDutyConfig> configs = Arrays.asList(new PagerDutyConfig());
        Mockito.when(pagerDutyConfigRepository.findByTenantId("any")).thenReturn(configs);
        Mockito.when(pagerDutyConfigMapper.modelToDTO(any())).thenReturn(getConfigDTO());
        PagerDutyConfigDTO config = pagerDutyDao.getConfig("any");

        assertEquals("integration_key", config.getIntegrationKey());
    }

    private PagerDutyConfigDTO getConfigDTO() {
        return PagerDutyConfigDTO.newBuilder()
                .setIntegrationKey("integration_key")
                .build();
    }
}
