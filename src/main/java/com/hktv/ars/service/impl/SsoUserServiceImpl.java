package com.hktv.ars.service.impl;

import com.hktv.ars.data.AuthUserDetails;
import com.hktv.ars.enums.CustomErrorLogMessage;
import com.hktv.ars.exception.CustomRuntimeException;
import com.hktv.ars.model.SsoUser;
import com.hktv.ars.repository.SsoUserDao;
import com.hktv.ars.service.SsoUserService;
import com.hktv.ars.util.UuidUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class SsoUserServiceImpl implements SsoUserService {
    private final SsoUserDao ssoUserDao;

    @Override
    @Transactional
    public String findByUserNameOrCreate(String userName) {

        Optional<SsoUser> ssoUserOptional = ssoUserDao.findByUsername(userName);

        String uuidString;

        if (ssoUserOptional.isEmpty()) {
            //get uuid or create uuid
            uuidString = UuidUtil.checkAndGetUniqueUuid(ssoUserDao);

            SsoUser ssoUser = SsoUser.builder()
                    .userName(userName)
                    .userUuid(uuidString)
                    .build();
            ssoUserDao.save(ssoUser);
        } else {
            uuidString = ssoUserOptional.get().getUserUuid();
        }

        return uuidString;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<String> getCurrentUserName() {
        return Optional.ofNullable(SecurityContextHolder.getContext().getAuthentication())
                .map(Authentication::getDetails)
                .filter(AuthUserDetails.class::isInstance)
                .map(AuthUserDetails.class::cast)
                .map(AuthUserDetails::getUsername);
    }

    @Override
    @Transactional(readOnly = true)
    public Long getCurrentUserId() {
        String userUuid = SecurityContextHolder.getContext().getAuthentication().getName();
        return ssoUserDao.findByUuid(userUuid)
                .orElseThrow(() -> new CustomRuntimeException(CustomErrorLogMessage.SSO_USER_NOT_FOUND, userUuid))
                .getId();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<SsoUser> findOptionalByUserUuid(String userUuid) {
        return ssoUserDao.findByUuid(userUuid);
    }
}
