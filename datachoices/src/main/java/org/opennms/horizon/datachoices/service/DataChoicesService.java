package org.opennms.horizon.datachoices.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.opennms.horizon.datachoices.dto.ToggleDataChoicesDTO;
import org.opennms.horizon.datachoices.model.DataChoices;
import org.opennms.horizon.datachoices.repository.DataChoicesRepository;
import org.springframework.stereotype.Component;

import javax.transaction.Transactional;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Component
@Transactional
@RequiredArgsConstructor
public class DataChoicesService {
    private final DataChoicesRepository repository;

    public void toggle(ToggleDataChoicesDTO request, String tenantId) {
        if (request.getToggle()) {
            toggleOn(tenantId);
        } else {
            toggleOff(tenantId);
        }
    }

    private void toggleOn(String tenantId) {
        Optional<DataChoices> dataChoicesOpt = repository.findByTenantId(tenantId);
        if (dataChoicesOpt.isEmpty()) {
            DataChoices dataChoices = new DataChoices();
            dataChoices.setTenantId(tenantId);
            repository.saveAndFlush(dataChoices);
        } else {
            log.warn("DataChoices already toggled on for tenant = {}", tenantId);
        }
    }

    private void toggleOff(String tenantId) {
        Optional<DataChoices> dataChoicesOpt = repository.findByTenantId(tenantId);
        if (dataChoicesOpt.isPresent()) {
            DataChoices dataChoices = dataChoicesOpt.get();
            repository.delete(dataChoices);
        } else {
            log.warn("DataChoices already toggled off for tenant = {}", tenantId);
        }
    }

    public void execute() {
        List<String> tenantIds = repository.findAll().stream()
            .map(DataChoices::getTenantId).distinct().collect(Collectors.toList());

        System.out.println("tenantIds.size() = " + tenantIds.size());

//        todo: perform queries and send HTTP request to usage-stats-handler
    }
}
