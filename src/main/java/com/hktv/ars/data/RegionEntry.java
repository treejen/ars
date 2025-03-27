package com.hktv.ars.data;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RegionEntry {
    String name;
    String code;
    int frequency;

    public RegionEntry(String name, String code, int frequency) {
        this.name = name;
        this.code = code;
        this.frequency = frequency;
    }
}
