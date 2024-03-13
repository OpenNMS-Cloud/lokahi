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
package org.opennms.horizon.minion.syslog;

import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;


import org.opennms.horizon.minion.syslog.config.SyslogdConfigFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

import static org.junit.Assert.*;

public class SyslogMessageTest {
    private static final Logger LOG = LoggerFactory.getLogger(SyslogMessageTest.class);

    private final SyslogdConfigFactory m_config;

    public SyslogMessageTest() throws Exception {
        InputStream stream = null;
        try {
            stream = ConfigurationTestUtils.getInputStreamForResource(this, "/etc/syslogd-configuration.xml");
            m_config = new SyslogdConfigFactory(stream);
        } finally {
            if (stream != null) {
                IOUtils.closeQuietly(stream);
            }
        }
    }
    


    @Test
    public void testCustomParserWithProcess() throws Exception {
        final SyslogParser parser = new CustomSyslogParser(m_config, SyslogdTestUtils.toByteBuffer("<6>test: 2007-01-01 127.0.0.1 OpenNMS[1234]: A SyslogNG style message"));
        assertTrue(parser.find());
        final SyslogMessage message = parser.parse();


        assertEquals("test", message.getMessageID());
        assertEquals("127.0.0.1", message.getHostName());
        assertEquals("OpenNMS", message.getProcessName());
        assertEquals("1234", message.getProcessId());
        assertEquals("A SyslogNG style message", message.getMessage());
    }


}
