/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2019 The OpenNMS Group, Inc.
 * OpenNMS(R) is Copyright (C) 1999-2019 The OpenNMS Group, Inc.
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

package org.opennms.horizon.core.configvars.impl;

import ch.hsr.geohash.GeoHash;
import com.google.common.base.Strings;
import lombok.Getter;
import lombok.Setter;
import org.opennms.configvars.ContextKey;
import org.opennms.configvars.EmptyScope;
import org.opennms.configvars.FallbackScope;
import org.opennms.configvars.MapScope;
import org.opennms.configvars.ObjectScope;
import org.opennms.configvars.Scope;
import org.opennms.horizon.core.configvars.EntityScopeProvider;
import org.opennms.horizon.shared.utils.InetAddressUtils;
import org.opennms.horizon.db.dao.api.IpInterfaceDao;
import org.opennms.horizon.db.dao.api.MonitoredServiceDao;
import org.opennms.horizon.db.dao.api.NodeDao;
import org.opennms.horizon.db.dao.api.SessionUtils;
import org.opennms.horizon.db.dao.api.SnmpInterfaceDao;
import org.opennms.horizon.db.model.OnmsAssetRecord;
import org.opennms.horizon.db.model.OnmsGeolocation;
import org.opennms.horizon.db.model.OnmsIpInterface;
import org.opennms.horizon.db.model.OnmsMetaData;
import org.opennms.horizon.db.model.OnmsMonitoredService;
import org.opennms.horizon.db.model.OnmsNode;
import org.opennms.horizon.db.model.OnmsSnmpInterface;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.opennms.horizon.core.configvars.EntityScopeContexts.ASSET;
import static org.opennms.horizon.core.configvars.EntityScopeContexts.INTERFACE;
import static org.opennms.horizon.core.configvars.EntityScopeContexts.NODE;
import static org.opennms.horizon.core.configvars.EntityScopeContexts.SERVICE;

public class EntityScopeProviderImpl implements EntityScopeProvider {

    @Getter @Setter
    private NodeDao nodeDao;

    @Getter @Setter
    private IpInterfaceDao ipInterfaceDao;

    @Getter @Setter
    private SnmpInterfaceDao snmpInterfaceDao;

    @Getter @Setter
    private MonitoredServiceDao monitoredServiceDao;

    @Getter @Setter
    private SessionUtils sessionUtils;

//========================================
// Operations
//----------------------------------------

