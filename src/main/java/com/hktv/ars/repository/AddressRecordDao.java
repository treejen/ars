package com.hktv.ars.repository;

import com.hktv.ars.data.AddressRecordData;
import com.hktv.ars.model.AddressRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;


public interface AddressRecordDao extends JpaRepository<AddressRecord, Long> {
    @Query(value = """
            select
                ar.address as address,
                ar.estate as estate,
                ar.street as street,
                ar.street_number as streetNumber,
                ar.district as district,
                ar.analysis_type as analysisType,
                ar.original_delivery_zone_code as originalDeliveryZoneCode,
                ar.actual_delivery_zone_code as actualDeliveryZoneCode,
                ar.will_delivery as willDeliver,
                ar.receive_time as receiveTime,
                ar.status as status,
                ar.latitude as latitude,
                ar.longitude as longitude,
                ar.creation_date as creationDate,
                ar.last_modified_date as lastModifiedDate,
                su.user_name as updateBy
            from address_record ar
            left join sso_user su on su.id = ar.update_by 
            """,
            countQuery = """
                    select
                        count(ar.id)
                    from address_record ar
                    left join sso_user su on su.id = ar.update_by 
                    """, nativeQuery = true)
    Page<AddressRecordData> findRecords(Pageable pageable);

}
