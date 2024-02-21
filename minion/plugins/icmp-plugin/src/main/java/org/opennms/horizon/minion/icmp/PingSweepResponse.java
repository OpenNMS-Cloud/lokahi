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
package org.opennms.horizon.minion.icmp;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

public class PingSweepResponse {

    private String error;

    private List<PingResult> pingResults = new ArrayList<>(0);

    public PingSweepResponse() {}

    public PingSweepResponse(Throwable ex) {
        error = toErrorMessage(ex);
    }

    public List<PingResult> getPingResults() {
        return pingResults;
    }

    public void setPingResults(List<PingResult> pingResults) {
        this.pingResults = pingResults;
    }

    public void addPingResult(PingResult pingResult) {
        this.pingResults.add(pingResult);
    }

    public String getErrorMessage() {
        return error;
    }

    public static String toErrorMessage(Throwable t) {
        if (t == null) {
            return null;
        }

        final StringWriter strackTrace = new StringWriter();
        final PrintWriter pw = new PrintWriter(strackTrace);
        t.printStackTrace(pw);
        return strackTrace.toString();
    }
}
