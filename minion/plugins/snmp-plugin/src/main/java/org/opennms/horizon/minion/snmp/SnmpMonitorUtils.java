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
package org.opennms.horizon.minion.snmp;

import java.math.BigInteger;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.regex.Pattern;
import lombok.Setter;
import org.opennms.horizon.minion.plugin.api.MonitoredService;
import org.opennms.horizon.shared.snmp.SnmpAgentConfig;
import org.opennms.horizon.shared.snmp.SnmpValue;
import org.opennms.snmp.contract.SnmpMonitorRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>Abstract SnmpMonitorStrategy class.</p>
 *
 * @author david
 * @version $Id: $
 */
public class SnmpMonitorUtils {

    private static final Logger LOG = LoggerFactory.getLogger(SnmpMonitorUtils.class);

    /**
     * Constant for less-than operand
     */
    public static final String LESS_THAN = "<";
    /** Constant <code>GREATER_THAN=">"</code> */
    public static final String GREATER_THAN = ">";
    /** Constant <code>LESS_THAN_EQUALS="<="</code> */
    public static final String LESS_THAN_EQUALS = "<=";
    /** Constant <code>GREATER_THAN_EQUALS=">="</code> */
    public static final String GREATER_THAN_EQUALS = ">=";
    /** Constant <code>EQUALS="="</code> */
    public static final String EQUALS = "=";
    /** Constant <code>NOT_EQUAL="!="</code> */
    public static final String NOT_EQUAL = "!=";
    /** Constant <code>MATCHES="~"</code> */
    public static final String MATCHES = "~";

    protected static boolean hex = false;

    // Wrap InetAddress static calls in a test-friendly injectable
    @Setter
    private FunctionWithException<String, InetAddress, UnknownHostException> inetLookupOperation =
            InetAddress::getByName;

    public SnmpAgentConfig getAgentConfig(MonitoredService svc, SnmpMonitorRequest snmpMonitorRequest)
            throws UnknownHostException {
        // return getKeyedInstance(parameters, "agent", () -> { return new SnmpAgentConfig(svc.getAddress()); });
        return new SnmpAgentConfig(inetLookupOperation.call(snmpMonitorRequest.getHost()));
    }

    public static String getStringValue(SnmpValue result) {
        // TODO: what is hex ?
        if (hex) return result.toHexString();
        return result.toString();
    }

    /**
     * Verifies that the result of the SNMP query meets the criteria specified
     * by the operator and the operand from the configuration file.
     *
     * @param result a {@link org.opennms.horizon.shared.snmp.SnmpValue} object.
     * @param operator a {@link String} object.
     * @param operand a {@link String} object.
     * @return a boolean.
     */
    public static boolean meetsCriteria(SnmpValue result, String operator, String operand) {

        Boolean retVal = null;

        retVal = isCriteriaNull(result, operator, operand);

        if (retVal == null) {
            String value = getStringValue(result);
            retVal = checkStringCriteria(operator, operand, value);

            if (retVal == null) {

                BigInteger val = BigInteger.valueOf(result.toLong());

                BigInteger intOperand = new BigInteger(operand);
                if (LESS_THAN.equals(operator)) {
                    return val.compareTo(intOperand) < 0;
                } else if (LESS_THAN_EQUALS.equals(operator)) {
                    return val.compareTo(intOperand) <= 0;
                } else if (GREATER_THAN.equals(operator)) {
                    return val.compareTo(intOperand) > 0;
                } else if (GREATER_THAN_EQUALS.equals(operator)) {
                    return val.compareTo(intOperand) >= 0;
                } else {
                    throw new IllegalArgumentException("operator " + operator + " is unknown");
                }
            }
        } else if (retVal.booleanValue()) {
            return true;
        }

        return retVal.booleanValue();
    }

    /**
     * @param operator
     * @param operand
     * @param value
     * @return
     */
    private static Boolean checkStringCriteria(final String operator, String operand, String value) {
        Boolean retVal = null;

        if (value == null) {
            value = "";
        } else if (value.startsWith(".")) {
            value = value.substring(1);
        }

        // Bug 2178 -- if this is a regex match, a leading "." in the operand
        // should not be stripped
        if (operand.startsWith(".") && !MATCHES.equals(operator)) {
            operand = operand.substring(1);
        }

        if (EQUALS.equals(operator)) retVal = Boolean.valueOf(operand.equals(value));
        else if (NOT_EQUAL.equals(operator)) retVal = Boolean.valueOf(!operand.equals(value));
        else if (MATCHES.equals(operator))
            retVal = Boolean.valueOf(Pattern.compile(operand).matcher(value).find());
        return retVal;
    }

    /**
     * @param result
     * @param operator
     * @param operand
     * @return
     */
    private static Boolean isCriteriaNull(Object result, String operator, String operand) {

        if (result == null) return Boolean.FALSE;
        if (operator == null || operand == null) {
            return Boolean.TRUE;
        } else {
            return null;
        }
    }

    // ========================================
    // Workarounds
    // ----------------------------------------

    // TODO
    @FunctionalInterface
    public interface FunctionWithException<ARG, RET, EXC extends Exception> {
        RET call(ARG arg) throws EXC;
    }
}
