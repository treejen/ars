package com.hktv.ars.service.impl;


import com.hktv.ars.data.sso.AuthResponseData;
import com.hktv.ars.data.sso.AuthoritiesSsoResponseData;
import com.hktv.ars.data.sso.OauthPrincipalSsoResponseData;
import com.hktv.ars.data.sso.OauthTokenSsoResponseData;
import com.hktv.ars.enums.CustomErrorLogMessage;
import com.hktv.ars.enums.JwtUsage;
import com.hktv.ars.exception.CustomRuntimeException;
import com.hktv.ars.exception.HttpClientSideException;
import com.hktv.ars.exception.HttpServerSideException;
import com.hktv.ars.model.AuthSecurityRole;
import com.hktv.ars.repository.AuthSecurityRoleDao;
import com.hktv.ars.service.AuthService;
import com.hktv.ars.service.SsoUserService;
import com.hktv.ars.util.JwtUtil;
import io.jsonwebtoken.io.Encoders;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final SsoUserService ssoUserService;

    private final AuthSecurityRoleDao authSecurityRoleDao;

    private static String token;
    private final RestTemplate restTemplate;
    private final JwtUtil jwtUtil;

    @Value("${ars.sso.oauthUrl}")
    String ssoOauthUrl;

    @Value("${ars.sso.ssoSecretKey}")
    private String ssoSecretKey;

    @PostConstruct
    private void initSsoToken() {
        token = Encoders.BASE64.encode((JwtUsage.ARS + ":" + ssoSecretKey).getBytes(StandardCharsets.UTF_8));
    }

    @Override
    @Transactional
    public OauthTokenSsoResponseData authenticateForLogInWithSso(String userName, String password) {

        // Get sso authorities to set roles(Post request to SSO)
        OauthTokenSsoResponseData resData = authenticateBySso(userName, password);
        OauthPrincipalSsoResponseData resPrincipalData = resData.getPrincipal();
        List<String> roleNames = resPrincipalData.getAuthorities().stream()
                .map(AuthoritiesSsoResponseData::getAuthority)
                .toList();

        List<String> authSecurityRoleNames = authSecurityRoleDao.findRoleNameByNames(roleNames);

        if (authSecurityRoleNames.isEmpty()) {
            throw new CustomRuntimeException(CustomErrorLogMessage.LOG_IN_ROLE_NOT_DEFINED);
        }

        // create user if not exist
        String ssoUserName = resPrincipalData.getUsername();
        String uuid = ssoUserService.findByUserNameOrCreate(ssoUserName);

        // generate token
        String userToken = generateTokenByUserUuid(resPrincipalData.getAuthorities(), password, uuid, ssoUserName);

        resData.setToken(userToken);
        resData.setUserUuid(uuid);
        return resData;
    }

    @Override
    @Transactional(readOnly = true)
    public List<SimpleGrantedAuthority> mappingRoles(List<AuthoritiesSsoResponseData> authorities) {
        List<SimpleGrantedAuthority> roles = new ArrayList<>();

        List<String> authSecurityRoles = authSecurityRoleDao.findAll().stream().map(AuthSecurityRole::getName).toList();

        for (AuthoritiesSsoResponseData authoritiesData : authorities) {
            String role = authoritiesData.getAuthority();
            if (authSecurityRoles.contains(role)) {
                roles.add(new SimpleGrantedAuthority(role));
            }
        }
        return roles;
    }

    private OauthTokenSsoResponseData authenticateBySso(String userName, String password) {

        // Get sso authorities to set roles(Post request to SSO)
        OauthTokenSsoResponseData resData = sendLogInAuthToHKTVSso(ssoOauthUrl, generateBodyParams(userName, password));

        verifyUserDisabled(resData);

        log.info("authorities from sso [{}]", resData.getPrincipal());

        return resData;
    }

    private MultiValueMap<String, Object> generateBodyParams(String userName, String password) {
        MultiValueMap<String, Object> bodyParams = new LinkedMultiValueMap<>();
        bodyParams.add("grant_type", "password");
        bodyParams.add("username", userName);
        bodyParams.add("password", password);
        return bodyParams;
    }

    private void verifyUserDisabled(OauthTokenSsoResponseData authData) {
        if (authData == null || authData.getPrincipal() == null || Boolean.FALSE.equals(authData.getPrincipal().getEnabled())) {
            throw new CustomRuntimeException(CustomErrorLogMessage.USER_IS_DISABLE, authData == null ? null : authData.getUserUuid());
        }
    }

    private OauthTokenSsoResponseData sendLogInAuthToHKTVSso(String url, MultiValueMap<String, Object> bodyParams) {

        HttpEntity<MultiValueMap<String, Object>> httpEntity = new HttpEntity<>(bodyParams, generateHeaders());
        try {

            ResponseEntity<OauthTokenSsoResponseData> response = restTemplate.exchange(url, HttpMethod.POST, httpEntity, OauthTokenSsoResponseData.class);

            statusCheck(response);

            return Optional.ofNullable(response.getBody())
                    .orElseThrow(() -> new CustomRuntimeException(CustomErrorLogMessage.SSO_LOGIN_FAIL_ERROR));

        } catch (HttpServerSideException | HttpClientSideException ex) {
            throw new CustomRuntimeException(CustomErrorLogMessage.SSO_LOGIN_FAIL_ERROR);

        } catch (Exception e) {
            log.error("Send to SSO fail, exception :", e);
            throw new CustomRuntimeException(CustomErrorLogMessage.FAIL_TO_ACCESS_SSO_ERROR);
        }
    }

    private HttpHeaders generateHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.set("Authorization", "Basic " + token);
        return headers;
    }

    private <T> void statusCheck(ResponseEntity<T> result) {
        if (!result.getStatusCode().is2xxSuccessful()) {
            throw new CustomRuntimeException(CustomErrorLogMessage.SSO_LOGIN_FAIL_ERROR);
        }
    }

    private String generateTokenByUserUuid(List<AuthoritiesSsoResponseData> authorities, String password, String userUuid, String username) {

        List<SimpleGrantedAuthority> roles = mappingRoles(authorities);

        UserDetails userDetails = new User(userUuid, password, roles);

        AuthResponseData authResponse = jwtUtil.generateJwt(true, JwtUsage.ARS_SSO.toString(), userDetails, username);

        return authResponse.getToken();
    }
}
