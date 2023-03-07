/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2011-2014 The OpenNMS Group, Inc.
 * OpenNMS(R) is Copyright (C) 1999-2014 The OpenNMS Group, Inc.
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

package org.opennms.horizon.inventory.service.trapconfig;

import org.apache.commons.lang3.builder.ToStringBuilder;

import java.util.Objects;

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

    public SnmpV3User(String securityName, String authenticationProtocol,
            String authenticationPassphrase, String privacyProtocol,
            String privacyPassphrase) {
        super();
        this.securityName = securityName;
        this.authProtocol = authenticationProtocol;
        this.authPassphrase = authenticationPassphrase;
        this.privacyProtocol = privacyProtocol;
        this.privacyPassphrase = privacyPassphrase;
    }

    public SnmpV3User(String engineId, String securityName, String authenticationProtocol,
            String authenticationPassphrase, String privacyProtocol, String privacyPassphrase) {
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
        return Objects.hash(engineId, securityName, securityLevel, authPassphrase, privacyPassphrase, authProtocol, privacyProtocol);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SnmpV3User that = (SnmpV3User) o;
        return Objects.equals(engineId, that.engineId) &&
                Objects.equals(securityName, that.securityName) &&
                Objects.equals(securityLevel, that.securityLevel) &&
                Objects.equals(authPassphrase, that.authPassphrase) &&
                Objects.equals(privacyPassphrase, that.privacyPassphrase) &&
                Objects.equals(authProtocol, that.authProtocol) &&
                Objects.equals(privacyProtocol, that.privacyProtocol);
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
