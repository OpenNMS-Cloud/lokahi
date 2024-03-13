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

import org.apache.commons.lang.StringUtils;
import org.opennms.horizon.minion.syslog.api.EventConstants;
import org.opennms.horizon.minion.syslog.api.EventIpcManagerFactory;
import org.opennms.horizon.minion.syslog.model.IEvent;
import org.opennms.horizon.minion.syslog.model.IParm;




import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Consumer;

public class DaemonTools {

    public static final Logger LOG = LoggerFactory.getLogger(DaemonTools.class);

    public static void handleReloadEvent(IEvent e, String daemonName, Consumer<IEvent> handleConfigurationChanged) {
        final IParm daemonNameParm = e.getParm(EventConstants.PARM_DAEMON_NAME);
        if (daemonNameParm == null || daemonNameParm.getValue() == null) {
            LOG.warn("The {} parameter has no value. Ignoring.", EventConstants.PARM_DAEMON_NAME);
            return;
        }

    }

}
