package com.hktv.ars.data;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class AddressComponent {
    private String longName;
    private String shortName;
    private List<String> types;
}
