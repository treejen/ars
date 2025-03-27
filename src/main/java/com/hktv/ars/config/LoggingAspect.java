package com.hktv.ars.config;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.hktv.ars.util.JsonUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.util.List;

@Aspect
@Component
@Slf4j
public class LoggingAspect {

    private static final List<String> SENSITIVE_ENDPOINTS = List.of(
            "/cms/sso/login",
            "/cms/sso/renew_token",
            "/swagger-ui.html",
            "/v3/api-docs/**",
            "/swagger-ui/**",
            "/user/login",
            "/error");

    private static final List<String> SKIP_ENDPOINTS = List.of("/healthcheck");

    @Autowired
    private HttpServletRequest request;

    @Autowired
    private HttpServletResponse response;

    @Pointcut("execution(* com.hktv.ars.controller..*(..))")
    public void controllerMethods() {
    }

    @AfterReturning(pointcut = "controllerMethods()", returning = "result")
    public void logAfterReturning(JoinPoint joinPoint, Object result) {
        String uri = request.getRequestURI();

        try {
            if (isSensitiveEndpoint(uri) || isFileResponse(result)) {
                log.info("API: {}, called, response logging is skipped.", uri);
            } else if (!isSkipEndpoint(uri)) {
                String json = JsonUtil.getInstance().writeValueAsString(result);
                log.info("API: {}, response: {}", uri, json);
            }
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
    }

    private boolean isSensitiveEndpoint(String url) {
        return url != null && SENSITIVE_ENDPOINTS.stream().anyMatch(url::contains);
    }

    private boolean isSkipEndpoint(String url) {
        return url != null && SKIP_ENDPOINTS.stream().anyMatch(url::contains);
    }

    private boolean isFileResponse(Object result) {
        if (result instanceof ResponseEntity) {
            ResponseEntity<?> responseEntity = (ResponseEntity<?>) result;
            if (responseEntity.getBody() instanceof StreamingResponseBody) {
                // Skip logging for StreamingResponseBody
                return true;
            }
        }
        return false;
    }
}