    @Override
    public Scope getScopeForNode(Integer nodeId) {
        if (nodeId == null) {
            return EmptyScope.EMPTY;
        }

        return this.sessionUtils.withReadOnlyTransaction(() -> {
            final OnmsNode node = nodeDao.get(nodeId);
            if (node == null) {
                return EmptyScope.EMPTY;
            }

            List<Scope> scopes = new ArrayList<>();
            scopes.add(transform(Scope.ScopeName.NODE, node.getMetaData()));

            Scope nodeScope = new ObjectScope<>(Scope.ScopeName.NODE, node)
                    .map(NODE, "criteria", this::getNodeCriteria)
                    .map(NODE, "label", (n) -> Optional.ofNullable(n.getLabel()))
                    .map(NODE, "foreign-source", (n) -> Optional.ofNullable(n.getForeignSource()))
                    .map(NODE, "foreign-id", (n) -> Optional.ofNullable(n.getForeignId()))
                    .map(NODE, "netbios-domain", (n) -> Optional.ofNullable(n.getNetBiosDomain()))
                    .map(NODE, "netbios-name", (n) -> Optional.ofNullable(n.getNetBiosName()))
                    .map(NODE, "os", (n) -> Optional.ofNullable(n.getOperatingSystem()))
                    .map(NODE, "sys-name", (n) -> Optional.ofNullable(n.getSysName()))
                    .map(NODE, "sys-location", (n) -> Optional.ofNullable(n.getSysLocation()))
                    .map(NODE, "sys-contact", (n) -> Optional.ofNullable(n.getSysContact()))
                    .map(NODE, "sys-description", (n) -> Optional.ofNullable(n.getSysDescription()))
                    .map(NODE, "sys-object-id", (n) -> Optional.ofNullable(n.getSysObjectId()))
                    .map(NODE, "location", (n) -> Optional.ofNullable(n.getLocation().getLocationName()))
                    .map(NODE, "area", (n) -> Optional.ofNullable(n.getLocation().getMonitoringArea()))
                    .map(NODE, "geohash", this::getNodeGeoHash);
            scopes.add(nodeScope);

            if (node.getAssetRecord() != null) {
                Scope assetScope = new ObjectScope<>(Scope.ScopeName.NODE, node.getAssetRecord())
                        .map(ASSET, "category", (a) -> Optional.ofNullable(a.getCategory()))
                        .map(ASSET, "manufacturer", (a) -> Optional.ofNullable(a.getManufacturer()))
                        .map(ASSET, "vendor", (a) -> Optional.ofNullable(a.getVendor()))
                        .map(ASSET, "model-number", (a) -> Optional.ofNullable(a.getModelNumber()))
                        .map(ASSET, "serial-number", (a) -> Optional.ofNullable(a.getSerialNumber()))
                        .map(ASSET, "description", (a) -> Optional.ofNullable(a.getDescription()))
                        .map(ASSET, "circuit-id", (a) -> Optional.ofNullable(a.getCircuitId()))
                        .map(ASSET, "asset-number", (a) -> Optional.ofNullable(a.getAssetNumber()))
                        .map(ASSET, "operating-system", (a) -> Optional.ofNullable(a.getOperatingSystem()))
                        .map(ASSET, "rack", (a) -> Optional.ofNullable(a.getRack()))
                        .map(ASSET, "slot", (a) -> Optional.ofNullable(a.getSlot()))
                        .map(ASSET, "port", (a) -> Optional.ofNullable(a.getPort()))
                        .map(ASSET, "region", (a) -> Optional.ofNullable(a.getRegion()))
                        .map(ASSET, "division", (a) -> Optional.ofNullable(a.getDivision()))
                        .map(ASSET, "department", (a) -> Optional.ofNullable(a.getDepartment()))
                        .map(ASSET, "building", (a) -> Optional.ofNullable(a.getBuilding()))
                        .map(ASSET, "floor", (a) -> Optional.ofNullable(a.getFloor()))
                        .map(ASSET, "room", (a) -> Optional.ofNullable(a.getRoom()))
                        .map(ASSET, "vendor-phone", (a) -> Optional.ofNullable(a.getVendorPhone()))
                        .map(ASSET, "vendor-fax", (a) -> Optional.ofNullable(a.getVendorFax()))
                        .map(ASSET, "vendor-asset-number", (a) -> Optional.ofNullable(a.getVendorAssetNumber()))
                        .map(ASSET, "username", (a) -> Optional.ofNullable(a.getUsername()))
                        .map(ASSET, "password", (a) -> Optional.ofNullable(a.getPassword()))
                        .map(ASSET, "enable", (a) -> Optional.ofNullable(a.getEnable()))
                        .map(ASSET, "connection", (a) -> Optional.ofNullable(a.getConnection()))
                        .map(ASSET, "autoenable", (a) -> Optional.ofNullable(a.getAutoenable()))
                        .map(ASSET, "last-modified-by", (a) -> Optional.ofNullable(a.getLastModifiedBy()))
                        .map(ASSET, "last-modified-date", (a) -> Optional.ofNullable(a.getLastModifiedDate()).map(Date::toString))
                        .map(ASSET, "date-installed", (a) -> Optional.ofNullable(a.getDateInstalled()))
                        .map(ASSET, "lease", (a) -> Optional.ofNullable(a.getLease()))
                        .map(ASSET, "lease-expires", (a) -> Optional.ofNullable(a.getLeaseExpires()))
                        .map(ASSET, "support-phone", (a) -> Optional.ofNullable(a.getSupportPhone()))
                        .map(ASSET, "maintcontract", (a) -> Optional.ofNullable(a.getMaintcontract()))
                        .map(ASSET, "maint-contract-expiration", (a) -> Optional.ofNullable(a.getMaintContractExpiration()))
                        .map(ASSET, "display-category", (a) -> Optional.ofNullable(a.getDisplayCategory()))
                        .map(ASSET, "notify-category", (a) -> Optional.ofNullable(a.getNotifyCategory()))
                        .map(ASSET, "poller-category", (a) -> Optional.ofNullable(a.getPollerCategory()))
                        .map(ASSET, "threshold-category", (a) -> Optional.ofNullable(a.getThresholdCategory()))
                        .map(ASSET, "comment", (a) -> Optional.ofNullable(a.getComment()))
                        .map(ASSET, "cpu", (a) -> Optional.ofNullable(a.getCpu()))
                        .map(ASSET, "ram", (a) -> Optional.ofNullable(a.getRam()))
                        .map(ASSET, "storagectrl", (a) -> Optional.ofNullable(a.getStoragectrl()))
                        .map(ASSET, "hdd1", (a) -> Optional.ofNullable(a.getHdd1()))
                        .map(ASSET, "hdd2", (a) -> Optional.ofNullable(a.getHdd2()))
                        .map(ASSET, "hdd3", (a) -> Optional.ofNullable(a.getHdd3()))
                        .map(ASSET, "hdd4", (a) -> Optional.ofNullable(a.getHdd4()))
                        .map(ASSET, "hdd5", (a) -> Optional.ofNullable(a.getHdd5()))
                        .map(ASSET, "hdd6", (a) -> Optional.ofNullable(a.getHdd6()))
                        .map(ASSET, "numpowersupplies", (a) -> Optional.ofNullable(a.getNumpowersupplies()))
                        .map(ASSET, "inputpower", (a) -> Optional.ofNullable(a.getInputpower()))
                        .map(ASSET, "additionalhardware", (a) -> Optional.ofNullable(a.getAdditionalhardware()))
                        .map(ASSET, "admin", (a) -> Optional.ofNullable(a.getAdmin()))
                        .map(ASSET, "snmpcommunity", (a) -> Optional.ofNullable(a.getSnmpcommunity()))
                        .map(ASSET, "rackunitheight", (a) -> Optional.ofNullable(a.getRackunitheight()))
                        .map(ASSET, "managed-object-type", (a) -> Optional.ofNullable(a.getManagedObjectType()))
                        .map(ASSET, "managed-object-instance", (a) -> Optional.ofNullable(a.getManagedObjectInstance()))
                        .map(ASSET, "geolocation", (a) -> Optional.ofNullable(a.getGeolocation()).map(Object::toString));
                scopes.add(assetScope);
            }

            return new FallbackScope(scopes);
        });

    }


