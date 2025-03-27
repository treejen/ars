package com.hktv.ars.repository;

import com.hktv.ars.model.AuthSecurityRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AuthSecurityRoleDao extends JpaRepository<AuthSecurityRole, Long> {

    @Query(value = " select name from auth_security_role where name in :names ", nativeQuery = true)
    List<String> findRoleNameByNames(List<String> names);

}
