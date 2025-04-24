package com.hktv.ars.enums;

import lombok.Getter;

@Getter
public enum AddressAnalysisType {
    ACHOCORASICK("achocorasick"),
    GOOGLE_MAP("googleMap"),
    STANZA("stanza");

    private final String value;

    AddressAnalysisType(String value) {
        this.value = value;
    }
}

