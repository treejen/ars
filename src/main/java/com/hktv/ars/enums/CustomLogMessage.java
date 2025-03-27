package com.hktv.ars.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum CustomLogMessage {
    SUCCESS("success"),
    ;

    private final String logMessage;

    public String getAsFormattedText(Object... o) {
        return String.format(logMessage, o);
    }
}
