package com.hktv.ars.enums;

import lombok.Getter;

@Getter
public enum AddressType {
    DISTRICT("DISTRICT", true, false),
    ESTATE("ESTATE", true, true),
    STREET("STREET", true, true),
    STREET_NUMBER("STREET NUMBER", false, true);

    private final String value;
    private final boolean isKeyWord;
    private final boolean isHasPoint;

    AddressType(String value, boolean isKeyWord, boolean isHasPoint) {
        this.value = value;
        this.isKeyWord = isKeyWord;
        this.isHasPoint = isHasPoint;
    }
}
