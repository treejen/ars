package com.hktv.ars.repository;

import com.hktv.ars.model.StreetNumber;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface StreetNumberDao extends JpaRepository<StreetNumber, Long> {

    @Query(value = """
            select sn.* from street_number sn
            join street s on sn.street_code = s.street_code
            where sn.street_number = :streetNumber 
            and (s.street_name_en = :streetName or s.street_name_zh = :streetName) 
            order by sn.last_modified_date desc limit 1
            """
            , nativeQuery = true)
    Optional<StreetNumber> findByStreetNameAndNumber(String streetName, String streetNumber);

}
