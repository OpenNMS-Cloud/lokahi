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
package org.opennms.horizon.minion.syslog.daemon;

/**
 * SimpleSpringContextJmxServiceDaemonMBean
 *
 * @author brozow
 * @version $Id: $
 */
public interface SimpleSpringContextJmxServiceDaemonMBean extends
        BaseOnmsMBean {

    /**
     * <p>getLoggingPrefix</p>
     *
     * @return a {@link String} object.
     */
    public String getLoggingPrefix();
    /**
     * <p>getSpringContext</p>
     *
     * @return a {@link String} object.
     */
    public String getSpringContext();
    /**
     * <p>setLoggingPrefix</p>
     *
     * @param loggingPrefix a {@link String} object.
     */
    public void setLoggingPrefix(String loggingPrefix);
    /**
     * <p>setSpringContext</p>
     *
     * @param springContext a {@link String} object.
     */
    public void setSpringContext(String springContext);

}
