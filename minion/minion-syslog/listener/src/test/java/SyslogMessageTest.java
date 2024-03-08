



import listener.syslog.YearGuesser;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import listener.syslog.SyslogdConfigFactory;
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
    
    @Before
    public void setUp() {
        MockLogAppender.setupLogging(true, "TRACE");
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

