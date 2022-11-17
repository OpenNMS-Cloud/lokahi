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
import org.opennms.taskset.contract.DetectorResponse;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class DetectorResponseService {
    private final IpInterfaceRepository ipInterfaceRepository;
    private final MonitoredServiceTypeService monitoredServiceTypeService;
    private final MonitoredServiceService monitoredServiceService;

    public void accept(DetectorResponse response) {
        log.info("Received Detector Response = {}", response);

        Inet ipAddress = new Inet(response.getIpAddress());

        //todo: send back location in response ?
        Optional<IpInterface> ipInterfaceOpt = ipInterfaceRepository
            .findByIpAddressAndLocation(ipAddress, "Default");

        if (ipInterfaceOpt.isPresent()) {
            IpInterface ipInterface = ipInterfaceOpt.get();

            if (response.getDetected()) {
                createMonitoredService(response, ipInterface);
                runMonitors(response, ipInterface);
            } else {
                deleteMonitoredService(response, ipInterface);
                stopMonitors(response, ipInterface);
            }
        } else {
            log.warn("Failed to find IP Interface during detection for ip = {}", ipAddress.toInetAddress());
        }
    }

    private void createMonitoredService(DetectorResponse response, IpInterface ipInterface) {
        MonitoredServiceType monitoredServiceType =
            monitoredServiceTypeService.createSingle(MonitoredServiceTypeDTO.newBuilder()
                .setServiceName(response.getMonitorType().name())
                .setTenantId(ipInterface.getTenantId())
                .build());

        MonitoredServiceDTO newMonitoredService = MonitoredServiceDTO.newBuilder()
            .setTenantId(ipInterface.getTenantId())
            .build();

        monitoredServiceService.create(newMonitoredService, monitoredServiceType, ipInterface);
    }

    private void deleteMonitoredService(DetectorResponse response, IpInterface ipInterface) {
        System.out.println("DetectorResponseService.deleteMonitoredService");
        System.out.println("response = " + response + ", ipInterface = " + ipInterface);
        //todo: implement this
    }

    private void runMonitors(DetectorResponse response, IpInterface ipInterface) {
        log.info("Run monitors for ip = {}", ipInterface.getIpAddress().getAddress());
        //todo: implement this
    }

    private void stopMonitors(DetectorResponse response, IpInterface ipInterface) {
        log.info("Stop monitors for ip = {}", ipInterface.getIpAddress().getAddress());
        //todo: implement this
    }
}
