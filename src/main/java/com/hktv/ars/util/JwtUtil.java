package com.hktv.ars.util;

import com.hktv.ars.data.sso.AuthResponseData;
import com.hktv.ars.enums.CustomErrorLogMessage;
import com.hktv.ars.enums.JwtUsage;
import com.hktv.ars.exception.CustomRuntimeException;
import com.hktv.ars.repository.AuthSecurityRoleDao;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.UnsupportedJwtException;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class JwtUtil {

    @Value("${ars.jwt.expirationInMs}")
    private int jwtExpirationInMs;

    private final KeyUtil keyUtil;
    private final AuthSecurityRoleDao authSecurityRoleDao;

    public AuthResponseData generateJwt(boolean needExpiration, String subject, UserDetails userDetails, String username) {

        Map<String, Object> claims = new HashMap<>();

        claims.put("roles", userDetails.getAuthorities().stream().map(GrantedAuthority::getAuthority).toList());

        PrivateKey privateKey = null;
        if (JwtUsage.ARS_SSO.toString().equals(subject)) {
            claims.put("userUuid", userDetails.getUsername());
            privateKey = keyUtil.getCmsAuthPrivateKey();
            if (StringUtils.isNotBlank(username)) {
                claims.put("username", username);
            }
        }
        return new AuthResponseData(doGenerateToken(claims, subject, needExpiration, privateKey));
    }

    private String doGenerateToken(Map<String, Object> claims, String subject, boolean needExpiration, PrivateKey privateKey) {

        String token = "";

        if (privateKey != null) {
            if (needExpiration) {
                token = Jwts.builder()
                        .setClaims(claims)
                        .setSubject(subject)
                        .setIssuedAt(new Date(System.currentTimeMillis()))
                        .setExpiration(new Date(System.currentTimeMillis() + jwtExpirationInMs))
                        .signWith(privateKey, SignatureAlgorithm.RS256)
                        .compact();
            } else {
                token = Jwts.builder()
                        .setClaims(claims)
                        .setSubject(subject)
                        .setIssuedAt(new Date(System.currentTimeMillis()))
                        .signWith(privateKey, SignatureAlgorithm.RS256)
                        .compact();
            }
        }

        return token;
    }

    private PublicKey getPublicKey(JwtUsage jwtUsage) {
        PublicKey publicKey;
        switch (jwtUsage) {
            case ARS_SSO:
                publicKey = keyUtil.getCmsAuthPublicKey();
                break;
            default:
                publicKey = null;
                break;
        }
        return publicKey;
    }

    public boolean validateToken(String token, JwtUsage jwtUsage) {

        PublicKey publicKey = getPublicKey(jwtUsage);

        try {
            Jwts.parserBuilder().setSigningKey(publicKey).build().parseClaimsJws(token);
        } catch (MalformedJwtException | UnsupportedJwtException | IllegalArgumentException ex) {
            throw new BadCredentialsException("INVALID_CREDENTIALS", ex);
        } catch (ExpiredJwtException ex) {
            throw ex;
        }

        return true;
    }

    public List<SimpleGrantedAuthority> getRolesFromToken(String token, JwtUsage jwtUsage) {

        List<SimpleGrantedAuthority> roles = new ArrayList<>();

        Claims claims = getAllClaims(token, jwtUsage);
        List<String> roleNames = (List<String>) claims.get("roles");

        List<String> authSecurityRoleNames = authSecurityRoleDao.findRoleNameByNames(roleNames);

        if (authSecurityRoleNames.isEmpty()) {
            throw new CustomRuntimeException(CustomErrorLogMessage.LOG_IN_ROLE_NOT_DEFINED);
        }

        authSecurityRoleNames.forEach(role -> roles.add(new SimpleGrantedAuthority(role)));

        return roles;
    }

    public Claims getAllClaims(String token, JwtUsage jwtUsage) {

        PublicKey publicKey = getPublicKey(jwtUsage);

        return Jwts.parserBuilder().setSigningKey(publicKey).build().parseClaimsJws(token).getBody();
    }

    public Claims decodeTokenClaims(String token) {
        String[] splitToken = token.split("\\.");

        String unsignedToken = "";
        if (splitToken.length >= 2) {
            unsignedToken = splitToken[0] + "." + splitToken[1] + ".";
        }

        Claims resultClaims;
        try {
            resultClaims = Jwts.parserBuilder().build().parseClaimsJwt(unsignedToken).getBody();
        } catch (ExpiredJwtException e) {
            resultClaims = e.getClaims();
        }

        return resultClaims;
    }

}