    @Override
    public Scope getScopeForInterface(Integer nodeId, String ipAddress) {
        if (nodeId == null || Strings.isNullOrEmpty(ipAddress)) {
            return EmptyScope.EMPTY;
        }

        return this.sessionUtils.withReadOnlyTransaction(() -> {
            final OnmsIpInterface ipInterface = this.ipInterfaceDao.findByNodeIdAndIpAddress(nodeId, ipAddress);
            if (ipInterface == null) {
                return EmptyScope.EMPTY;
            }

            return new FallbackScope(transform(Scope.ScopeName.INTERFACE, ipInterface.getMetaData()),
                    mapIpInterfaceKeys(ipInterface)
                            .map(INTERFACE, "if-alias", (i) -> Optional.ofNullable(i.getSnmpInterface()).map(OnmsSnmpInterface::getIfAlias))
                            .map(INTERFACE, "if-description", (i) -> Optional.ofNullable(i.getSnmpInterface()).map(OnmsSnmpInterface::getIfDescr))
                            .map(INTERFACE, "phy-addr", (i) -> Optional.ofNullable(i.getSnmpInterface()).map(OnmsSnmpInterface::getPhysAddr))
            );
        });
    }

    @Override
    public Scope getScopeForInterfaceByIfIndex(Integer nodeId, int ifIndex) {
        if (nodeId == null) {
            return EmptyScope.EMPTY;
        }

        return this.sessionUtils.withReadOnlyTransaction(() -> {
            OnmsSnmpInterface snmpInterface = this.snmpInterfaceDao.findByNodeIdAndIfIndex(nodeId, ifIndex);
            if (snmpInterface == null) {
                return EmptyScope.EMPTY;
            }

            ArrayList<Scope> scopes = new ArrayList<>();

            // SNMP interface facts
            scopes.add(new ObjectScope<>(Scope.ScopeName.INTERFACE, snmpInterface)
                    .map(INTERFACE, "if-alias", (i) -> Optional.ofNullable(i.getIfAlias()))
                    .map(INTERFACE, "if-description", (i) -> Optional.ofNullable(i.getIfDescr()))
                    .map(INTERFACE, "phy-addr", (i) -> Optional.ofNullable(i.getPhysAddr())));

            // IP interface facts w/ meta-data extracted from IP interface
            Optional.ofNullable(snmpInterface.getPrimaryIpInterface())
                    .ifPresent(ipInterface -> {
                        scopes.add(transform(Scope.ScopeName.INTERFACE, ipInterface.getMetaData()));
                        scopes.add(mapIpInterfaceKeys(ipInterface));
                    });

            return new FallbackScope(scopes);
        });
    }

