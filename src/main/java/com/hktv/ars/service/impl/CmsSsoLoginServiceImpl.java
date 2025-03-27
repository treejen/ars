package com.hktv.ars.service.impl;

import com.hktv.ars.data.sso.AuthCmsRequestData;
import com.hktv.ars.data.sso.AuthResponseData;
import com.hktv.ars.data.sso.AuthoritiesSsoResponseData;
import com.hktv.ars.data.sso.CmsLoginMainRequestData;
import com.hktv.ars.data.sso.CmsLoginMainResponseData;
import com.hktv.ars.data.sso.OauthTokenSsoResponseData;
import com.hktv.ars.enums.CustomErrorLogMessage;
import com.hktv.ars.enums.JwtUsage;
import com.hktv.ars.exception.CustomRuntimeException;
import com.hktv.ars.service.AuthService;
import com.hktv.ars.service.CmsSsoLoginService;
import com.hktv.ars.util.JwtUtil;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class CmsSsoLoginServiceImpl implements CmsSsoLoginService {

    private final AuthService authService;
    private final JwtUtil jwtUtil;

    @Override
    public CmsLoginMainResponseData login(CmsLoginMainRequestData requestData) {
        String username = requestData.getUsername();
        String password = requestData.getPassword();

        // Get sso authorities to set roles(Post request to SSO)
        OauthTokenSsoResponseData oauthTokenResponseData = authService.authenticateForLogInWithSso(username, password);

        String ssoReturnUsername = oauthTokenResponseData.getPrincipal().getUsername();

        List<String> authoritiesFromSso = oauthTokenResponseData.getPrincipal().getAuthorities()
                .stream()
                .map(AuthoritiesSsoResponseData::getAuthority)
                .toList();

        return CmsLoginMainResponseData.builder()
                .token(oauthTokenResponseData.getToken())
                .username(ssoReturnUsername)
                .roles(authoritiesFromSso)
                .build();
    }

    @Override
    public AuthResponseData renewCmsToken(AuthCmsRequestData authenticationRequest) {
        Claims claims = jwtUtil.decodeTokenClaims(authenticationRequest.getOriginToken());
        String userUuid = (String) claims.get("userUuid");
        String username = (String) claims.get("username");

        if (StringUtils.isEmpty(userUuid)) {
            throw new CustomRuntimeException(CustomErrorLogMessage.FAIL_TO_ACCESS_SSO_ERROR);
        }

        List<SimpleGrantedAuthority> roles = claims.entrySet().stream()
                .filter(entry -> entry.getKey().startsWith("roles"))
                .map(entry -> (List<?>) entry.getValue()).findFirst().orElse(List.of())
                .stream().map(String.class::cast)
                .map(SimpleGrantedAuthority::new).toList();

        UserDetails userDetails = new User(userUuid, "", roles);

        return jwtUtil.generateJwt(true, JwtUsage.ARS_SSO.toString(), userDetails, username);
    }
}
