package org.opennms.horizon.inventory.repository;

import org.opennms.horizon.inventory.model.SnmpInterface;
import org.opennms.horizon.inventory.service.Constants;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

@Component
public class SnmpInterfaceSpecifications {

    public static Specification<SnmpInterface> hasName(String search) {
        return (root, query, criteriaBuilder) -> criteriaBuilder.like(root.get(Constants.SNMP_INTERFACE_COL_NAME), "%" + search + "%");
    }

    public static Specification<SnmpInterface> hasDesc(String search) {
        return (root, query, criteriaBuilder) -> criteriaBuilder.like(root.get(Constants.SNMP_INTERFACE_COL_DESCR), "%" + search + "%");
    }

    public static Specification<SnmpInterface> hasAlias(String search) {
        return (root, query, criteriaBuilder) -> criteriaBuilder.like(root.get(Constants.SNMP_INTERFACE_COL_ALIAS), "%" + search + "%");
    }

    public static Specification<SnmpInterface> hasPhysicalAddress(String search) {
        return (root, query, criteriaBuilder) -> criteriaBuilder.like(root.get(Constants.SNMP_INTERFACE_COL_PHY_ADDR), "%" + search + "%");
    }
}