    @Override
    public Scope getScopeForService(Integer nodeId, InetAddress ipAddress, String serviceName) {
        if (nodeId == null || ipAddress == null || Strings.isNullOrEmpty(serviceName)) {
            return EmptyScope.EMPTY;
        }

        return this.sessionUtils.withReadOnlyTransaction(() -> {
            OnmsMonitoredService monitoredService = this.monitoredServiceDao.get(nodeId, ipAddress, serviceName);
            if (monitoredService == null) {
                return EmptyScope.EMPTY;
            }

            return new FallbackScope(transform(Scope.ScopeName.SERVICE, monitoredService.getMetaData()),
                    new ObjectScope<>(Scope.ScopeName.SERVICE, monitoredService)
                            .map(SERVICE, "name", (s) -> Optional.of(s.getServiceName()))
            );
        });
    }

//========================================
// Internals
//----------------------------------------

    private Optional<String> getNodeCriteria(OnmsNode node) {
        Objects.requireNonNull(node, "Node can not be null");
        if (node.getForeignSource() != null) {
            return Optional.of(node.getForeignSource() + ":" + node.getForeignId());
        } else {
            return Optional.of(Integer.toString(node.getId()));
        }
    }

    /**
     * Computes a geohash from the lat/lon associated with the node.
     *
     * This function is expected to be called in the context of a transaction.
     *
     * @param node node from which to derive the geohash
     * @return geohash
     */
    private Optional<String> getNodeGeoHash(OnmsNode node) {
        double latitude = Double.NaN;
        double longitude = Double.NaN;

        // Safely retrieve the geo-location from the node's asset record
        final OnmsAssetRecord assetRecord = node.getAssetRecord();
        if (assetRecord == null) {
            return Optional.empty();
        }
        final OnmsGeolocation geolocation = assetRecord.getGeolocation();
        if (geolocation == null) {
            return Optional.empty();
        }

        // Safely retrieve the lat/lon value from the geo-location
        if (geolocation.getLatitude() != null) {
            latitude = geolocation.getLatitude();
        }
        if (geolocation.getLongitude() != null) {
            longitude = geolocation.getLongitude();
        }
        if (!Double.isFinite(latitude) || !Double.isFinite(longitude)) {
            return Optional.empty();
        }

        // We have a finite lat/lon, compute the geohash using maximum precision
        return Optional.of(GeoHash.withCharacterPrecision(latitude, longitude, 12).toBase32());
    }

    private ObjectScope<OnmsIpInterface> mapIpInterfaceKeys(OnmsIpInterface ipInterface) {
        return new ObjectScope<>(Scope.ScopeName.INTERFACE, ipInterface)
                .map(INTERFACE, "hostname", (i) -> Optional.ofNullable(i.getIpHostName()))
                .map(INTERFACE, "address", (i) -> Optional.ofNullable(i.getIpAddress()).map(InetAddressUtils::toIpAddrString))
                .map(INTERFACE, "netmask", (i) -> Optional.ofNullable(i.getNetMask()).map(InetAddressUtils::toIpAddrString))
                .map(INTERFACE, "if-index", (i) -> Optional.ofNullable(i.getIfIndex()).map(Object::toString));
    }

    private MapScope transform(final Scope.ScopeName scopeName, final Collection<OnmsMetaData> metaData) {
        final Map<ContextKey, String> map = metaData.stream()
                .collect(Collectors.toMap(e -> new ContextKey(e.getContext(), e.getKey()), OnmsMetaData::getValue));
        return new MapScope(scopeName, map);
    }
}
