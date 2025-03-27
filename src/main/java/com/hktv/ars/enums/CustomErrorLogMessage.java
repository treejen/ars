package com.hktv.ars.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum CustomErrorLogMessage {
    HTTP_STATUS_UNAUTHORIZED("AUTHENTICATION_ERROR", 401),
    INTERNAL_ERROR("Internal error", 500),
    USER_NOT_FOUND_EXCEPTION("User not found user uuid: [%s]", 501),
    NO_USER_EXCEPTION("Failed to get user", 502),
    JSON_CONVERSION_EXCEPTION("Fail to conversion: [%s]", 503),
    REQUEST_CONTEXT_NOT_AVAILABLE("Request context not available", 504),

    AUTHENTICATION_ERROR("Authentication error", 2001),
    TOKEN_EXPIRE_ERROR("Token expired", 2002),
    TOKEN_AUTHENTICATION_FORBIDDEN_API_ERROR("Token authentication forbidden api", 2003),
    KEY_LOAD_FAILED("Key load failed", 2004),

    METHOD_ARGUMENT_ERROR("METHOD_ARGUMENT_ERROR",3001),
    USER_IS_DISABLE("User is disable [%s]", 8001),
    SSO_LOGIN_FAIL_ERROR("Failed to log in SSO", 8002),
    FAIL_TO_ACCESS_SSO_ERROR("Failed to access SSO", 8003),
    SSO_USER_NOT_FOUND("Sso user not found. User ID , Name or Uuid : [%s]", 8004),
    LOG_IN_ROLE_NOT_DEFINED("Log in role not defined", 8005),


    FILE_NOT_FOUND("File not found, file path: [%s]", 1001),
    FILE_VALIDATE_FAIL("File validate fail reason: [%s]", 1002),
    FILE_UPLOAD_FAIL("File upload fail.", 1003),
    FILE_DOWNLOAD_FAIL("File download fail. error: [%s]", 1004),
    FILE_SAVE_FAIL("File save fail.", 1005),
    FILE_NAME_DUPLICATE("File name duplicate: [%s]", 1006),

    QUARTZ_JOB_NOT_FOUND("Quartz job not found. Job name: [%s]", 1100),
    QUARTZ_NEXT_FIRE_TIME_INVALID("Quartz next fire time invalid: [%s]", 1101),
    QUARTZ_JOB_UPDATE_FAIL("Quartz job update fail: [%s]", 1102),
    QUARTZ_JOB_TRIGGER_FAIL("Quartz job trigger fail: [%s]", 1103),

    SYSTEM_VARIABLE_NOT_FOUND("System variable not found. Name : [%s]", 1400),

    ;

    private final String logErrorMsg;

    private final int code;

    public String getAsFormattedText(Object... o) {
        return String.format(logErrorMsg, o);
    }

}
