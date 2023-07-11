/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2022 The OpenNMS Group, Inc.
 * OpenNMS(R) is Copyright (C) 1999-2022 The OpenNMS Group, Inc.
 *
 * OpenNMS(R) is a registered trademark of The OpenNMS Group, Inc.
 *
 * OpenNMS(R) is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
 *
 * OpenNMS(R) is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with OpenNMS(R).  If not, see:
 *      http://www.gnu.org/licenses/
 *
 * For more information contact:
 *     OpenNMS(R) Licensing <license@opennms.org>
 *     http://www.opennms.org/
 *     http://www.opennms.com/
 *******************************************************************************/

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
        assertThat(values).isNotNull()
            .contains("T:tenant01", "L:LOC01");

        // cert-manager use case
        dn = "OU=T:tenant01+OU=OpenNMSCloud+OU=L:LOC01,CN=opennms-minion-ssl-gateway,O=OpenNMS,L=TBD,ST=TBD,C=CA";

        values = parser.get(dn, "OU");
        assertThat(values).isNotNull()
            .contains("T:tenant01", "L:LOC01");
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
