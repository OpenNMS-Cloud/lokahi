package org.opennms.horizon.inventory.service.taskset.response;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.opennms.taskset.contract.DetectorResponse;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DetectorResponseService {

    public void accept(DetectorResponse response) {
        log.info("Received Detector Response = {}", response);

    }
}
