package org.opennms.horizon.inventory.service;

import com.vladmihalcea.hibernate.type.basic.Inet;
import lombok.RequiredArgsConstructor;
import org.opennms.horizon.inventory.dto.DeviceCreateDTO;
import org.opennms.horizon.inventory.dto.NodeDTO;
import org.opennms.horizon.inventory.mapper.NodeMapper;
import org.opennms.horizon.inventory.model.IpInterface;
import org.opennms.horizon.inventory.model.MonitoringLocation;
import org.opennms.horizon.inventory.model.Node;
import org.opennms.horizon.inventory.repository.IpInterfaceRepository;
import org.opennms.horizon.inventory.repository.MonitoringLocationRepository;
import org.opennms.horizon.inventory.repository.NodeRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class NodeService {
    private final NodeRepository nodeRepository;
    private final MonitoringLocationRepository monitoringLocationRepository;
    private final IpInterfaceRepository ipInterfaceRepository;

    private final NodeMapper mapper;

    public List<NodeDTO> findByTenantId(String tenantId) {
        List<Node> all = nodeRepository.findByTenantId(tenantId);
        return all
            .stream()
            .map(mapper::modelToDTO)
            .collect(Collectors.toList());
    }

    public Optional<NodeDTO> getByIdAndTenantId(long id, String tenantId){
        return nodeRepository.findByIdAndTenantId(id, tenantId).map(mapper::modelToDTO);
    }

    private void saveIpInterfaces(DeviceCreateDTO request, Node node, String tenantId) {
        if (request.hasManagementIp()) {
            IpInterface ipInterface = new IpInterface();

            ipInterface.setNode(node);
            ipInterface.setTenantId(tenantId);
            ipInterface.setIpAddress(new Inet(request.getManagementIp()));

            ipInterfaceRepository.save(ipInterface);
        }
    }

    private MonitoringLocation saveMonitoringLocation(DeviceCreateDTO request, String tenantId) {
        Optional<MonitoringLocation> found =
            monitoringLocationRepository.findByLocationAndTenantId(request.getLocation(), tenantId);

        if (found.isPresent()) {
            return found.get();
        } else {
            MonitoringLocation location = new MonitoringLocation();

            location.setTenantId(tenantId);
            location.setLocation(request.getLocation());

            return monitoringLocationRepository.save(location);
        }
    }

    private Node saveNode(DeviceCreateDTO request, MonitoringLocation monitoringLocation, String tenantId) {
        Node node = new Node();

        node.setTenantId(tenantId);
        node.setNodeLabel(request.getLabel());
        node.setCreateTime(LocalDateTime.now());
        node.setMonitoringLocation(monitoringLocation);
        node.setMonitoringLocationId(monitoringLocation.getId());

        return nodeRepository.save(node);
    }

    public Node createDevice(DeviceCreateDTO request, String tenantId) {
        MonitoringLocation monitoringLocation = saveMonitoringLocation(request, tenantId);
        Node node = saveNode(request, monitoringLocation, tenantId);
        saveIpInterfaces(request, node, tenantId);

        return node;
    }
}
