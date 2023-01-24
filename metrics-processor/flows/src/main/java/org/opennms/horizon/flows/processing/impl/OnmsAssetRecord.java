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

import java.beans.PropertyDescriptor;
import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.springframework.beans.BeanWrapper;
import org.springframework.beans.PropertyAccessorFactory;

import com.google.common.base.MoreObjects;

/**
 * Represents the asset information for a node.
 *
 */
public class OnmsAssetRecord implements Serializable {
    @Serial
    private static final long serialVersionUID = -8259333820682056097L;

    /**
     * Constant <code>AUTOENABLED="A"</code>
     */
    public static final String AUTOENABLED = "A";

    /**
     * Constant <code>SSH_CONNECTION="ssh"</code>
     */
    public static final String SSH_CONNECTION = "ssh";

    //public enum Autoenable {AUTOENABLED};

    /**
     * Constant <code>TELNET_CONNECTION="telnet"</code>
     */
    public static final String TELNET_CONNECTION = "telnet";

    /**
     * Constant <code>RSH_CONNECTION="rsh"</code>
     */
    public static final String RSH_CONNECTION = "rsh";

    //public enum AssetConnections {TELNET_CONNECTION,SSH_CONNECTION,RSH_CONNECTION};

    private Integer m_id;

    /**
     * identifier field
     */
    private String m_category = "Unspecified";

    /**
     * identifier field
     */
    private String m_manufacturer;

    /**
     * identifier field
     */
    private String m_vendor;

    /**
     * identifier field
     */
    private String m_modelNumber;

    /**
     * identifier field
     */
    private String m_serialNumber;

    /**
     * identifier field
     */
    private String m_description;

    /**
     * identifier field
     */
    private String m_circuitId;

    /**
     * identifier field
     */
    private String m_assetNumber;

    /**
     * identifier field
     */
    private String m_operatingSystem;

    /**
     * identifier field
     */
    private String m_rack;

    /**
     * identifier field
     */
    private String m_slot;

    /**
     * identifier field
     */
    private String m_port;

    /**
     * identifier field
     */
    private String m_region;

    /**
     * identifier field
     */
    private String m_division;

    /**
     * identifier field
     */
    private String m_department;

    /**
     * identifier field
     */
    private String m_building;

    /**
     * identifier field
     */
    private String m_floor;

    /**
     * identifier field
     */
    private String m_room;

    /**
     * identifier field
     */
    private String m_vendorPhone;

    /**
     * identifier field
     */
    private String m_vendorFax;

    /**
     * identifier field
     */
    private String m_vendorAssetNumber;

    /**
     * identifier field
     */
    private String m_username;

    /**
     * identifier field
     */
    private String m_password;

    /**
     * identifier field
     */
    private String m_enable;

    /**
     * identifier field
     */
    private String m_connection;

    /**
     * identifier field
     */
    private String m_autoenable;

    /**
     * identifier field
     */
    private String m_lastModifiedBy = "";

    /**
     * identifier field
     */
    private Date m_lastModifiedDate = new Date();

    /**
     * identifier field
     */
    private String m_dateInstalled;

    /**
     * identifier field
     */
    private String m_lease;

    /**
     * identifier field
     */
    private String m_leaseExpires;

    /**
     * identifier field
     */
    private String m_supportPhone;

    /**
     * identifier field
     */
    private String m_maintcontract;

    /**
     * identifier field
     */
    private String m_maintContractExpiration;

    /**
     * identifier field
     */
    private String m_displayCategory;

    /**
     * identifier field
     */
    private String m_notifyCategory;

    /**
     * identifier field
     */
    private String m_pollerCategory;

    /**
     * identifier field
     */
    private String m_thresholdCategory;

    /**
     * identifier field
     */
    private String m_comment;

    /**
     * identifier field
     */
    private String m_cpu;

    /**
     * identifier field
     */
    private String m_ram;

    /**
     * identifier field
     */
    private String m_storagectrl;

    /**
     * identifier field
     */
    private String m_hdd1;

    /**
     * identifier field
     */
    private String m_hdd2;

    /**
     * identifier field
     */
    private String m_hdd3;

    /**
     * identifier field
     */
    private String m_hdd4;

    /**
     * identifier field
     */
    private String m_hdd5;

    /**
     * identifier field
     */
    private String m_hdd6;

    /**
     * identifier field
     */
    private String m_numpowersupplies;

