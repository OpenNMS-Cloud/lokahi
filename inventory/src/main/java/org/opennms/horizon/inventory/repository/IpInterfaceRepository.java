package org.opennms.horizon.inventory.repository;

import java.util.List;
import java.util.Optional;

import com.vladmihalcea.hibernate.type.basic.Inet;
import org.opennms.horizon.inventory.model.IpInterface;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface IpInterfaceRepository extends JpaRepository<IpInterface, Long> {
    List<IpInterface> findByTenantId(String tenantId);

    @Query("select ip from IpInterface ip" +
        " inner join Node n on ip.nodeId = n.id" +
        " inner join MonitoringLocation ml on n.monitoringLocationId = ml.id" +
        " where ip.ipAddress = ?1 and ml.location = ?2 and ip.tenantId = ?3")
    List<IpInterface> findByIpAddressAndLocationAndTenantId(Inet ipAddress, String location, String tenantId);

    @Query("SELECT ip " +
        "from IpInterface ip " +
        "WHERE ip.ipAddress = :ipAddress " +
        "AND ip.node.monitoringLocation.location = :location ")
    Optional<IpInterface> findByIpAddressAndLocation(@Param("ipAddress") Inet ipAddress,
                                                     @Param("location") String location);

    List<IpInterface> findByNodeIdAndTenantId(long nodeId, String tenantId);
}
