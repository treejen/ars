package com.hktv.ars.enums;

import lombok.Getter;

@Getter
public enum CronJobStatusEnum {
    SUCCESS("SUCCESS"),
    FAILED("FAILED"),
    PENDING("PENDING"),
    ;

    private final String value;

    CronJobStatusEnum(String value) {
        this.value = value;
    }
}
