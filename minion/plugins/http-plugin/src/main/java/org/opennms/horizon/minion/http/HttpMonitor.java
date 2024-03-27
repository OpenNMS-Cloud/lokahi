package org.opennms.horizon.minion.http;

import com.google.protobuf.Any;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Struct;
import com.google.protobuf.Value;
import org.apache.commons.lang3.StringUtils;
import org.opennms.horizon.events.Base64;
import org.opennms.horizon.minion.plugin.api.*;
import org.opennms.horizon.shared.utils.IPLike;
import org.opennms.horizon.shared.utils.InetAddressUtils;
import org.opennms.monitors.http.contract.HttpMonitorRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.InterruptedIOException;
import java.net.*;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.TreeMap;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Pattern;

public class HttpMonitor  extends AbstractServiceMonitor {
    public static final Logger LOG = LoggerFactory.getLogger(HttpMonitor.class);

    private static final Pattern HEADER_PATTERN = Pattern.compile("header[0-9]+$");

    /**
     * Default HTTP ports.
     */
    private static final int[] DEFAULT_PORTS = { 80, 8080, 8888};

    /**
     * Default retries.
     */
    private static final int DEFAULT_RETRY = 0;

    /**
     * Default URL to 'GET'
     */
    private static final String DEFAULT_URL = "/";

    /**
     * Default timeout. Specifies how long (in milliseconds) to block waiting for data from the
     * monitored interface.
     */
    private static final int DEFAULT_TIMEOUT = 3000; // 3 second timeout on read()

    public static final String PARAMETER_VERBOSE = "verbose";
    public static final String PARAMETER_USER_AGENT = "user-agent";
    public static final String PARAMETER_BASIC_AUTHENTICATION = "basic-authentication";
    public static final String PARAMETER_USER = "user";
    public static final String PARAMETER_PASSWORD = "password";
    public static final String PARAMETER_HOST_NAME = "host-name";
    public static final String PARAMETER_RESPONSE_TEXT = "response-text";
    public static final String PARAMETER_RESPONSE = "response";
    public static final String PARAMETER_URL = "url";
    public static final String PARAMETER_PORT = "port";

    @Override
    public CompletableFuture<ServiceMonitorResponse> poll(MonitoredService svc, Any config) {

        CompletableFuture<ServiceMonitorResponse> future = null;
        LOG.info("Hello HttpMonitor from poll method  11111......");
        System.out.println("Hello HttpMonitor Service....");
        try {
            HttpMonitorRequest icmpMonitorRequest = config.unpack(HttpMonitorRequest.class);

        } catch (InvalidProtocolBufferException e) {
            throw new RuntimeException(e);
        }
        if (!config.is(HttpMonitorRequest.class)) {
                throw new IllegalArgumentException("config must be an HttpRequest; type-url=" + config.getTypeUrl());
            }
        System.out.println("poll end");
        return future;
    }



    private void logResponseTimes(Double responseTime, String line) {
        LOG.debug("poll: response= {}", line);
        LOG.debug("poll: responseTime= {}ms", responseTime);
    }

    /**
     * <p>wrapSocket</p>
     *
     * @return a {@link java.net.Socket} object.
     * @throws java.io.IOException if any.
     */
    protected SocketWrapper getSocketWrapper() {
        return new DefaultSocketWrapper();
    }

    private static boolean determineVerbosity(final Map<String, Object> parameters) {
        final String verbose = ParameterMap.getKeyedString(parameters, PARAMETER_VERBOSE, null);
        return (verbose != null && verbose.equalsIgnoreCase("true")) ? true : false;
    }

    private static String determineUserAgent(final Map<String, Object> parameters) {
        String agent = resolveKeyedString(parameters, PARAMETER_USER_AGENT, null);
        if (isBlank(agent)) {
            return "OpenNMS HttpMonitor";
        }
        return agent;
    }

    static String determineBasicAuthentication(final Map<String, Object> parameters) {
        String credentials = resolveKeyedString(parameters, PARAMETER_BASIC_AUTHENTICATION, null);

        if (isNotBlank(credentials)) {
            credentials = new String(Base64.encodeBase64(credentials.getBytes()));
        } else {

            String user = resolveKeyedString(parameters, PARAMETER_USER, null);

            if (isBlank(user)) {
                credentials = null;
            } else {
                String passwd = resolveKeyedString(parameters, PARAMETER_PASSWORD, "");
                credentials = new String(Base64.encodeBase64((user+":"+passwd).getBytes()));
            }
        }

        return credentials;
    }

