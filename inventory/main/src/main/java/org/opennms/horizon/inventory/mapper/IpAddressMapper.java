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
package org.opennms.horizon.inventory.mapper;

import java.net.InetAddress;
import java.net.UnknownHostException;
import org.apache.commons.lang3.StringUtils;
import org.mapstruct.Mapper;
import org.opennms.horizon.shared.utils.InetAddressUtils;

@Mapper(componentModel = "spring")
public interface IpAddressMapper {
    default InetAddress map(String value) throws UnknownHostException {
        if (StringUtils.isNotEmpty(value)) {
            return InetAddressUtils.getInetAddress(value);
        } else {
            return null;
        }
    }

    default String map(InetAddress value) {
        if (value != null) {
            return InetAddressUtils.toIpAddrString(value);
        } else {
            return null;
        }
    }
}
