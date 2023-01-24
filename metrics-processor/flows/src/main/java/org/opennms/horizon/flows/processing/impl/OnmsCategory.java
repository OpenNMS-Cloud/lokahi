/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2006-2014 The OpenNMS Group, Inc.
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

package org.opennms.horizon.flows.processing.impl;

import java.util.HashSet;
import java.util.Set;

import com.google.common.base.MoreObjects;

/**
 * <p>OnmsCategory class.</p>
 */
public class OnmsCategory implements Comparable<OnmsCategory> {

    /**
     * identifier field
     */
    private Integer m_id;

    /**
     * persistent field
     */
    private String m_name;

    /**
     * persistent field
     */
    private String m_description;

    private Set<String> m_authorizedGroups = new HashSet<>();

    //private Set<OnmsNode> m_memberNodes;

    /**
     * <p>Constructor for OnmsCategory.</p>
     *
     * @param name  a {@link String} object.
     * @param descr a {@link String} object.
     */
    public OnmsCategory(String name, String descr) {
        m_name = name;
        m_description = descr;
    }

    /**
     * default constructor
     */
    public OnmsCategory() {
    }

    /**
     * <p>Constructor for OnmsCategory.</p>
     *
     * @param name a {@link String} object.
     */
    public OnmsCategory(String name) {
        this();
        setName(name);
    }

    /**
     * <p>getId</p>
     *
     * @return a {@link Integer} object.
     */
    public Integer getId() {
        return m_id;
    }

    /**
     * <p>setId</p>
     *
     * @param id a {@link Integer} object.
     */
    public void setId(Integer id) {
        m_id = id;
    }

    /**
     * <p>getName</p>
     *
     * @return a {@link String} object.
     */
    public String getName() {
        return m_name;
    }

    /**
     * <p>setName</p>
     *
     * @param name a {@link String} object.
     */
    public void setName(String name) {
        m_name = name;
    }

    /**
     * <p>getDescription</p>
     *
     * @return a {@link String} object.
     */
    public String getDescription() {
        return m_description;
    }

    /**
     * <p>setDescription</p>
     *
     * @param description a {@link String} object.
     */
    public void setDescription(String description) {
        m_description = description;
    }

    /**
     * <p>getAuthorizedGroups</p>
     *
     * @return a {@link Set} object.
     */
    public Set<String> getAuthorizedGroups() {
        return m_authorizedGroups;
    }

    /**
     * <p>setAuthorizedGroups</p>
     *
     * @param authorizedGroups a {@link Set} object.
     */
    public void setAuthorizedGroups(Set<String> authorizedGroups) {
        m_authorizedGroups = authorizedGroups;
    }

    /**
     * <p>toString</p>
     *
     * @return a {@link String} object.
     */
    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
            .add("id", getId())
            .add("name", getName())
            .add("description", getDescription())
            .add("authorizedGroups", getAuthorizedGroups())
            .toString();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object obj) {
        if (obj instanceof OnmsCategory) {
            OnmsCategory t = (OnmsCategory) obj;
            return m_name.equals(t.m_name);
        }
        return false;
    }

    /**
     * <p>hashCode</p>
     *
     * @return a int.
     */
    @Override
    public int hashCode() {
        return m_name.hashCode();
    }

    /**
     * <p>compareTo</p>
     *
     * @param o a {@link OnmsCategory} object.
     * @return a int.
     */
    @Override
    public int compareTo(OnmsCategory o) {
        return m_name.compareToIgnoreCase(o.m_name);
    }
}