    private static String determineHttpHeader(final Map<String, Object> parameters, String key) {
        return ParameterMap.getKeyedString(parameters, key, null);
    }

    private static String determineResponseText(final Map<String, Object> parameters) {
        return ParameterMap.getKeyedString(parameters, PARAMETER_RESPONSE_TEXT, null);
    }

    private static String determineResponse(final Map<String, Object> parameters) {
        return ParameterMap.getKeyedString(parameters, PARAMETER_RESPONSE, determineDefaultResponseRange(determineUrl(parameters)));
    }

    private static String determineUrl(final Map<String, Object> parameters) {
        String url = resolveKeyedString(parameters, PARAMETER_URL, DEFAULT_URL);
        return url;
    }

    /**
     * <p>determinePorts</p>
     *
     * @param parameters a {@link java.util.Map} object.
     * @return an array of int.
     */
    protected int[] determinePorts(final Map<String, Object> parameters) {
        return ParameterMap.getKeyedIntegerArray(parameters, PARAMETER_PORT, DEFAULT_PORTS);
    }

    private static String determineDefaultResponseRange(String url) {
        if (url == null || url.equals(DEFAULT_URL)) {
            return "100-499";
        }
        return "100-399";
    }

    private static boolean isNotBlank(String str) {
        return org.apache.commons.lang.StringUtils.isNotBlank(str);
    }

    private static boolean isBlank(String str) {
        return org.apache.commons.lang.StringUtils.isBlank(str);
    }

    final class HttpMonitorClient {
        private double m_responseTime;
        final InetAddress m_addr;
        final Map<String, Object> m_parameters;
        String m_httpCmd;
        Socket m_httpSocket;
        private BufferedReader m_lineRdr;
        private String m_currentLine;
        private int m_serviceStatus;
        private String m_reason;
        private final StringBuilder m_html = new StringBuilder();
        private int m_serverResponseCode;
        private TimeoutTracker m_timeoutTracker;
        private int m_currentPort;
        private String m_responseText;
        private boolean m_responseTextFound = false;
        private final String m_nodeLabel;
        private boolean m_headerFinished = false;

        HttpMonitorClient(final String nodeLabel, final InetAddress addr, final Map<String, Object>parameters) {
            m_nodeLabel = nodeLabel;
            m_addr = addr;
            m_parameters = parameters;
            buildCommand();
            m_serviceStatus = PollStatus.SERVICE_UNAVAILABLE;
            m_responseText = determineResponseText(parameters);
        }

        public void read() throws IOException {
            for (int nullCount = 0; nullCount < 2;) {
                readLinedMatching();
                if (isEndOfStream()) {
                    nullCount++;
                }
            }
        }

        public int getCurrentPort() {
            return m_currentPort;
        }

        public Map<String, Object> getParameters() {
            return m_parameters;
        }

        public boolean isResponseTextFound() {
            return m_responseTextFound;
        }
        public void setResponseTextFound(final boolean found) {
            m_responseTextFound  = found;
        }

        private String determineVirtualHost(final InetAddress addr, final Map<String, Object> parameters) {
            String virtualHost = ParameterMap.getKeyedString(parameters, PARAMETER_HOST_NAME, null);
            final String host = InetAddressUtils.str(addr);
            // Wrap IPv6 addresses in square brackets
            if (addr instanceof Inet6Address) {
                return "[" + host + "]";
            } else {
                return host;
            }
        }

        public boolean checkCurrentLineMatchesResponseText() {
            if (!m_headerFinished && StringUtils.isEmpty(m_currentLine)) {
                m_headerFinished = true;  // Set to true when all HTTP headers has been processed.
            }
            if (!m_headerFinished) { // Skip perform the regex processing over HTTP headers.
                return false;
            }
            if (m_responseText.charAt(0) == '~' && !m_responseTextFound) {
                m_responseTextFound = m_currentLine.matches(m_responseText.substring(1));
            } else {
                m_responseTextFound = (m_currentLine.indexOf(m_responseText) != -1 ? true : false);
            }
            return m_responseTextFound;
        }

