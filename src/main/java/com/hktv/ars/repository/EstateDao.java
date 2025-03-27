package com.hktv.ars.repository;

import com.hktv.ars.model.Estate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EstateDao extends JpaRepository<Estate, Long> {

    @Query(value = """
            select * from estate
            where estate_name_en = :estateName or estate_name_zh = :estateName 
            order by last_modified_date desc limit 1
            """
            , nativeQuery = true)
    Optional<Estate> findByEstateName(String estateName);

}
