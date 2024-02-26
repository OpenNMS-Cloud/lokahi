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
package org.opennms.horizon.inventory.model.discovery.active;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.opennms.horizon.inventory.mapper.EncryptAttributeConverter;

@Getter
@Setter
@Entity(name = "azure_active_discovery")
public class AzureActiveDiscovery extends ActiveDiscovery {

    @NotNull
    @Column(name = "client_id")
    private String clientId;

    @NotNull
    @Convert(converter = EncryptAttributeConverter.class)
    @Column(name = "client_secret")
    private String clientSecret;

    @NotNull
    @Column(name = "subscription_id")
    private String subscriptionId;

    @NotNull
    @Column(name = "directory_id")
    private String directoryId;
}
