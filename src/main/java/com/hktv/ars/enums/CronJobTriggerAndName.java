package com.hktv.ars.enums;

import lombok.Getter;

@Getter
public enum CronJobTriggerAndName {
    SEND_TO_MMS("sendToMmsJob", "sendToMmsJobTrigger");

    private final String detailIdentity;
    private final String triggerIdentity;

    CronJobTriggerAndName(String detailIdentity, String triggerIdentity) {
        this.detailIdentity = detailIdentity;
        this.triggerIdentity = triggerIdentity;
    }
}
