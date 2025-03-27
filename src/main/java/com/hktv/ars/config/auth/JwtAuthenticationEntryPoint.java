package com.hktv.ars.config.auth;


import com.hktv.ars.constant.ExceptionConst;
import com.hktv.ars.data.base.ResultData;
import com.hktv.ars.enums.CustomErrorLogMessage;
import com.hktv.ars.util.JsonUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.hc.core5.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException) throws IOException {
        CustomErrorLogMessage customErrorLogMessage = getCustomLogMessage(request);

        if (customErrorLogMessage == CustomErrorLogMessage.TOKEN_AUTHENTICATION_FORBIDDEN_API_ERROR) {
            response.setStatus(HttpStatus.SC_FORBIDDEN);
        } else {
            response.setStatus(HttpStatus.SC_UNAUTHORIZED);
        }

        response.setContentType("application/json;charset=utf-8");
        response.getWriter().print(JsonUtil.getInstance().writeValueAsString(new ResultData<String>(
                customErrorLogMessage.getCode(),
                customErrorLogMessage.getLogErrorMsg(),
                null)));
    }

    private CustomErrorLogMessage getCustomLogMessage(HttpServletRequest request) {
        CustomErrorLogMessage customErrorLogMessage = (CustomErrorLogMessage) request.getAttribute(ExceptionConst.FILTER_EXCEPTION);

        if (ObjectUtils.isEmpty(customErrorLogMessage)) {
            customErrorLogMessage = CustomErrorLogMessage.AUTHENTICATION_ERROR;
        }

        return customErrorLogMessage;
    }
}