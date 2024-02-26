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
package org.opennms.horizon.inventory.service.trapconfig;

import java.util.Objects;
import org.apache.commons.lang3.builder.ToStringBuilder;

public class SnmpV3User {

    private String engineId;
    private String securityName;
    private Integer securityLevel;
    private String authPassphrase;
    private String privacyPassphrase;
    private String authProtocol;
    private String privacyProtocol;

    public SnmpV3User() {
        super();
    }

    public SnmpV3User(
            String securityName,
            String authenticationProtocol,
            String authenticationPassphrase,
            String privacyProtocol,
            String privacyPassphrase) {
        super();
        this.securityName = securityName;
        this.authProtocol = authenticationProtocol;
        this.authPassphrase = authenticationPassphrase;
        this.privacyProtocol = privacyProtocol;
        this.privacyPassphrase = privacyPassphrase;
    }

    public SnmpV3User(
            String engineId,
            String securityName,
            String authenticationProtocol,
            String authenticationPassphrase,
            String privacyProtocol,
            String privacyPassphrase) {
        this(securityName, authenticationProtocol, authenticationPassphrase, privacyProtocol, privacyPassphrase);
        this.engineId = engineId;
    }

    public String getEngineId() {
        return engineId;
    }

    public void setEngineId(String engineId) {
        this.engineId = engineId;
    }

    public String getSecurityName() {
        return securityName;
    }

    public void setSecurityName(String securityName) {
        this.securityName = securityName;
    }

    public Integer getSecurityLevel() {
        return securityLevel;
    }

    public void setSecurityLevel(Integer securityLevel) {
        this.securityLevel = securityLevel;
    }

    public String getAuthPassphrase() {
        return authPassphrase;
    }

    public void setAuthPassphrase(String authenticationPassphrase) {
        this.authPassphrase = authenticationPassphrase;
    }

    public String getPrivacyPassphrase() {
        return privacyPassphrase;
    }

    public void setPrivacyPassphrase(String privacyPassphrase) {
        this.privacyPassphrase = privacyPassphrase;
    }

    public String getAuthProtocol() {
        return authProtocol;
    }

    public void setAuthProtocol(String authenticationProtocol) {
        this.authProtocol = authenticationProtocol;
    }

    public String getPrivacyProtocol() {
        return privacyProtocol;
    }

    public void setPrivacyProtocol(String privacyProtocol) {
        this.privacyProtocol = privacyProtocol;
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                engineId,
                securityName,
                securityLevel,
                authPassphrase,
                privacyPassphrase,
                authProtocol,
                privacyProtocol);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SnmpV3User that = (SnmpV3User) o;
        return Objects.equals(engineId, that.engineId)
                && Objects.equals(securityName, that.securityName)
                && Objects.equals(securityLevel, that.securityLevel)
                && Objects.equals(authPassphrase, that.authPassphrase)
                && Objects.equals(privacyPassphrase, that.privacyPassphrase)
                && Objects.equals(authProtocol, that.authProtocol)
                && Objects.equals(privacyProtocol, that.privacyProtocol);
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("authPassphrase", authPassphrase)
                .append("authProtocol", authProtocol)
                .append("engineId", engineId)
                .append("privPassPhrase", privacyPassphrase)
                .append("privProtocol", privacyProtocol)
                .append("securityName", securityName)
                .toString();
    }
}
