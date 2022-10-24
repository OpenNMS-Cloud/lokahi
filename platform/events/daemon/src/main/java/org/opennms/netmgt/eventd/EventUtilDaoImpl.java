package org.opennms.netmgt.eventd;

import java.util.Objects;

import org.opennms.horizon.shared.utils.InetAddressUtils;
import org.opennms.horizon.db.dao.api.IpInterfaceDao;
import org.opennms.horizon.db.dao.api.NodeDao;
import org.opennms.horizon.db.dao.api.SessionUtils;
import org.opennms.horizon.db.model.OnmsIpInterface;
import org.opennms.horizon.db.model.OnmsNode;

import com.codahale.metrics.MetricRegistry;

public class EventUtilDaoImpl extends AbstractEventUtil {

    private final NodeDao nodeDao;
    private final IpInterfaceDao ipInterfaceDao;

    public EventUtilDaoImpl(MetricRegistry registry, SessionUtils sessionUtils, NodeDao nodeDao, IpInterfaceDao ipInterfaceDao) {
        super(registry, sessionUtils);
        this.nodeDao = Objects.requireNonNull(nodeDao);
        this.ipInterfaceDao = Objects.requireNonNull(ipInterfaceDao);
    }

    @Override
    public String getAssetFieldValue(String parm, long nodeId) {
        throw new UnsupportedOperationException("FIXME: OOPS");
    }

    @Override
    public String getHardwareFieldValue(String parm, long nodeId) {
        throw new UnsupportedOperationException("FIXME: OOPS");
    }

    @Override
    public String getHostName(int nodeId, String hostip) {
        OnmsIpInterface ints = ipInterfaceDao.findByNodeIdAndIpAddress(nodeId, hostip);
        if (ints == null) {
            return hostip;
        } else {
            final String hostname = ints.getIpHostName();
            return (hostname == null) ? hostip : hostname;
        }
    }

    @Override
    public String getIfAlias(long nodeId, String ipAddr) {
        OnmsIpInterface iface = ipInterfaceDao.findByNodeIdAndIpAddress((int)nodeId, ipAddr);
        if (iface != null && iface.getSnmpInterface() != null) {
            return iface.getSnmpInterface().getIfAlias();
        } else {
            return null;
        }
    }

    @Override
    public String getForeignId(long nodeId) {
        OnmsNode node = nodeDao.get((int)nodeId);
        return node == null ? null : node.getForeignId();
    }

    @Override
    public String getForeignSource(long nodeId) {
        OnmsNode node = nodeDao.get((int)nodeId);
        if (node != null) {
            return node.getForeignSource();
        }
        return null;
    }

    @Override
    public String getNodeLabel(long nodeId) {
        return nodeDao.getLabelForId((int) nodeId);
    }

    @Override
    public String getNodeLocation(long nodeId) {
        return nodeDao.getLocationForId((int) nodeId);
    }

    @Override
    public String getPrimaryInterface(long nodeId) {
        final OnmsIpInterface onmsIpInterface = ipInterfaceDao.findPrimaryInterfaceByNodeId((int)nodeId);
        if (onmsIpInterface != null) {
            return InetAddressUtils.toIpAddrString(onmsIpInterface.getIpAddress());
        } else {
            return null;
        }
    }

}
