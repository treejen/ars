package com.hktv.ars.service;


import com.hktv.ars.model.SsoUser;

import java.util.Optional;

public interface SsoUserService {

    String findByUserNameOrCreate(String username);

    Optional<String> getCurrentUserName();

    Long getCurrentUserId();

    Optional<SsoUser> findOptionalByUserUuid(String userUuid);
}
