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
package org.opennms.horizon.minioncertverifier.parser;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.naming.InvalidNameException;
import javax.naming.ldap.LdapName;
import javax.naming.ldap.Rdn;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class BasicDnParser implements CertificateDnParser {

    private final Logger logger = LoggerFactory.getLogger(BasicDnParser.class);

    @Override
    public List<String> get(String dn, String type) {
        if (dn == null) {
            return Collections.emptyList();
        }

        try {
            LdapName name = new LdapName(dn.replaceAll("\\+OU", ",OU"));
            List<String> values = new ArrayList<>();
            for (Rdn rdn : name.getRdns()) {
                if (type.equals(rdn.getType())) {
                    values.add(String.valueOf(rdn.getValue()));
                }
            }

            return values;
        } catch (InvalidNameException e) {
            logger.warn("Could not parse distinguished name '{}'", dn, e);
        }

        return Collections.emptyList();
    }
}
