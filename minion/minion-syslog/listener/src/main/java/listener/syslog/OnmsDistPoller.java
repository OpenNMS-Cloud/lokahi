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
package listener.syslog;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.xml.bind.annotation.XmlRootElement;
import java.io.Serializable;

/**
 * Represents an OpenNMS Distributed Poller.
 */


@XmlRootElement(name="distPoller")
public class OnmsDistPoller  implements Serializable {

    private static final long serialVersionUID = -1094353783612066524L;

    /**
     * default constructor
     */
    public OnmsDistPoller() {}

    /**
     * minimal constructor
     *
     * @param id a {@link String} object.
     */
    public OnmsDistPoller(String id) {
        // org.opennms.netmgt.dao.api.MonitoringLocationDao.DEFAULT_MONITORING_LOCATION_ID

    }
}
