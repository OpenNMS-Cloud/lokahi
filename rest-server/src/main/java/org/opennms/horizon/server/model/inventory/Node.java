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
package org.opennms.horizon.server.model.inventory;

import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.opennms.horizon.server.model.inventory.tag.Tag;

@Getter
@Setter
public class Node {
    private long id;
    private String nodeLabel;
    private String nodeAlias;
    private String scanType;
    private String monitoredState;
    private long createTime;
    private long monitoringLocationId;
    private List<Tag> tags;
    private List<IpInterface> ipInterfaces;
    private List<SnmpInterface> snmpInterfaces;
    private List<AzureInterface> azureInterfaces;
    private String objectId;
    private String systemName;
    private String systemDescr;
    private String systemLocation;
    private String systemContact;
    private String location;
    private List<Long> discoveryIds;
}
