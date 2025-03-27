package com.hktv.ars.repository;



import com.hktv.ars.model.SystemVariable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SystemVariableDao extends JpaRepository<SystemVariable, Long> {

    @Query(value = "select * from system_variable as sv where sv.name = ?1", nativeQuery = true)
    Optional<SystemVariable> findByName(String name);

    @Query(value = "SELECT * FROM system_variable WHERE visible = 1 ",
            countQuery = "SELECT count(id) FROM system_variable WHERE visible = 1 ", nativeQuery = true)
    Page<SystemVariable> findSystemVariables(Pageable pageable);

    @Query(value = "select s.* from system_variable s where s.name in (:nameList) ", nativeQuery = true)
    List<SystemVariable> findAllInNameList(List<String> nameList);
}