        public String getResponseText() {
            return m_responseText;
        }

        public void setResponseText(final String responseText) {
            m_responseText = responseText;
        }

        public void setCurrentPort(final int currentPort) {
            m_currentPort = currentPort;
        }

        public TimeoutTracker getTimeoutTracker() {
            return m_timeoutTracker;
        }

        public void setTimeoutTracker(final TimeoutTracker tracker) {
            m_timeoutTracker = tracker;
        }

        public Double getResponseTime() {
            return m_responseTime;
        }

        public void setResponseTime(final double elapsedTimeInMillis) {
            m_responseTime = elapsedTimeInMillis;
        }

        private void connect() throws IOException, SocketException {
            m_httpSocket = new Socket();
            m_httpSocket.connect(new InetSocketAddress(m_addr, m_currentPort), m_timeoutTracker.getConnectionTimeout());
            m_serviceStatus = PollStatus.SERVICE_UNRESPONSIVE;
            m_httpSocket.setSoTimeout(m_timeoutTracker.getSoTimeout());
            m_httpSocket = getSocketWrapper().wrapSocket(m_httpSocket);
        }

        public void closeConnection() {
            try {
                if (m_httpSocket != null) {
                    m_httpSocket.close();
                    m_httpSocket = null;
                }
            } catch (final IOException e) {
                e.fillInStackTrace();
                HttpMonitor.LOG.warn("Error closing socket connection", e);
            }
        }

        public int getPollStatus() {
            return m_serviceStatus;
        }

        public void setPollStatus(final int serviceStatus) {
            m_serviceStatus = serviceStatus;
        }

        public String getCurrentLine() {
            return m_currentLine;
        }


        public int getServerResponse() {
            return m_serverResponseCode;
        }

        private void determineServerInitialResponse() {
            int serverResponseValue = -1;

            if (m_currentLine != null) {

                if (m_currentLine.startsWith("HTTP/")) {
                    serverResponseValue = parseHttpResponse();

                    if (IPLike.matchNumericListOrRange(String.valueOf(serverResponseValue), determineResponse(m_parameters))) {
                        if (HttpMonitor.LOG.isDebugEnabled()) {
                            HttpMonitor.LOG.debug("determineServerResponse: valid server response: "+serverResponseValue+" found.");
                        }
                        m_serviceStatus = PollStatus.SERVICE_AVAILABLE;
                    } else {
                        m_serviceStatus = PollStatus.SERVICE_UNAVAILABLE;
                        final StringBuilder sb = new StringBuilder();
                        sb.append("HTTP response value: ");
                        sb.append(serverResponseValue);
                        sb.append(". Expecting: ");
                        sb.append(determineResponse(m_parameters));
                        sb.append(".");
                        m_reason = sb.toString();
                    }
                }
            }
            m_serverResponseCode = serverResponseValue;
        }

        private int parseHttpResponse() {
            final StringTokenizer t = new StringTokenizer(m_currentLine);
            if (t.hasMoreTokens()) {
                t.nextToken();
            }

            int serverResponse = -1;
            if (t.hasMoreTokens()) {
                try {
                    serverResponse = Integer.parseInt(t.nextToken());
                } catch (final NumberFormatException nfE) {
                    if (HttpMonitor.LOG.isInfoEnabled()) {
                        HttpMonitor.LOG.info("Error converting response code from host = {}, response = {}", m_addr, m_currentLine);
                    }
                }
            }
            return serverResponse;
        }

        public boolean isEndOfStream() {
            if (m_currentLine == null) {
                return true;
            }
            return false;
        }

        public String readLine() throws IOException {
            m_currentLine = m_lineRdr.readLine();

            if (determineVerbosity(m_parameters) && HttpMonitor.LOG.isDebugEnabled()) {
                HttpMonitor.LOG.debug("\t<<: {}", m_currentLine);
            }

            m_html.append(m_currentLine);
            return m_currentLine;
        }

        public String readLinedMatching() throws IOException {
            readLine();

            if (m_responseText != null && m_currentLine != null && !m_responseTextFound) {
                if (checkCurrentLineMatchesResponseText()) {
                    if (HttpMonitor.LOG.isDebugEnabled()) {
                        HttpMonitor.LOG.debug("response-text: "+m_responseText+": found.");
                    }
                    m_serviceStatus = PollStatus.SERVICE_AVAILABLE;
                }
            }
            return m_currentLine;
        }

