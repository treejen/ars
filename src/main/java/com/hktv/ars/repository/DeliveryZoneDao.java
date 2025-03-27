package com.hktv.ars.repository;

import com.hktv.ars.model.DeliveryZone;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DeliveryZoneDao extends JpaRepository<DeliveryZone, Long> {
}