    /**
     * identifier field
     */
    private String m_inputpower;

    /**
     * identifier field
     */
    private String m_additionalhardware;

    /**
     * identifier field
     */
    private String m_admin;

    /**
     * identifier field
     */
    private String m_snmpcommunity;

    /**
     * identifier field
     */
    private String m_rackunitheight;

    /**
     * persistent field
     */
    private OnmsNode m_node;

    private String m_managedObjectType;

    private String m_managedObjectInstance;

    private OnmsGeolocation m_geolocation = new OnmsGeolocation();

    /**
     * default constructor
     */
    public OnmsAssetRecord() {
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
    public void setId(final Integer id) {
        m_id = id;
    }

    /**
     * The node this asset information belongs to.
     *
     * @return a {@link OnmsNode} object.
     */
    public OnmsNode getNode() {
        return m_node;
    }

    /**
     * Set the node associated with the asset record
     *
     * @param node a {@link OnmsNode} object.
     */
    public void setNode(OnmsNode node) {
        m_node = node;
    }

    /**
     * --# category         : A broad idea of what this asset does (examples are
     * --#                    desktop, printer, server, infrastructure, etc.).
     *
     * @return a {@link String} object.
     */
    public String getCategory() {
        return m_category;
    }

    /**
     * <p>setCategory</p>
     *
     * @param category a {@link String} object.
     */
    public void setCategory(final String category) {
        m_category = category;
    }

    /**
     * --# manufacturer     : Name of the manufacturer of this asset.
     *
     * @return a {@link String} object.
     */
    public String getManufacturer() {
        return m_manufacturer;
    }

    /**
     * <p>setManufacturer</p>
     *
     * @param manufacturer a {@link String} object.
     */
    public void setManufacturer(final String manufacturer) {
        m_manufacturer = manufacturer;
    }

    /**
     * --# vendor           : Vendor from whom this asset was purchased.
     *
     * @return a {@link String} object.
     */
    public String getVendor() {
        return m_vendor;
    }

    /**
     * <p>setVendor</p>
     *
     * @param vendor a {@link String} object.
     */
    public void setVendor(final String vendor) {
        m_vendor = vendor;
    }

    /**
     * --# modelNumber      : The model number of this asset.
     *
     * @return a {@link String} object.
     */
    public String getModelNumber() {
        return m_modelNumber;
    }

    /**
     * <p>setModelNumber</p>
     *
     * @param modelnumber a {@link String} object.
     */
    public void setModelNumber(final String modelnumber) {
        m_modelNumber = modelnumber;
    }

    /**
     * --# serialNumber     : The serial number of this asset.
     *
     * @return a {@link String} object.
     */
    public String getSerialNumber() {
        return m_serialNumber;
    }

    /**
     * <p>setSerialNumber</p>
     *
     * @param serialnumber a {@link String} object.
     */
    public void setSerialNumber(final String serialnumber) {
        m_serialNumber = serialnumber;
    }

    /**
     * --# description      : A free-form description.
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
    public void setDescription(final String description) {
        m_description = description;
    }

    /**
     * --# circuitId        : The electrical/network circuit this asset connects to.
     *
     * @return a {@link String} object.
     */
    public String getCircuitId() {
        return m_circuitId;
    }

    /**
     * <p>setCircuitId</p>
     *
     * @param circuitid a {@link String} object.
     */
    public void setCircuitId(final String circuitid) {
        m_circuitId = circuitid;
    }

    /**
     * --# assetNumber      : A business-specified asset number.
     *
     * @return a {@link String} object.
     */
    public String getAssetNumber() {
        return m_assetNumber;
    }

    /**
     * <p>setAssetNumber</p>
     *
     * @param assetnumber a {@link String} object.
     */
    public void setAssetNumber(final String assetnumber) {
        m_assetNumber = assetnumber;
    }

    /**
     * --# operatingSystem  : The operating system, if any.
     *
     * @return a {@link String} object.
     */

    public String getOperatingSystem() {
        return m_operatingSystem;
    }

    /**
     * <p>setOperatingSystem</p>
     *
     * @param operatingsystem a {@link String} object.
     */
    public void setOperatingSystem(final String operatingsystem) {
        m_operatingSystem = operatingsystem;
    }

    /**
     * --# rack             : For servers, the rack it is installed in.
     *
     * @return a {@link String} object.
     */
    public String getRack() {
        return m_rack;
    }

    /**
     * <p>setRack</p>
     *
     * @param rack a {@link String} object.
     */
    public void setRack(final String rack) {
        m_rack = rack;
    }

    /**
     * --# slot             : For servers, the slot in the rack it is installed in.
     *
     * @return a {@link String} object.
     */
    public String getSlot() {
        return m_slot;
    }

    /**
     * <p>setSlot</p>
     *
     * @param slot a {@link String} object.
     */
    public void setSlot(final String slot) {
        m_slot = slot;
    }

    /**
     * --# port             : For servers, the port in the slot it is installed in.
     *
     * @return a {@link String} object.
     */
    public String getPort() {
        return m_port;
    }

    /**
     * <p>setPort</p>
     *
     * @param port a {@link String} object.
     */
    public void setPort(final String port) {
        m_port = port;
    }

    /**
     * --# region           : A broad geographical or organizational area.
     *
     * @return a {@link String} object.
     */
    public String getRegion() {
        return m_region;
    }

    /**
     * <p>setRegion</p>
     *
     * @param region a {@link String} object.
     */
    public void setRegion(final String region) {
        m_region = region;
    }

    /**
     * --# division         : A broad geographical or organizational area.
     *
     * @return a {@link String} object.
     */
    public String getDivision() {
        return m_division;
    }

    /**
     * <p>setDivision</p>
     *
     * @param division a {@link String} object.
     */
    public void setDivision(final String division) {
        m_division = division;
    }

    /**
     * --# department       : The department this asset belongs to.
     *
     * @return a {@link String} object.
     */
    public String getDepartment() {
        return m_department;
    }

    /**
     * <p>setDepartment</p>
     *
     * @param department a {@link String} object.
     */
    public void setDepartment(final String department) {
        m_department = department;
    }

    public OnmsGeolocation getGeolocation() {
        return m_geolocation;
    }

    public void setGeolocation(final OnmsGeolocation geolocation) {
        m_geolocation = geolocation;
    }

    /**
     * --# building         : The building where this asset resides.
     *
     * @return a {@link String} object.
     */
    public String getBuilding() {
        return m_building;
    }

    /**
     * <p>setBuilding</p>
     *
     * @param building a {@link String} object.
     */
    public void setBuilding(final String building) {
        m_building = building;
    }

    /**
     * --# floor            : The floor of the building where this asset resides.
     *
     * @return a {@link String} object.
     */
    public String getFloor() {
        return m_floor;
    }

    /**
     * <p>setFloor</p>
     *
     * @param floor a {@link String} object.
     */
    public void setFloor(final String floor) {
        m_floor = floor;
    }

    /**
     * --# room             : The room where this asset resides.
     *
     * @return a {@link String} object.
     */
    public String getRoom() {
        return m_room;
    }

    /**
     * <p>setRoom</p>
     *
     * @param room a {@link String} object.
     */
    public void setRoom(final String room) {
        m_room = room;
    }

    /**
     * --# vendorPhone      : A contact number for the vendor.
     *
     * @return a {@link String} object.
     */
    public String getVendorPhone() {
        return m_vendorPhone;
    }

    /**
     * <p>setVendorPhone</p>
     *
     * @param vendorphone a {@link String} object.
     */
    public void setVendorPhone(final String vendorphone) {
        m_vendorPhone = vendorphone;
    }

    /**
     * --# vendorFax        : A fax number for the vendor.
     *
     * @return a {@link String} object.
     */
    public String getVendorFax() {
        return m_vendorFax;
    }

    /**
     * <p>setVendorFax</p>
     *
     * @param vendorfax a {@link String} object.
     */
    public void setVendorFax(final String vendorfax) {
        m_vendorFax = vendorfax;
    }

    /**
     * <p>getVendorAssetNumber</p>
     *
     * @return a {@link String} object.
     */
    public String getVendorAssetNumber() {
        return m_vendorAssetNumber;
    }

    /**
     * <p>setVendorAssetNumber</p>
     *
     * @param vendorassetnumber a {@link String} object.
     */
    public void setVendorAssetNumber(final String vendorassetnumber) {
        m_vendorAssetNumber = vendorassetnumber;
    }

    /**
     * --# userLastModified : The last user who modified this record.
     *
     * @return a {@link String} object.
     */
    public String getLastModifiedBy() {
        return m_lastModifiedBy == null? null : m_lastModifiedBy.trim();
    }

    /**
     * <p>setLastModifiedBy</p>
     *
     * @param userlastmodified a {@link String} object.
     */
    public void setLastModifiedBy(final String userlastmodified) {
        m_lastModifiedBy = userlastmodified;
    }

    /**
     * --# lastModifiedDate : The last time this record was modified.
     *
     * @return a {@link Date} object.
     */
    public Date getLastModifiedDate() {
        return m_lastModifiedDate;
    }

    /**
     * <p>setLastModifiedDate</p>
     *
     * @param lastmodifieddate a {@link Date} object.
     */
    public void setLastModifiedDate(final Date lastmodifieddate) {
        m_lastModifiedDate = lastmodifieddate;
    }

    /**
     * --# dateInstalled    : The date the asset was installed.
     *
     * @return a {@link String} object.
     */
    public String getDateInstalled() {
        return m_dateInstalled;
    }

    /**
     * <p>setDateInstalled</p>
     *
     * @param dateinstalled a {@link String} object.
     */
    public void setDateInstalled(final String dateinstalled) {
        m_dateInstalled = dateinstalled;
    }

    /**
     * --# lease            : The lease number of this asset.
     *
     * @return a {@link String} object.
     */
    public String getLease() {
        return m_lease;
    }

    /**
     * <p>setLease</p>
     *
     * @param lease a {@link String} object.
     */
    public void setLease(final String lease) {
        m_lease = lease;
    }

    /**
     * --# leaseExpires     : The date the lease expires for this asset.
     *
     * @return a {@link String} object.
     */
    public String getLeaseExpires() {
        return m_leaseExpires;
    }

    /**
     * <p>setLeaseExpires</p>
     *
     * @param leaseexpires a {@link String} object.
     */
    public void setLeaseExpires(final String leaseexpires) {
        m_leaseExpires = leaseexpires;
    }

    /**
     * --# supportPhone     : A support phone number for this asset.
     *
     * @return a {@link String} object.
     */
    public String getSupportPhone() {
        return m_supportPhone;
    }

    /**
     * <p>setSupportPhone</p>
     *
     * @param supportphone a {@link String} object.
     */
    public void setSupportPhone(final String supportphone) {
        m_supportPhone = supportphone;
    }

    /**
     * --# maintcontract    : The maintenance contract number for this asset.
     *
     * @return a {@link String} object.
     */
    public String getMaintcontract() {
        return m_maintcontract;
    }

    /**
     * <p>setMaintContract</p>
     *
     * @param maintcontract a {@link String} object.
     */
    public void setMaintcontract(final String maintcontract) {
        m_maintcontract = maintcontract;
    }

    /**
     * --# maintContractNumber: The maintenance contract number for this asset.
     *
     * @return a {@link String} object.
     * @deprecated This field is provided for backwards compatibility with OpenNMS &lt; 1.10
     */
    public String getMaintContractNumber() {
        return getMaintcontract();
    }

    /**
     * <p>setMaintContractNumber</p>
     *
     * @param maintcontract a {@link String} object.
     * @deprecated This field is provided for backwards compatibility with OpenNMS &lt; 1.10
     */
    public void setMaintContractNumber(final String maintcontract) {
        setMaintcontract(maintcontract);
    }

    /**
     * <p>getMaintContractExpiration</p>
     *
     * @return a {@link String} object.
     */
    public String getMaintContractExpiration() {
        return m_maintContractExpiration;
    }

    /**
     * <p>setMaintContractExpiration</p>
     *
     * @param maintcontractexpires a {@link String} object.
     */
    public void setMaintContractExpiration(final String maintcontractexpires) {
        m_maintContractExpiration = maintcontractexpires;
    }

    /**
     * <p>getDisplayCategory</p>
     *
     * @return a {@link String} object.
     */
    public String getDisplayCategory() {
        return m_displayCategory;
    }

    /**
     * <p>setDisplayCategory</p>
     *
     * @param displaycategory a {@link String} object.
     */
    public void setDisplayCategory(final String displaycategory) {
        m_displayCategory = displaycategory;
    }

    /**
     * <p>getNotifyCategory</p>
     *
     * @return a {@link String} object.
     */
    public String getNotifyCategory() {
        return m_notifyCategory;
    }

    /**
     * <p>setNotifyCategory</p>
     *
     * @param notifycategory a {@link String} object.
     */
    public void setNotifyCategory(final String notifycategory) {
        m_notifyCategory = notifycategory;
    }

    /**
     * <p>getPollerCategory</p>
     *
     * @return a {@link String} object.
     */
    public String getPollerCategory() {
        return m_pollerCategory;
    }

    /**
     * <p>setPollerCategory</p>
     *
     * @param pollercategory a {@link String} object.
     */
    public void setPollerCategory(final String pollercategory) {
        m_pollerCategory = pollercategory;
    }

    /**
     * <p>getThresholdCategory</p>
     *
     * @return a {@link String} object.
     */
    public String getThresholdCategory() {
        return m_thresholdCategory;
    }

    /**
     * <p>setThresholdCategory</p>
     *
     * @param thresholdcategory a {@link String} object.
     */
    public void setThresholdCategory(final String thresholdcategory) {
        m_thresholdCategory = thresholdcategory;
    }

    /**
     * <p>getComment</p>
     *
     * @return a {@link String} object.
     */
    public String getComment() {
        return m_comment;
    }

    /**
     * <p>setComment</p>
     *
     * @param comment a {@link String} object.
     */
    public void setComment(final String comment) {
        m_comment = comment;
    }

    /**
     * <p>getManagedObjectType</p>
     *
     * @return a {@link String} object.
     */
    public String getManagedObjectType() {
        return m_managedObjectType;
    }

    /**
     * <p>setManagedObjectType</p>
     *
     * @param mot a {@link String} object.
     */
    public void setManagedObjectType(final String mot) {
        m_managedObjectType = mot;
    }

    /**
     * <p>getManagedObjectInstance</p>
     *
     * @return a {@link String} object.
     */
    public String getManagedObjectInstance() {
        return m_managedObjectInstance;
    }

    /**
     * <p>setManagedObjectInstance</p>
     *
     * @param moi a {@link String} object.
     */
    public void setManagedObjectInstance(final String moi) {
        m_managedObjectInstance = moi;
    }

    /**
     * <p>getUsername</p>
     *
     * @return a {@link String} object.
     */
    public String getUsername() {
        return m_username;
    }

    /**
     * <p>setUsername</p>
     *
     * @param username a {@link String} object.
     */
    public void setUsername(final String username) {
        m_username = username;
    }

    /**
     * <p>getPassword</p>
     *
     * @return a {@link String} object.
     */
    public String getPassword() {
        return m_password;
    }

    /**
     * <p>setPassword</p>
     *
     * @param password a {@link String} object.
     */
    public void setPassword(final String password) {
        m_password = password;
    }

    /**
     * <p>getEnable</p>
     *
     * @return a {@link String} object.
     */
    public String getEnable() {
        return m_enable;
    }

    /**
     * <p>setEnable</p>
     *
     * @param enable a {@link String} object.
     */
    public void setEnable(final String enable) {
        m_enable = enable;
    }

    /**
     * <p>getConnection</p>
     *
     * @return a {@link String} object.
     */
    public String getConnection() {
        return m_connection;
    }

    /**
     * <p>setConnection</p>
     *
     * @param connection a {@link String} object.
     */
    public void setConnection(final String connection) {
        if (TELNET_CONNECTION.equalsIgnoreCase(connection)) {
            m_connection = TELNET_CONNECTION;
        } else if (SSH_CONNECTION.equalsIgnoreCase(connection)) {
            m_connection = SSH_CONNECTION;
        } else if (RSH_CONNECTION.equalsIgnoreCase(connection)) {
            m_connection = RSH_CONNECTION;
        } else {
            m_connection = connection;
        }
    }

    /**
     * <p>getAutoenable</p>
     *
     * @return a {@link String} object.
     */
    public String getAutoenable() {
        return m_autoenable;
    }

    /**
     * <p>setAutoenable</p>
     *
     * @param autoenable a {@link String} object.
     */
    public void setAutoenable(final String autoenable) {
        m_autoenable = autoenable;
    }

    /**
     * <p>getCpu</p>
     *
     * @return a {@link String} object.
     */
    public String getCpu() {
        return m_cpu;
    }

    /**
     * <p>setCpu</p>
     *
     * @param cpu a {@link String} object.
     */
    public void setCpu(final String cpu) {
        m_cpu = cpu;
    }

    /**
     * <p>getRam</p>
     *
     * @return a {@link String} object.
     */
    public String getRam() {
        return m_ram;
    }

    /**
     * <p>setRam</p>
     *
     * @param ram a {@link String} object.
     */
    public void setRam(final String ram) {
        m_ram = ram;
    }

    /**
     * <p>getSnmpcommunity</p>
     *
     * @return a {@link String} object.
     */
    public String getSnmpcommunity() {
        return m_snmpcommunity;
    }

    /**
     * <p>setSnmpcommunity</p>
     *
     * @param snmpcommunity a {@link String} object.
     */
    public void setSnmpcommunity(final String snmpcommunity) {
        m_snmpcommunity = snmpcommunity;
    }

    /**
     * <p>getRackunitheight</p>
     *
     * @return a {@link String} object.
     */
    public String getRackunitheight() {
        return m_rackunitheight;
    }

    public void setRackunitheight(final String rackunitheight) {
        m_rackunitheight = rackunitheight;
    }

    /**
     * <p>getAdmin</p>
     *
     * @return a {@link String} object.
     */
    public String getAdmin() {
        return m_admin;
    }

    /**
     * <p>setAdmin</p>
     *
     * @param admin a {@link String} object.
     */
    public void setAdmin(final String admin) {
        m_admin = admin;
    }

    /**
     * <p>getAdditionalhardware</p>
     *
     * @return a {@link String} object.
     */
    public String getAdditionalhardware() {
        return m_additionalhardware;
    }

    /**
     * <p>setAdditionalhardware</p>
     *
     * @param additionalhardware a {@link String} object.
     */
    public void setAdditionalhardware(final String additionalhardware) {
        m_additionalhardware = additionalhardware;
    }

    /**
     * <p>getInputpower</p>
     *
     * @return a {@link String} object.
     */
    public String getInputpower() {
        return m_inputpower;
    }

    /**
     * <p>setInputpower</p>
     *
     * @param inputpower a {@link String} object.
     */
    public void setInputpower(final String inputpower) {
        m_inputpower = inputpower;
    }

    /**
     * <p>getNumpowersupplies</p>
     *
     * @return a {@link String} object.
     */
    public String getNumpowersupplies() {
        return m_numpowersupplies;
    }

    /**
     * <p>setNumpowersupplies</p>
     *
     * @param numpowersupplies a {@link String} object.
     */
    public void setNumpowersupplies(final String numpowersupplies) {
        m_numpowersupplies = numpowersupplies;
    }

    /**
     * <p>getHdd6</p>
     *
     * @return a {@link String} object.
     */
    public String getHdd6() {
        return m_hdd6;
    }

    /**
     * <p>setHdd6</p>
     *
     * @param hdd6 a {@link String} object.
     */
    public void setHdd6(final String hdd6) {
        m_hdd6 = hdd6;
    }

    /**
     * <p>getHdd5</p>
     *
     * @return a {@link String} object.
     */
    public String getHdd5() {
        return m_hdd5;
    }

    /**
     * <p>setHdd5</p>
     *
     * @param hdd5 a {@link String} object.
     */
    public void setHdd5(final String hdd5) {
        m_hdd5 = hdd5;
    }

    /**
     * <p>getHdd4</p>
     *
     * @return a {@link String} object.
     */
    public String getHdd4() {
        return m_hdd4;
    }

    /**
     * <p>setHdd4</p>
     *
     * @param hdd4 a {@link String} object.
     */
    public void setHdd4(final String hdd4) {
        m_hdd4 = hdd4;
    }

    /**
     * <p>getHdd3</p>
     *
     * @return a {@link String} object.
     */
    public String getHdd3() {
        return m_hdd3;
    }

    /**
     * <p>setHdd3</p>
     *
     * @param hdd3 a {@link String} object.
     */
    public void setHdd3(final String hdd3) {
        m_hdd3 = hdd3;
    }

    /**
     * <p>getHdd2</p>
     *
     * @return a {@link String} object.
     */
    public String getHdd2() {
        return m_hdd2;
    }

    /**
     * <p>setHdd2</p>
     *
     * @param hdd2 a {@link String} object.
     */
    public void setHdd2(final String hdd2) {
        m_hdd2 = hdd2;
    }

    /**
     * <p>getHdd1</p>
     *
     * @return a {@link String} object.
     */
    public String getHdd1() {
        return m_hdd1;
    }

    /**
     * <p>setHdd1</p>
     *
     * @param hdd1 a {@link String} object.
     */
    public void setHdd1(final String hdd1) {
        m_hdd1 = hdd1;
    }

    /**
     * <p>getStoragectrl</p>
     *
     * @return a {@link String} object.
     */
    public String getStoragectrl() {
        return m_storagectrl;
    }

    /**
     * <p>setStoragectrl</p>
     *
     * @param storagectrl a {@link String} object.
     */
    public void setStoragectrl(final String storagectrl) {
        m_storagectrl = storagectrl;
    }

    /**
     * PROXY METHOD: do not delete until {@link OnmsGeolocation} is truly a separate table, or projection mapping will fail.
     */
    public String getAddress1() {
        return m_geolocation == null ? null : m_geolocation.getAddress1();
    }

    @Deprecated
    public void setAddress1(final String address1) {
        if (m_geolocation != null)
            m_geolocation.setAddress1(address1);
    }

    /**
     * PROXY METHOD: do not delete until {@link OnmsGeolocation} is truly a separate table, or projection mapping will fail.
     */
    public String getAddress2() {
        return m_geolocation == null ? null : m_geolocation.getAddress2();
    }

    @Deprecated
    public void setAddress2(final String address2) {
        if (m_geolocation != null)
            m_geolocation.setAddress2(address2);
    }

    /**
     * PROXY METHOD: do not delete until {@link OnmsGeolocation} is truly a separate table, or projection mapping will fail.
     */
    public String getCity() {
        return m_geolocation == null ? null : m_geolocation.getCity();
    }

    @Deprecated
    public void setCity(final String city) {
        if (m_geolocation != null)
            m_geolocation.setCity(city);
    }

    /**
     * PROXY METHOD: do not delete until {@link OnmsGeolocation} is truly a separate table, or projection mapping will fail.
     */
    public String getState() {
        return m_geolocation == null ? null : m_geolocation.getState();
    }

    @Deprecated
    public void setState(final String state) {
        if (m_geolocation != null)
            m_geolocation.setState(state);
    }

    /**
     * PROXY METHOD: do not delete until {@link OnmsGeolocation} is truly a separate table, or projection mapping will fail.
     */
    public String getZip() {
        return m_geolocation == null ? null : m_geolocation.getZip();
    }

    @Deprecated
    public void setZip(final String zip) {
        if (m_geolocation != null)
            m_geolocation.setZip(zip);
    }

    /**
     * PROXY METHOD: do not delete until {@link OnmsGeolocation} is truly a separate table, or projection mapping will fail.
     */
    public String getCountry() {
        return m_geolocation == null ? null : m_geolocation.getCountry();
    }

    @Deprecated
    public void setCountry(final String country) {
        if (m_geolocation != null)
            m_geolocation.setCountry(country);
    }

    /**
     * PROXY METHOD: do not delete until {@link OnmsGeolocation} is truly a separate table, or projection mapping will fail.
     */
    public Double getLongitude() {
        return m_geolocation == null ? null : m_geolocation.getLongitude();
    }

    @Deprecated
    public void setLongitude(final Double longitude) {
        if (m_geolocation != null)
            m_geolocation.setLongitude(longitude);
    }

    /**
     * PROXY METHOD: do not delete until {@link OnmsGeolocation} is truly a separate table, or projection mapping will fail.
     */
    public Double getLatitude() {
        return m_geolocation == null ? null : m_geolocation.getLatitude();
    }

    @Deprecated
    public void setLatitude(final Double latitude) {
        if (m_geolocation != null)
            m_geolocation.setLatitude(latitude);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
        .add("category", getCategory())
        .add("manufacturer", getManufacturer())
        .add("vendor", getVendor())
        .add("modelnumber", getModelNumber())
        .add("serialnumber", getSerialNumber())
        .add("description", getDescription())
        .add("circuitid", getCircuitId())
        .add("assetnumber", getAssetNumber())
        .add("operatingsystem", getOperatingSystem())
        .add("rack", getRack())
        .add("slot", getSlot())
        .add("port", getPort())
        .add("region", getRegion())
        .add("division", getDivision())
        .add("department", getDepartment())
        .add("address1", m_geolocation == null ? null : m_geolocation.getAddress1())
        .add("address2", m_geolocation == null ? null : m_geolocation.getAddress2())
        .add("city", m_geolocation == null ? null : m_geolocation.getCity())
        .add("state", m_geolocation == null ? null : m_geolocation.getState())
        .add("zip", m_geolocation == null ? null : m_geolocation.getZip())
        .add("country", m_geolocation == null ? null : m_geolocation.getCountry())
        .add("longitude", m_geolocation == null ? null : m_geolocation.getLongitude())
        .add("latitude", m_geolocation == null ? null : m_geolocation.getLatitude())
        .add("building", getBuilding())
        .add("floor", getFloor())
        .add("room", getRoom())
        .add("username", getUsername())
        .add("password", getPassword())
        .add("enable", getEnable())
        .add("autoenable", getAutoenable())
        .add("connection", getConnection())
        .add("vendorphone", getVendorPhone())
        .add("vendorfax", getVendorFax())
        .add("vendorassetnumber", getVendorAssetNumber())
        .add("userlastmodified", getLastModifiedBy())
        .add("lastmodifieddate", getLastModifiedDate())
        .add("dateinstalled", getDateInstalled())
        .add("lease", getLease())
        .add("leaseexpires", getLeaseExpires())
        .add("supportphone", getSupportPhone())
        .add("maintcontract", getMaintcontract())
        .add("maintcontractexpires", getMaintContractExpiration())
        .add("displaycategory", getDisplayCategory())
        .add("notifycategory", getNotifyCategory())
        .add("pollercategory", getPollerCategory())
        .add("thresholdcategory", getThresholdCategory())
        .add("comment", getComment())
        .add("cpu", getCpu())
        .add("ram", getRam())
        .add("storagectrl", getStoragectrl())
        .add("hdd1", getHdd1())
        .add("hdd2", getHdd2())
        .add("hdd3", getHdd3())
        .add("hdd4", getHdd4())
        .add("hdd5", getHdd5())
        .add("hdd6", getHdd6())
        .add("numpowersupplies", getNumpowersupplies())
        .add("inputpower", getInputpower())
        .add("additionalhardware", getAdditionalhardware())
        .add("admin", getAdmin())
        .add("snmpcommunity", getSnmpcommunity())
        .add("rackunitheight", getRackunitheight())
        .toString();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj == null || obj.getClass() != this.getClass()) {
            throw new IllegalArgumentException("the Operation Object passed is either null or of the wrong class");
        }

        final OnmsAssetRecord cmpAsset = (OnmsAssetRecord) obj;

        final Integer newNodeId = cmpAsset.getNode().getId();
        if (newNodeId == null) {
            return false;
        }

        if (m_node.getId().equals(cmpAsset.getNode().getId())) {
            return true;
        }

        return false;
    }

