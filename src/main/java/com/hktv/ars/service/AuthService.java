package com.hktv.ars.service;

import com.hktv.ars.data.sso.AuthoritiesSsoResponseData;
import com.hktv.ars.data.sso.OauthTokenSsoResponseData;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;

public interface AuthService {

    OauthTokenSsoResponseData authenticateForLogInWithSso(String userName, String password);

    List<SimpleGrantedAuthority> mappingRoles(List<AuthoritiesSsoResponseData> authorities);
}
