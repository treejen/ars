package com.hktv.ars.repository;

import com.hktv.ars.model.SsoUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SsoUserDao extends UuidDao<SsoUser>, JpaRepository<SsoUser, Long> {
    @Query(value = " select * from sso_user where user_name = :username ", nativeQuery = true)
    Optional<SsoUser> findByUsername(String username);

    @Query(value = " select * from sso_user where user_uuid = :uuid ", nativeQuery = true)
    Optional<SsoUser> findByUuid(String uuid);
}
