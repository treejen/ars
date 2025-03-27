package com.hktv.ars.exception;


import com.hktv.ars.enums.CustomErrorLogMessage;

public class CustomRuntimeException extends RuntimeException {

    private final String errorMsg;
    private final int returnCode;

    public CustomRuntimeException(CustomErrorLogMessage customErrorLogMessage, Object... logParameter) {
        super(String.valueOf(customErrorLogMessage.getCode()));
        this.returnCode = customErrorLogMessage.getCode();

        if(logParameter.length > 0) {
            this.errorMsg = customErrorLogMessage.getAsFormattedText(logParameter);
        } else {
            this.errorMsg = customErrorLogMessage.getLogErrorMsg();
        }
    }

    public int getErrorCode() {
        return this.returnCode;
    }

    public String getLogMessage() {
        return this.errorMsg;
    }

    @Override
    public String getMessage() {
        return this.errorMsg;
    }
}
