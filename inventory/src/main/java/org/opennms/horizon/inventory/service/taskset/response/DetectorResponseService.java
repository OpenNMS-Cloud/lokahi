package org.opennms.horizon.inventory.service.taskset.response;

import com.vladmihalcea.hibernate.type.basic.Inet;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.opennms.horizon.inventory.dto.MonitoredServiceDTO;
import org.opennms.horizon.inventory.dto.MonitoredServiceTypeDTO;
import org.opennms.horizon.inventory.model.IpInterface;
import org.opennms.horizon.inventory.model.MonitoredServiceType;
import org.opennms.horizon.inventory.repository.IpInterfaceRepository;
import org.opennms.horizon.inventory.service.MonitoredServiceService;
import org.opennms.horizon.inventory.service.MonitoredServiceTypeService;
import org.opennms.horizon.inventory.service.taskset.MonitorTaskSetService;
import org.opennms.taskset.contract.DetectorResponse;
import org.opennms.taskset.contract.MonitorType;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class DetectorResponseService {
    private final IpInterfaceRepository ipInterfaceRepository;
    private final MonitoredServiceTypeService monitoredServiceTypeService;
    private final MonitoredServiceService monitoredServiceService;
    private final MonitorTaskSetService monitorTaskSetService;

    public void accept(String location, DetectorResponse response) {
        log.info("Received Detector Response = {} for location = {}", response, location);

        Inet ipAddress = new Inet(response.getIpAddress());

        //todo: This should have tenantId in it, as it is possible
        // that a different tenant is using the same location and ipAddress
        Optional<IpInterface> ipInterfaceOpt = ipInterfaceRepository
            .findByIpAddressAndLocation(ipAddress, location);

        if (ipInterfaceOpt.isPresent()) {
            IpInterface ipInterface = ipInterfaceOpt.get();

            if (response.getDetected()) {
                createMonitoredService(response, ipInterface);

                MonitorType monitorType = response.getMonitorType();
                monitorTaskSetService.sendMonitorTask(location, monitorType, ipInterface);

            } else {
                log.info("{} not detected on ip address = {}", response.getMonitorType(), ipAddress.getAddress());
            }
        } else {
            log.warn("Failed to find IP Interface during detection for ip = {}", ipAddress.toInetAddress());
        }
    }

    private void createMonitoredService(DetectorResponse response, IpInterface ipInterface) {
        String tenantId = ipInterface.getTenantId();

        MonitoredServiceType monitoredServiceType =
            monitoredServiceTypeService.createSingle(MonitoredServiceTypeDTO.newBuilder()
                .setServiceName(response.getMonitorType().name())
                .setTenantId(tenantId)
                .build());

        MonitoredServiceDTO newMonitoredService = MonitoredServiceDTO.newBuilder()
            .setTenantId(tenantId)
            .build();

        monitoredServiceService.createSingle(newMonitoredService, monitoredServiceType, ipInterface);
    }
}