    @Override
    public int hashCode() {
        return 223 * m_node.getId().hashCode();
    }

    /**
     * Used to merge the contents of one asset record to another.  If equals implementation
     * returns false, the merge is aborted.
     *
     * @param newRecord a {@link OnmsAssetRecord} object.
     */
    public void mergeRecord(OnmsAssetRecord newRecord) {

        if (!this.equals(newRecord)) {
            return;
        }

        OnmsGeolocation toGeolocation = this.getGeolocation();
        if (toGeolocation == null) {
            toGeolocation = new OnmsGeolocation();
            this.setGeolocation(toGeolocation);
        }
        final OnmsGeolocation fromGeolocation = newRecord.getGeolocation();

        //this works because all asset properties are strings
        //if the model dependencies ever change to not include spring, this will break
        final BeanWrapper currentBean = PropertyAccessorFactory.forBeanPropertyAccess(this);
        final BeanWrapper newBean = PropertyAccessorFactory.forBeanPropertyAccess(newRecord);
        final PropertyDescriptor[] pds = newBean.getPropertyDescriptors();

        // Don't update these properties
        final List<String> blackListedProperties = new ArrayList<>();
        blackListedProperties.add("class");
        blackListedProperties.add("city");
        blackListedProperties.add("zip");
        blackListedProperties.add("state");
        blackListedProperties.add("country");
        blackListedProperties.add("longitude");
        blackListedProperties.add("latitude");
        blackListedProperties.add("address1");
        blackListedProperties.add("address2");

        for (final PropertyDescriptor pd : pds) {
            final String propertyName = pd.getName();
            if (blackListedProperties.contains(propertyName)) {
                continue;
            }

            // This should never fail since both of these objects are of the same type
            if (newBean.getPropertyValue(propertyName) != null) {
                currentBean.setPropertyValue(propertyName, newBean.getPropertyValue(propertyName));
            }
        }

        toGeolocation.mergeGeolocation(fromGeolocation);
        setGeolocation(toGeolocation);
    }
}
