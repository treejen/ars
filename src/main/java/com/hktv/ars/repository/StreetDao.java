package com.hktv.ars.repository;

import com.hktv.ars.model.Street;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface StreetDao extends JpaRepository<Street, Long> {

    @Query(value = """
            select * from street
            where street_name_en = :streetName or street_name_zh = :streetName 
            order by last_modified_date desc limit 1
            """
            , nativeQuery = true)
    Optional<Street> findByStreetName(String streetName);

}
