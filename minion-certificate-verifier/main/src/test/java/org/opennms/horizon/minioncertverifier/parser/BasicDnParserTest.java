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

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

public class BasicDnParserTest {

    CertificateDnParser parser = new BasicDnParser();

    @Test
    public void testParser() {
        String dn = "OU=T:tenant01,OU=L:LOC01,CN=opennms-minion-ssl-gateway,O=OpenNMS,L=TBD,ST=TBD,C=CA";

        List<String> values = parser.get(dn, "OU");
        assertThat(values).isNotNull().contains("T:tenant01", "L:LOC01");

        // cert-manager use case
        dn = "OU=T:tenant01+OU=OpenNMSCloud+OU=L:LOC01,CN=opennms-minion-ssl-gateway,O=OpenNMS,L=TBD,ST=TBD,C=CA";

        values = parser.get(dn, "OU");
        assertThat(values).isNotNull().contains("T:tenant01", "L:LOC01");
    }

    @Test
    public void testEmptyValue() {
        List<String> values = parser.get("", "OU");
        assertThat(values).isNotNull().isEmpty();
    }

    @Test
    public void testNullValue() {
        List<String> values = parser.get(null, "OU");
        assertThat(values).isNotNull().isEmpty();
    }
}
