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

import java.util.Collection;


/**
 * <p>EventSubscriptionService interface.</p>
 *
 * @author ranger
 * @version $Id: $
 */
public interface EventSubscriptionService {
    

    public void addEventListener(EventListener listener);


    public void addEventListener(EventListener listener, Collection<String> ueis);


    public void addEventListener(EventListener listener, String uei);

    public void removeEventListener(EventListener listener);


    public void removeEventListener(EventListener listener, Collection<String> ueis);

    public void removeEventListener(EventListener listener, String uei);

    /**
     * Checks if there is at least one listener for the given uei.
     *
     * @param uei the uie to check for
     *
     * @return {@code true} iff there is at least one listener
     */
    public boolean hasEventListener(final String uei);


}
