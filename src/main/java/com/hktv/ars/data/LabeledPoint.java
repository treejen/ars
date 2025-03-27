package com.hktv.ars.data;

import lombok.Getter;

import java.io.Serial;

/**
 * 帶標籤的數據點，繼承自基礎數據點
 */
@Getter
public class LabeledPoint extends Point {
    @Serial
    private static final long serialVersionUID = 1L;
    private final String label;

    public LabeledPoint(double[] features, String label) {
        super(features);
        this.label = label;
    }

    @Override
    public String toString() {
        return "LabeledPoint{" + super.toString() + ", label=" + label + '}';
    }
}
