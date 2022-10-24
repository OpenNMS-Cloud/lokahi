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

package org.opennms.horizon.db.model;

import com.google.common.base.MoreObjects;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import java.io.Serializable;

/**
 * <p>OnmsDataChoices class.</p>
 */
@Entity
@Table(name = "datachoices")
public class OnmsDataChoices implements Serializable {
    private static final long serialVersionUID = 7275548439687562161L;

    private Integer m_id;

    private String m_systemId;

    private Boolean m_enabled = false;

    /**
     * default constructor
     */
    public OnmsDataChoices() {
    }

    /**
     * <p>getId</p>
     *
     * @return a {@link Integer} object.
     */
    @Id
    @SequenceGenerator(name = "datachoiceSequence", sequenceName = "datachoiceNxtId", allocationSize = 1)
    @GeneratedValue(generator = "datachoiceSequence")
    @Column(name = "datachoiceid", nullable = false)
    public Integer getId() {
        return this.m_id;
    }

    /**
     * <p>setId</p>
     *
     * @param datachoicesid a {@link Integer} object.
     */
    public void setId(Integer datachoicesid) {
        this.m_id = datachoicesid;
    }

    /**
     * <p>getSystemId</p>
     *
     * @return a {@link String} object.
     */
    @Column(name = "systemid", length = 256, nullable = false)
    public String getSystemId() {
        return this.m_systemId;
    }

    /**
     * <p>setSystemId</p>
     *
     * @param systemid a {@link String} object.
     */
    public void setSystemId(String systemid) {
        this.m_systemId = systemid;
    }


    /**
     * <p>getEnabled</p>
     *
     * @return a {@link String} object.
     */
    @Column(name = "enabled", nullable = false)
    public Boolean getEnabled() {
        return this.m_enabled;
    }

    /**
     * <p>setEnabled</p>
     *
     * @param enabled a {@link Boolean} object.
     */
    public void setEnabled(Boolean enabled) {
        this.m_enabled = enabled;
    }


    /**
     * <p>toString</p>
     *
     * @return a {@link String} object.
     */
    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
            .add("datachoiceid", getId())
            .add("systemid", getSystemId())
            .add("enabled", getEnabled())
            .toString();
    }
}
