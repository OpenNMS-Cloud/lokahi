package org.opennms.horizon.inventory.service.taskset.response;

import lombok.extern.slf4j.Slf4j;
import org.opennms.taskset.contract.MonitorResponse;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class MonitorResponseService {

    public void accept(String location, MonitorResponse response) {
        log.info("Received Monitor Response = {} for location = {}", response, location);
    }
}
