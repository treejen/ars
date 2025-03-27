package com.hktv.ars.filter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.hktv.ars.data.base.CachedBodyHttpServletRequest;
import com.hktv.ars.model.SsoUser;
import com.hktv.ars.service.SsoUserService;
import com.hktv.ars.util.JsonUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.security.Principal;
import java.util.Collections;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Component
@AllArgsConstructor
public class RequestFilter extends OncePerRequestFilter {

    private final SsoUserService ssoUserService;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {
        HttpServletRequest httpRequest;
        boolean isMultipart = isMultipart(request);
        if (isMultipart) {
            httpRequest = request;
        } else {
            httpRequest = new CachedBodyHttpServletRequest(request);
        }

        String uri = httpRequest.getRequestURI();
        String method = httpRequest.getMethod();
        String params = getParams(httpRequest);
        String body = isMultipart ? "" : getBody(httpRequest);
        String ssoUserName = getSsoUserName(httpRequest);

        log.info("API: {}, method: {}, params: [{}], body: [{}], ssoUserName: [{}]",
                uri, method, params, body, ssoUserName);

        filterChain.doFilter(httpRequest, response);
    }

    private String getParams(HttpServletRequest request) {
        return Collections.list(request.getParameterNames()).stream()
                .map(paramName -> {
                    try {
                        return paramName + ":" + JsonUtil.getInstance().writeValueAsString(request.getParameter(paramName));
                    } catch (JsonProcessingException e) {
                        log.error("JsonProcessingException: ", e);
                    }
                    return null;
                })
                .collect(Collectors.joining(", "));
    }

    private boolean isMultipart(HttpServletRequest request) {
        return StringUtils.startsWithIgnoreCase(request.getContentType(), "multipart/");
    }

    private String getBody(HttpServletRequest request) throws IOException {
        String body = "";
        String requestBody = request.getReader().lines().collect(Collectors.joining(System.lineSeparator()));
        if (StringUtils.isNoneBlank(requestBody)) {
            Object json = JsonUtil.getInstance().readValue(requestBody, Object.class);
            body = JsonUtil.getInstance().writeValueAsString(json);
        }
        return body;
    }

    private String getSsoUserName(HttpServletRequest request) {
        String ssoUserName = "";
        Principal userPrincipal = request.getUserPrincipal();
        if (userPrincipal != null) {
            String ssoUserUuid = userPrincipal.getName();
            Optional<SsoUser> ssoUserOptional = ssoUserService.findOptionalByUserUuid(ssoUserUuid);
            if (ssoUserOptional.isPresent()) {
                ssoUserName = ssoUserOptional.get().getDbUsername();
            }
        }
        return ssoUserName;
    }
}