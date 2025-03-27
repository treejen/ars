package com.hktv.ars.config.auth;


import com.hktv.ars.constant.ExceptionConst;
import com.hktv.ars.constant.HeaderConst;
import com.hktv.ars.data.AuthUserDetails;
import com.hktv.ars.enums.CustomErrorLogMessage;
import com.hktv.ars.enums.JwtUsage;
import com.hktv.ars.exception.CustomRuntimeException;
import com.hktv.ars.util.JwtUtil;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

@Component
@AllArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;

    private static final Predicate<String> AUTH_TOKEN_NOT_VALID = token ->
            StringUtils.isEmpty(token) || !token.startsWith(HeaderConst.HEADER_AUTHORIZATION_PREFIX);

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {
        JwtUsage jwtUsage = null;
        CustomErrorLogMessage customErrorLogMessage = null;
        try {
            final String authHeader = request.getHeader(HeaderConst.HEADER_AUTHORIZATION);
            if (!AUTH_TOKEN_NOT_VALID.test(authHeader)) {
                final String jwtToken = authHeader.substring(7);

                Claims tokenClaims = jwtUtil.decodeTokenClaims(jwtToken);
                String subject = tokenClaims.getSubject();

                jwtUsage = JwtUsage.valueOf(subject.toUpperCase());

                jwtUtil.validateToken(jwtToken, jwtUsage);
                UserDetails userDetails = new User(subject, "", new ArrayList<>());

                AuthUserDetails.AuthUserDetailsBuilder builder = AuthUserDetails.builder();
                if (JwtUsage.ARS_SSO.equals(jwtUsage)) {
                    String userUuid = (String) tokenClaims.get("userUuid");
                    if (userUuid != null) {
                        List<SimpleGrantedAuthority> roles = jwtUtil.getRolesFromToken(jwtToken, jwtUsage);
                        userDetails = new User(userUuid, "", roles);
                    }
                    Optional.ofNullable(tokenClaims.get("username"))
                            .map(String::valueOf)
                            .ifPresent(builder::username);
                }

                UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(
                        userDetails, null, userDetails.getAuthorities());
                authenticationToken.setDetails(builder.build());
                SecurityContextHolder.getContext().setAuthentication(authenticationToken);
            }
        } catch (ExpiredJwtException ex) {
            log.warn("ExpiredJwtException Token expired. jwt usage: [{}]", jwtUsage);
            customErrorLogMessage = CustomErrorLogMessage.TOKEN_EXPIRE_ERROR;
        } catch (CustomRuntimeException ex) {
            log.info("CustomRuntimeException , jwt usage :[{}] error msg: [{}]", jwtUsage, ex.getLogMessage());
            customErrorLogMessage = CustomErrorLogMessage.TOKEN_AUTHENTICATION_FORBIDDEN_API_ERROR;
        } catch (BadCredentialsException ex) {
            log.info("BadCredentialsException token authentication error, jwt usage: [{}]", jwtUsage);
            customErrorLogMessage = CustomErrorLogMessage.AUTHENTICATION_ERROR;
        }  catch (Exception ex) {
            log.info("Exception [{}]", jwtUsage);
            customErrorLogMessage = CustomErrorLogMessage.AUTHENTICATION_ERROR;
        }

        if (ObjectUtils.isNotEmpty(customErrorLogMessage)) {
            request.setAttribute(ExceptionConst.FILTER_EXCEPTION, customErrorLogMessage);
        }

        filterChain.doFilter(request, response);
    }
}