        public void sendHttpCommand() throws IOException {
            if (determineVerbosity(m_parameters) && HttpMonitor.LOG.isDebugEnabled()) {
                HttpMonitor.LOG.debug("Sending HTTP command: {}", m_httpCmd);
            }
            m_httpSocket.getOutputStream().write(m_httpCmd.getBytes());
            m_lineRdr = new BufferedReader(new InputStreamReader(m_httpSocket.getInputStream()));
            readLine();
            if (determineVerbosity(m_parameters)) {
                HttpMonitor.LOG.debug("Server response: {}", m_currentLine);
            }
            determineServerInitialResponse();
            m_headerFinished = false; // Clean header flag for each HTTP request.
        }

        private void buildCommand() {
            /*
             * Sorting this map just in case the poller gets changed and the Map
             * is no longer a TreeMap.
             */
            final StringBuilder sb = new StringBuilder();
            sb.append("GET ").append(determineUrl(m_parameters)).append(" HTTP/1.1\r\n");
            sb.append("Connection: CLOSE \r\n");
            sb.append("Host: ").append(determineVirtualHost(m_addr, m_parameters)).append("\r\n");
            sb.append("User-Agent: ").append(determineUserAgent(m_parameters)).append("\r\n");

            if (determineBasicAuthentication(m_parameters) != null) {
                sb.append("Authorization: Basic ").append(determineBasicAuthentication(m_parameters)).append("\r\n");
            }

            for (final String parmKey : m_parameters.keySet()) {
                if (HEADER_PATTERN.matcher(parmKey).matches()) {
                    sb.append(determineHttpHeader(m_parameters, parmKey)).append("\r\n");
                }
            }

            sb.append("\r\n");
            final String cmd = sb.toString();
            if (HttpMonitor.LOG.isDebugEnabled()) {
                HttpMonitor.LOG.debug("checkStatus: cmd:\n", cmd);
            }
            m_httpCmd = cmd;
        }

        public void setReason(final String reason) {
            m_reason = reason;
        }

        public String getReason() {
            return m_reason;
        }

        public Socket getHttpSocket() {
            return m_httpSocket;
        }

        public void setHttpSocket(final Socket httpSocket) {
            m_httpSocket = httpSocket;
        }

        protected PollStatus determinePollStatusResponse() {
            /*
             Add the 'qualifier' parm to the parameter map. This parm will
             contain the port on which the service was found if AVAILABLE or
             will contain a comma delimited list of the port(s) which were
             tried if the service is UNAVAILABLE
            */

            if (getPollStatus() == PollStatus.SERVICE_UNAVAILABLE) {
                //
                // Build port string
                //
                final StringBuilder testedPorts = new StringBuilder();
                for (int i = 0; i < determinePorts(getParameters()).length; i++) {
                    if (i == 0) {
                        testedPorts.append(determinePorts(getParameters())[0]);
                    } else {
                        testedPorts.append(',').append(determinePorts(getParameters())[i]);
                    }
                }

                // Add to parameter map
                getParameters().put("qualifier", testedPorts.toString());
                setReason(getReason() + "/Ports: " + testedPorts.toString());

                if (HttpMonitor.LOG.isDebugEnabled()) {
                    HttpMonitor.LOG.debug("checkStatus: Reason: \""+getReason()+"\"");
                }
                return PollStatus.unavailable(getReason());

            } else if (getPollStatus() == PollStatus.SERVICE_AVAILABLE) {
                getParameters().put("qualifier", Integer.toString(getCurrentPort()));
                return PollStatus.available(getResponseTime());
            } else {
                return PollStatus.get(getPollStatus(), getReason());
            }
        }

    }

    protected static String resolveKeyedString(final Map<String, Object> parameterMap, final String key, final String defaultValue) {
        String ret = ParameterMap.getKeyedString(parameterMap, key, defaultValue);
        String subKey = "subbed-" + key;
        if (parameterMap.containsKey(subKey)) {
            ret = ParameterMap.getKeyedString(parameterMap, subKey, defaultValue);
        }
        return ret;
    }

}
